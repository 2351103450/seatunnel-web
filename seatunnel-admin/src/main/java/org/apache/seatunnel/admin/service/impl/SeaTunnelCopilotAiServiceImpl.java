package org.apache.seatunnel.admin.service.impl;

import jakarta.annotation.Resource;
import org.apache.seatunnel.admin.service.SeaTunnelAiService;
import org.apache.seatunnel.communal.bean.dto.AiGenerateRequest;
import org.apache.seatunnel.copilot.intent.ParseResponse;
import org.apache.seatunnel.copilot.intent.service.IntentParseService;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class SeaTunnelCopilotAiServiceImpl implements SeaTunnelAiService {

    @Resource
    private IntentParseService intentParseService;


    @Override
    public ChatResponse copilot(AiGenerateRequest aiGenerateRequest) {

        ParseResponse parseResponse = intentParseService.parseAndValidate(aiGenerateRequest);

        System.out.println("parseResponse = " + parseResponse);

        return null;
    }
}