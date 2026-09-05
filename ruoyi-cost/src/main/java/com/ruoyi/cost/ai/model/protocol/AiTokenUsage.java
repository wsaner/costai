package com.ruoyi.cost.ai.model.protocol;

public record AiTokenUsage(int promptTokens, int completionTokens, int totalTokens)
{
    public static final AiTokenUsage EMPTY = new AiTokenUsage(0, 0, 0);
}
