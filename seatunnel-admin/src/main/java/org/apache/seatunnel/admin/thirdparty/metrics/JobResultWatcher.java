package org.apache.seatunnel.admin.thirdparty.metrics;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.admin.thirdparty.client.SeatunnelRestClient;
import org.apache.seatunnel.communal.enums.JobResult;
import org.apache.seatunnel.communal.enums.JobStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class JobResultWatcher {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Resource
    private JobMetricsMonitor metricsMonitor;

    @Resource
    private JobResultHandler resultHandler;

    @Resource
    private SeatunnelRestClient seatunnelRestClient;

    @Value("${seatunnel.result.poll-interval-ms:2000}")
    private long pollIntervalMs;

    @Value("${seatunnel.result.poll-timeout-ms:0}") // 0 表示不超时
    private long pollTimeoutMs;

    public void registerByRest(JobRuntimeContext context) {
        executor.submit(() -> {
            long start = System.currentTimeMillis();
            Long instanceId = context.getInstanceId();
            Long engineId = context.getEngineId();

            try {
                while (true) {
                    if (pollTimeoutMs > 0 && (System.currentTimeMillis() - start) > pollTimeoutMs) {
                        throw new IllegalStateException("Polling job-info timeout, engineId=" + engineId);
                    }

                    Map jobInfo = seatunnelRestClient.jobInfo(engineId);
                    String statusStr = (jobInfo == null) ? null : String.valueOf(jobInfo.get("jobStatus"));

                    if (statusStr == null || "null".equals(statusStr)) {
                        // 文档说：查不到 job 可能只返回 {"jobId":""}
                        log.warn("job-info returned no status, engineId={}, resp={}", engineId, jobInfo);
                        Thread.sleep(pollIntervalMs);
                        continue;
                    }

                    JobStatus status = parseJobStatus(statusStr);

                    if (status == JobStatus.RUNNING) {
                        Thread.sleep(pollIntervalMs);
                        continue;
                    }

                    // 结束态
                    if (status == JobStatus.FINISHED) {
                        resultHandler.handleSuccess(instanceId);
                    } else {
                        // 你现有 handleFailure(instanceId, JobResult) 需要 JobResult，这里做个最小封装
                        JobResult jr = new JobResult(JobStatus.FAILED);
                        jr.setStatus(status);
                        jr.setError(String.valueOf(jobInfo.get("errorMsg")));
                        resultHandler.handleFailure(instanceId, jr);
                    }

                    // 最终 metrics 入库 + 清理
                    metricsMonitor.finalizeAndPersist(instanceId);
                    return;
                }
            } catch (Exception e) {
                resultHandler.handleFailure(instanceId, e);
                try {
                    metricsMonitor.finalizeAndPersist(instanceId);
                } catch (Exception ignored) {}
            } finally {
                log.info("REST job result watcher finished: {}", instanceId);
            }
        });
    }

    private JobStatus parseJobStatus(String s) {
        try {
            return JobStatus.valueOf(s);
        } catch (Exception e) {
            // 兼容可能出现的不同拼写（比如 CANCELED vs CANCELLED）
            if ("CANCELLED".equalsIgnoreCase(s)) return JobStatus.CANCELED;
            return JobStatus.UNKNOWABLE;
        }
    }
}