package org.apache.seatunnel.copilot.rule;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class RuleParameterEnhancer {

//    @Resource
//    private MetadataService metadataService;

    public void enhance(PipelineParameter parameter) {

        long tableCount = 6000;

        int splitSize;

        if (tableCount < 100_000) {
            splitSize = 1;
        } else if (tableCount < 10_000_000) {
            splitSize = 4;
        } else {
            splitSize = 8;
        }

        parameter.setSplitSize(splitSize);
        parameter.getReasonMap().put("splitSize",
                "根据表数据量 " + tableCount + " 自动计算 split.size=" + splitSize);
    }
}
