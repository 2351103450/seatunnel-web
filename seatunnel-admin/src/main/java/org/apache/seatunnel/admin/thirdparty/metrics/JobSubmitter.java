package org.apache.seatunnel.admin.thirdparty.metrics;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.admin.service.SeatunnelJobInstanceService;
import org.apache.seatunnel.admin.thirdparty.client.SeatunnelRestClient;
import org.apache.seatunnel.communal.bean.po.SeatunnelJobInstancePO;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class JobSubmitter {

    @Resource
    private JobConfigFileService configFileService;

    @Resource
    private SeatunnelRestClient seatunnelRestClient; // ✅ 新增：REST 客户端

    @Resource
    private JobMetricsMonitor metricsMonitor;

    @Resource
    private JobResultWatcher resultWatcher;

    @Resource
    private JobResultHandler resultHandler;

    @Resource
    private SeatunnelJobInstanceService instanceService;

    public void submit(Long instanceId, String hoconConfig) {

        SeatunnelJobInstancePO instancePO = instanceService.getById(instanceId);
        String logPath = instancePO.getLogPath();

        JobFileLogger jobLogger = new JobFileLogger(logPath);
        jobLogger.info("=== Job Submit Start (REST API) ===");
        jobLogger.info("Job instanceId: " + instanceId);

        boolean success = false;

        try {
            jobLogger.info("Writing config file...");
            String configFile = configFileService.writeConfig(instanceId, hoconConfig);
            jobLogger.info("Config file written to: " + configFile);

            jobLogger.info("Submitting job via REST API...");
            String filename = "job-" + instanceId + ".conf";
            Map resp = seatunnelRestClient.submitJobUpload(
                    hoconConfig.getBytes(StandardCharsets.UTF_8),
                    filename
            );

            Object jobIdObj = resp.get("jobId");
            if (jobIdObj == null) {
                throw new IllegalStateException("REST submit response missing jobId, resp=" + resp);
            }

            String engineId = String.valueOf(jobIdObj);
            jobLogger.info("Job submitted, engineId(jobId): " + engineId);

            resultHandler.updateEngineId(instanceId, engineId);

            JobRuntimeContext context = new JobRuntimeContext(instanceId, engineId, configFile);

            metricsMonitor.register(context);
            jobLogger.info("Metrics monitor registered");

            resultWatcher.registerByRest(context);
            jobLogger.info("REST result watcher registered");

            jobLogger.info("=== Job Submit Complete ===");
            success = true;
        } catch (Exception e) {
            jobLogger.error("Job submit failed", e);
            resultHandler.handleFailure(instanceId, e);
            throw new RuntimeException("Submit job failed", e);
        } finally {
            if (!success) {
                log.error("Job submission ended unsuccessfully: {}", instanceId);
            }
            jobLogger.close();
        }
    }
}