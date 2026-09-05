package com.ruoyi.cost.ai.model.protocol;

public record AiChatResponse(String content, String model, String requestId,
        String finishReason, AiTokenUsage tokenUsage)
{
}
