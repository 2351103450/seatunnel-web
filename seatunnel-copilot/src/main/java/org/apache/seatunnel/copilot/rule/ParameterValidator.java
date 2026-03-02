package org.apache.seatunnel.copilot.rule;

import org.springframework.stereotype.Component;

@Component
public class ParameterValidator {

    public void validate(PipelineParameter p) {

        if (p.getSourceId() == null) {
            throw new IllegalArgumentException("sourceId 不能为空");
        }

        if (p.getSplitSize() != null && p.getSplitSize() <= 0) {
            throw new IllegalArgumentException("splitSize 必须大于0");
        }
    }
}
