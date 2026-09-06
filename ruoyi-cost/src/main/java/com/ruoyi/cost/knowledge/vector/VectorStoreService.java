package com.ruoyi.cost.knowledge.vector;

import java.math.BigDecimal;
import java.util.List;

/** 向量存储边界，知识库业务不得依赖Qdrant协议对象。 */
public interface VectorStoreService
{
    void saveVectors(String collection, int dimension, List<VectorRecord> records);
    List<VectorSearchResult> searchSimilar(String collection, List<Double> query, int topK, BigDecimal threshold);
    void deleteDocumentVectors(String collection, Long documentId);
    void deleteCollection(String collection);
}
