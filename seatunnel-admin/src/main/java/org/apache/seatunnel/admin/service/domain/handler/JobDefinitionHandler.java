package org.apache.seatunnel.admin.service.domain.handler;

import org.apache.seatunnel.admin.service.domain.model.JobDefinitionAnalysisResult;
import org.apache.seatunnel.communal.bean.dto.SeatunnelBatchJobDefinitionDTO;

public interface JobDefinitionHandler {

    /**
     * Whether current handler supports this dto
     */
    boolean supports(SeatunnelBatchJobDefinitionDTO dto);

    /**
     * Analyze definition and extract metadata
     */
    JobDefinitionAnalysisResult analyze(SeatunnelBatchJobDefinitionDTO dto);

    /**
     * Build HOCON config for current mode
     */
    String buildHoconConfig(SeatunnelBatchJobDefinitionDTO dto);
}