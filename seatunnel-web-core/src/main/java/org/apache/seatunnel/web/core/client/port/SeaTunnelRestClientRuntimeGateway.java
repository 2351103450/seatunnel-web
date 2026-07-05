package org.apache.seatunnel.web.core.client.port;

import jakarta.annotation.Resource;
import org.apache.seatunnel.web.core.client.port.SeaTunnelClientRuntimeGateway;
import org.apache.seatunnel.web.engine.client.rest.SeaTunnelRestClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SeaTunnelRestClientRuntimeGateway implements SeaTunnelClientRuntimeGateway {

    @Resource
    private SeaTunnelRestClient seaTunnelRestClient;

    @Override
    public List<Map<String, Object>> metrics(Long clientId) {
        return seaTunnelRestClient.systemMonitoringInformation(clientId);
    }

    @Override
    public String jobLogs(
            Long clientId,
            String engineJobId,
            String format
    ) {
        return seaTunnelRestClient.jobLogs(clientId, engineJobId, format);
    }

    @Override
    public Map<String, Object> checkpointOverview(
            Long clientId,
            Long jobId
    ) {
        return seaTunnelRestClient.checkpointOverview(clientId, jobId);
    }

    @Override
    public List<Map<String, Object>> checkpointHistory(
            Long clientId,
            Long jobId,
            Long pipelineId,
            Integer limit,
            String status
    ) {
        return seaTunnelRestClient.checkpointHistory(
                clientId,
                jobId,
                pipelineId,
                limit,
                status
        );
    }
}
