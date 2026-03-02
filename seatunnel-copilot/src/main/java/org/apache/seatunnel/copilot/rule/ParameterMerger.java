package org.apache.seatunnel.copilot.rule;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ParameterMerger {

    public PipelineParameter merge(PipelineParameter base,
                                   PipelineParameter enhanced,
                                   PipelineParameter llm) {

        PipelineParameter result = new PipelineParameter();

        // 固定字段
        result.setSourceId(base.getSourceId());
        result.setSourceTable(base.getSourceTable());
        result.setSinkId(base.getSinkId());

        // split 优先规则
        result.setSplitSize(
                llm.getSplitSize() != null ?
                        llm.getSplitSize() :
                        enhanced.getSplitSize()
        );

        // LLM 优先
        result.setWriteMode(llm.getWriteMode());
        result.setSaveMode(llm.getSaveMode());

        result.setReasonMap(
                Stream.of(base, enhanced, llm)
                        .map(PipelineParameter::getReasonMap)
                        .flatMap(m -> m.entrySet().stream())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (a, b) -> b
                        ))
        );

        return result;
    }
}
