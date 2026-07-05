package org.apache.seatunnel.web.api.service.impl.client;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.web.common.enums.JobMode;
import org.apache.seatunnel.web.core.exceptions.ServiceException;
import org.apache.seatunnel.web.core.utils.MetricValueParser;
import org.apache.seatunnel.web.dao.entity.JobInstance;
import org.apache.seatunnel.web.dao.entity.SeaTunnelClient;
import org.apache.seatunnel.web.dao.entity.StreamingJobInstance;
import org.apache.seatunnel.web.dao.repository.JobInstanceDao;
import org.apache.seatunnel.web.dao.repository.SeaTunnelClientDao;
import org.apache.seatunnel.web.dao.repository.StreamingJobInstanceDao;
import org.apache.seatunnel.web.engine.client.rest.SeaTunnelRestClient;
import org.apache.seatunnel.web.spi.bean.vo.SeaTunnelClientMetricsVO;
import org.apache.seatunnel.web.spi.enums.Status;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SeaTunnelClientRuntimeAppService {

    @Resource
    private SeaTunnelClientDao seaTunnelClientDao;

    @Resource
    private JobInstanceDao jobInstanceDao;

    @Resource
    private StreamingJobInstanceDao streamingJobInstanceDao;

    @Resource
    private SeaTunnelRestClient seaTunnelRestClient;

    public SeaTunnelClientMetricsVO metrics(Long id) {
        getEntity(id);

        List<Map<String, Object>> metricsList =
                seaTunnelRestClient.systemMonitoringInformation(id);

        Map<String, Object> metricMap =
                metricsList == null || metricsList.isEmpty() ? null : metricsList.get(0);

        Double cpuUsage = MetricValueParser.parsePercent(
                metricMap == null ? null : metricMap.get("load.system")
        );
        Double memoryUsage = MetricValueParser.parsePercent(
                metricMap == null ? null : metricMap.get("heap.memory.used/total")
        );
        Integer threadCount = MetricValueParser.parseInteger(
                metricMap == null ? null : metricMap.get("thread.count")
        );
        Integer runningOps = MetricValueParser.parseInteger(
                metricMap == null ? null : metricMap.get("operations.running.count")
        );

        return new SeaTunnelClientMetricsVO(
                cpuUsage,
                memoryUsage,
                threadCount,
                runningOps
        );
    }

    public String logsByInstanceId(Long instanceId, String jobMode) {
        if (instanceId == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "instanceId 不能为空"
            );
        }

        if (StringUtils.isNotBlank(jobMode)) {
            JobMode mode = resolveJobMode(jobMode);

            if (mode == JobMode.BATCH) {
                return getOfflineInstanceLogs(instanceId);
            }

            if (mode == JobMode.STREAMING) {
                return getStreamingInstanceLogs(instanceId);
            }
        }

        JobInstance offlineInstance = jobInstanceDao.queryById(instanceId);
        if (offlineInstance != null) {
            return getEngineLogs(
                    offlineInstance.getClientId(),
                    offlineInstance.getEngineJobId(),
                    "BATCH",
                    instanceId
            );
        }

        StreamingJobInstance streamingInstance =
                streamingJobInstanceDao.queryById(instanceId);

        if (streamingInstance != null) {
            return getEngineLogs(
                    streamingInstance.getClientId(),
                    streamingInstance.getEngineJobId(),
                    "STREAMING",
                    instanceId
            );
        }

        throw new ServiceException(
                Status.INTERNAL_SERVER_ERROR_ARGS,
                "任务实例不存在, instanceId=" + instanceId
        );
    }

    public Map<String, Object> checkpointOverview(
            Long clientId,
            Long jobId
    ) {
        checkClientAndJob(clientId, jobId);
        return seaTunnelRestClient.checkpointOverview(clientId, jobId);
    }

    public List<Map<String, Object>> checkpointHistory(
            Long clientId,
            Long jobId,
            Long pipelineId,
            Integer limit,
            String status
    ) {
        checkClientAndJob(clientId, jobId);

        int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 200);

        return seaTunnelRestClient.checkpointHistory(
                clientId,
                jobId,
                pipelineId,
                safeLimit,
                status
        );
    }

    private String getOfflineInstanceLogs(Long instanceId) {
        JobInstance instance = jobInstanceDao.queryById(instanceId);

        if (instance == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "离线任务实例不存在, instanceId=" + instanceId
            );
        }

        return getEngineLogs(
                instance.getClientId(),
                instance.getEngineJobId(),
                "BATCH",
                instanceId
        );
    }

    private String getStreamingInstanceLogs(Long instanceId) {
        StreamingJobInstance instance =
                streamingJobInstanceDao.queryById(instanceId);

        if (instance == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "实时任务实例不存在, instanceId=" + instanceId
            );
        }

        return getEngineLogs(
                instance.getClientId(),
                instance.getEngineJobId(),
                "STREAMING",
                instanceId
        );
    }

    private String getEngineLogs(
            Long clientId,
            String engineJobId,
            String jobMode,
            Long instanceId
    ) {
        if (clientId == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "clientId 为空, jobMode=" + jobMode + ", instanceId=" + instanceId
            );
        }

        if (StringUtils.isBlank(engineJobId)) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "engineJobId 为空，任务可能尚未成功提交, jobMode="
                            + jobMode
                            + ", instanceId="
                            + instanceId
            );
        }

        return seaTunnelRestClient.jobLogs(clientId, engineJobId, "json");
    }

    private JobMode resolveJobMode(String jobMode) {
        try {
            return JobMode.valueOf(jobMode.trim().toUpperCase());
        } catch (Exception e) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "不支持的任务模式: " + jobMode
            );
        }
    }

    private void checkClientAndJob(
            Long clientId,
            Long jobId
    ) {
        if (clientId == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "clientId 不能为空"
            );
        }

        if (jobId == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "jobId 不能为空"
            );
        }

        getEntity(clientId);
    }

    private SeaTunnelClient getEntity(Long id) {
        if (id == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "客户端 ID 不能为空"
            );
        }

        SeaTunnelClient entity = seaTunnelClientDao.queryById(id);

        if (entity == null) {
            throw new ServiceException(
                    Status.INTERNAL_SERVER_ERROR_ARGS,
                    "客户端不存在, id=" + id
            );
        }

        return entity;
    }
}