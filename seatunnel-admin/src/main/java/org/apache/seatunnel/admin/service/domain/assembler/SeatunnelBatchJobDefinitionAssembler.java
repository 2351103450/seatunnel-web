package org.apache.seatunnel.admin.service.domain.assembler;

import org.apache.seatunnel.admin.service.domain.model.JobDefinitionAnalysisResult;
import org.apache.seatunnel.communal.bean.dto.SeatunnelBatchJobDefinitionDTO;
import org.apache.seatunnel.communal.bean.po.SeatunnelBatchJobDefinitionPO;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class SeatunnelBatchJobDefinitionAssembler {

    public SeatunnelBatchJobDefinitionPO create(SeatunnelBatchJobDefinitionDTO dto,
                                                JobDefinitionAnalysisResult analysis,
                                                Date now) {
        return SeatunnelBatchJobDefinitionPO.builder()
                .id(dto.getId())
                .jobName(dto.getJobName())
                .jobDesc(dto.getJobDesc())
                .jobDefinitionInfo(analysis.getNormalizedJobDefinitionInfo())
                .jobVersion(1)
                .clientId(dto.getClientId())
                .wholeSync(Boolean.TRUE.equals(dto.getWholeSync()))
                .parallelism(dto.getParallelism())
                .sourceType(analysis.getSourceType())
                .sourceTable(analysis.getSourceTableJson())
                .sinkType(analysis.getSinkType())
                .sinkTable(analysis.getSinkTableJson())
                .createTime(now)
                .updateTime(now)
                .build();
    }

    public void update(SeatunnelBatchJobDefinitionPO po,
                       SeatunnelBatchJobDefinitionDTO dto,
                       JobDefinitionAnalysisResult analysis,
                       Date now) {
        po.setJobName(dto.getJobName());
        po.setJobDesc(dto.getJobDesc());
        po.setJobDefinitionInfo(analysis.getNormalizedJobDefinitionInfo());
        po.setClientId(dto.getClientId());
        po.setWholeSync(Boolean.TRUE.equals(dto.getWholeSync()));
        po.setParallelism(dto.getParallelism());
        po.setSourceType(analysis.getSourceType());
        po.setSourceTable(analysis.getSourceTableJson());
        po.setSinkType(analysis.getSinkType());
        po.setSinkTable(analysis.getSinkTableJson());
        po.setJobVersion(po.getJobVersion() == null ? 1 : po.getJobVersion() + 1);
        po.setUpdateTime(now);
    }
}