package org.apache.seatunnel.copilot.rule;

import org.springframework.stereotype.Component;

@Component
public class DecisionEngine {

    public ExecutionDecision decide(RiskLevel risk) {

        return switch (risk) {
            case LOW -> ExecutionDecision.AUTO_EXECUTE;
            case MEDIUM -> ExecutionDecision.MANUAL_CONFIRM;
            case HIGH -> ExecutionDecision.REJECT;
        };
    }
}
