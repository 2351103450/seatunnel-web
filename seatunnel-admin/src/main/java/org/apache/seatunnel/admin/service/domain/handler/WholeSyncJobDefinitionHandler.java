package org.apache.seatunnel.admin.service.domain.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.apache.seatunnel.admin.components.parser.JobDefinitionResolver;
import org.apache.seatunnel.admin.service.SeaTunnelJobInstanceService;
import org.apache.seatunnel.admin.service.domain.model.JobDefinitionAnalysisResult;
import org.apache.seatunnel.communal.bean.dto.SeatunnelBatchJobDefinitionDTO;
import org.apache.seatunnel.communal.bean.entity.NodeTypes;
import org.springframework.stereotype.Component;

@Component
public class WholeSyncJobDefinitionHandler implements JobDefinitionHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private JobDefinitionResolver jobDefinitionResolver;

    @Resource
    private SeaTunnelJobInstanceService seatunnelJobInstanceService;

    @Override
    public boolean supports(SeatunnelBatchJobDefinitionDTO dto) {
        return Boolean.TRUE.equals(dto.getWholeSync());
    }

    @Override
    public JobDefinitionAnalysisResult analyze(SeatunnelBatchJobDefinitionDTO dto) {
        String jobInfo = dto.getJobDefinitionInfo();

        NodeTypes nodeTypes = jobDefinitionResolver.resolveWholeSync(jobInfo);

        return JobDefinitionAnalysisResult.builder()
                .sourceType(nodeTypes.getSourceTypes())
                .sinkType(nodeTypes.getSinkTypes())
                .sourceTableJson(toJson(nodeTypes.getSourceTableMap()))
                .sinkTableJson(toJson(nodeTypes.getSinkTableMap()))
                .normalizedJobDefinitionInfo(jobInfo)
                .build();
    }

    @Override
    public String buildHoconConfig(SeatunnelBatchJobDefinitionDTO dto) {
        return seatunnelJobInstanceService.buildHoconConfigByWholeSync(dto);
    }

    private String toJson(Object obj) {
        try {
            return obj == null ? "{}" : OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }
}