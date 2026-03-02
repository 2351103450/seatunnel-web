package org.apache.seatunnel.admin.service.impl;

import jakarta.annotation.Resource;
import org.apache.seatunnel.admin.service.SeaTunnelAiService;
import org.apache.seatunnel.copilot.intent.Intent;
import org.apache.seatunnel.copilot.intent.IntentParser;
import org.apache.seatunnel.copilot.intent.IntentRouter;
import org.apache.seatunnel.copilot.llm.LlmClient;
import org.apache.seatunnel.copilot.rule.CopilotOrchestrator;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class SeaTunnelCopilotAiServiceImpl implements SeaTunnelAiService {

    @Resource
    private IntentParser intentParser;

    @Resource
    private CopilotOrchestrator copilotOrchestrator;


    @Override
    public ChatResponse copilot(String userInput) {

        Intent intent = intentParser.parse(userInput);

        copilotOrchestrator.execute(intent);

        return null;
    }
}