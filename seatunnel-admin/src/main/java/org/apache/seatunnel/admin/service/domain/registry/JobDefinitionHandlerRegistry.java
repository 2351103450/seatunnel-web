package org.apache.seatunnel.admin.service.domain.registry;

import jakarta.annotation.Resource;
import org.apache.seatunnel.admin.service.domain.handler.JobDefinitionHandler;
import org.apache.seatunnel.communal.bean.dto.SeatunnelBatchJobDefinitionDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobDefinitionHandlerRegistry {

    @Resource
    private List<JobDefinitionHandler> handlers;

    public JobDefinitionHandler getHandler(SeatunnelBatchJobDefinitionDTO dto) {
        return handlers.stream()
                .filter(handler -> handler.supports(dto))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No suitable JobDefinitionHandler found"));
    }
}