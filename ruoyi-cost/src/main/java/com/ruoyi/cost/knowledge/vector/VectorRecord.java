package com.ruoyi.cost.knowledge.vector;

import java.util.List;
import java.util.Map;

public record VectorRecord(Object id, List<Double> vector, Map<String, Object> payload)
{
}
