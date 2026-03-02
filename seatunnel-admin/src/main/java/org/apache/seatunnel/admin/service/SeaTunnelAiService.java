package org.apache.seatunnel.admin.service;

import org.springframework.ai.chat.model.ChatResponse;

public interface SeaTunnelAiService {
    ChatResponse copilot(String prompt);
}
