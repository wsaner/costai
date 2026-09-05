package com.ruoyi.cost.ai.model.protocol;

import java.util.List;

public record AiEmbeddingResponse(List<List<Double>> embeddings, String model,
        String requestId, AiTokenUsage tokenUsage)
{
}
