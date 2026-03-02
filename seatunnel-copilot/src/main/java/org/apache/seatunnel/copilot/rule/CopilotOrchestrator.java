package org.apache.seatunnel.copilot.rule;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import org.apache.seatunnel.copilot.intent.Intent;
import org.springframework.stereotype.Component;

@Component
public class CopilotOrchestrator {

    @Resource
    private BaseParameterBuilder baseBuilder;

    @Resource
    private RuleParameterEnhancer ruleEnhancer;

    @Resource
    private LlmParameterAdvisor llmAdvisor;

    @Resource
    private ParameterValidator validator;

    @Resource
    private RiskEvaluator riskEvaluator;

    @Resource
    private DecisionEngine decisionEngine;

    @Resource
    private NodeBuilder nodeBuilder;

    public JsonNode execute(Intent intent) {

        PipelineParameter parameter = baseBuilder.build(intent);

        ruleEnhancer.enhance(parameter);

        llmAdvisor.advise(parameter);

        validator.validate(parameter);

        RiskLevel risk = riskEvaluator.evaluate(parameter);

        ExecutionDecision decision = decisionEngine.decide(risk);

        if (decision == ExecutionDecision.REJECT) {
            throw new RuntimeException("高风险操作，拒绝执行");
        }

        return nodeBuilder.build(parameter);
    }
}
