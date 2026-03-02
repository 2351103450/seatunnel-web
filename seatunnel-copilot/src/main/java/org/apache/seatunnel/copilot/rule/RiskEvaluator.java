package org.apache.seatunnel.copilot.rule;

import org.springframework.stereotype.Component;

@Component
public class RiskEvaluator {

    public RiskLevel evaluate(PipelineParameter p) {

        if (p.getSplitSize() != null && p.getSplitSize() > 16) {
            return RiskLevel.HIGH;
        }

        return RiskLevel.LOW;
    }
}
