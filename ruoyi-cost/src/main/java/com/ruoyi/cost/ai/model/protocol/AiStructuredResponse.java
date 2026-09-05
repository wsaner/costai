package com.ruoyi.cost.ai.model.protocol;

import com.fasterxml.jackson.databind.JsonNode;

public record AiStructuredResponse(JsonNode data, String rawJson, String model,
        String requestId, String finishReason, AiTokenUsage tokenUsage)
{
}
