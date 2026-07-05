package org.apache.seatunnel.web.core.client.port;

import java.util.List;
import java.util.Map;

public interface SeaTunnelClientRuntimeGateway {

    List<Map<String, Object>> metrics(Long clientId);

    String jobLogs(Long clientId, String engineJobId, String format);

    Map<String, Object> checkpointOverview(Long clientId, Long jobId);

    List<Map<String, Object>> checkpointHistory(
            Long clientId,
            Long jobId,
            Long pipelineId,
            Integer limit,
            String status
    );
}