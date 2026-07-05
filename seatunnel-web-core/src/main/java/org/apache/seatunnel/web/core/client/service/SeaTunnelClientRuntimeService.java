package org.apache.seatunnel.web.core.client.service;

import org.apache.seatunnel.web.core.client.port.SeaTunnelClientRuntimeGateway;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


public class SeaTunnelClientRuntimeService {

    private final SeaTunnelClientRuntimeGateway runtimeGateway;

    public SeaTunnelClientRuntimeService(SeaTunnelClientRuntimeGateway runtimeGateway) {
        this.runtimeGateway = runtimeGateway;
    }

    public List<Map<String, Object>> metrics(Long clientId) {
        return runtimeGateway.metrics(clientId);
    }

    public String logs(Long clientId, String engineJobId) {
        if (clientId == null) {
            throw new IllegalArgumentException("clientId cannot be empty");
        }

        if (engineJobId == null || engineJobId.trim().isEmpty()) {
            throw new IllegalArgumentException("engineJobId cannot be empty");
        }

        return runtimeGateway.jobLogs(clientId, engineJobId, "json");
    }

    public Map<String, Object> checkpointOverview(Long clientId, Long jobId) {
        return runtimeGateway.checkpointOverview(clientId, jobId);
    }

    public List<Map<String, Object>> checkpointHistory(
            Long clientId,
            Long jobId,
            Long pipelineId,
            Integer limit,
            String status
    ) {
        return runtimeGateway.checkpointHistory(
                clientId,
                jobId,
                pipelineId,
                limit,
                status
        );
    }
}