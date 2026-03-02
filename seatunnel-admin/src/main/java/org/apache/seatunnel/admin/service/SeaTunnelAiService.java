package org.apache.seatunnel.admin.service;

import org.apache.seatunnel.communal.bean.dto.AiGenerateRequest;
import org.springframework.ai.chat.model.ChatResponse;

public interface SeaTunnelAiService {
    ChatResponse copilot(AiGenerateRequest aiGenerateRequest);
}
