package com.ruoyi.cost.knowledge.vector;

import java.util.Map;

public record VectorSearchResult(String id, double score, Map<String, Object> payload)
{
}
