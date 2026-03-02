package org.apache.seatunnel.copilot.rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.apache.seatunnel.copilot.llm.LlmClient;
import org.springframework.stereotype.Component;

@Component
public class LlmParameterAdvisor {

    @Resource
    private LlmClient llmClient;

    @Resource
    private ObjectMapper objectMapper;

    public void advise(PipelineParameter parameter) {

        String prompt = buildPrompt(parameter);

        String json = llmClient.callJson("", prompt);

        try {
            JsonNode node = objectMapper.readTree(json);

            parameter.setWriteMode(node.get("writeMode").asText());
            parameter.setSaveMode(node.get("saveMode").asText());

            parameter.getReasonMap().put(
                    "writeMode",
                    node.get("writeModeReason").asText()
            );

        } catch (Exception e) {
            throw new RuntimeException("LLM 参数解析失败", e);
        }
    }

    private String buildPrompt(PipelineParameter p) {
        return """
                你是数据同步参数顾问。
                必须输出JSON：

                {
                  "writeMode": "",
                  "writeModeReason": "",
                  "saveMode": ""
                }

                sourceTable: %s
                """.formatted(p.getSourceTable());
    }
}
