package com.ruoyi.cost.knowledge.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

public class KnowledgeBase extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String description;
    private String embeddingModel;
    private String vectorStore;
    private String vectorCollection;
    private String status;
    private Integer documentCount;
    private Integer chunkCount;
    private Integer topK;
    private BigDecimal similarityThreshold;
    private Integer maxContextChars;
    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public String getVectorStore() { return vectorStore; }
    public void setVectorStore(String vectorStore) { this.vectorStore = vectorStore; }
    public String getVectorCollection() { return vectorCollection; }
    public void setVectorCollection(String vectorCollection) { this.vectorCollection = vectorCollection; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getDocumentCount() { return documentCount; }
    public void setDocumentCount(Integer documentCount) { this.documentCount = documentCount; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    public BigDecimal getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(BigDecimal similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    public Integer getMaxContextChars() { return maxContextChars; }
    public void setMaxContextChars(Integer maxContextChars) { this.maxContextChars = maxContextChars; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
