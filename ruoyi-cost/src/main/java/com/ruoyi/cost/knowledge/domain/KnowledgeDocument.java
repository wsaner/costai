package com.ruoyi.cost.knowledge.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

public class KnowledgeDocument extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long knowledgeBaseId;
    private Long projectFileId;
    private String documentName;
    private String documentType;
    private String parseStatus;
    private Integer chunkCount;
    private Integer charCount;
    private String contentHash;
    private String embeddingModel;
    private String vectorCollection;
    private String errorMessage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date indexedTime;
    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public Long getProjectFileId() { return projectFileId; }
    public void setProjectFileId(Long projectFileId) { this.projectFileId = projectFileId; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getParseStatus() { return parseStatus; }
    public void setParseStatus(String parseStatus) { this.parseStatus = parseStatus; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public Integer getCharCount() { return charCount; }
    public void setCharCount(Integer charCount) { this.charCount = charCount; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public String getVectorCollection() { return vectorCollection; }
    public void setVectorCollection(String vectorCollection) { this.vectorCollection = vectorCollection; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getIndexedTime() { return indexedTime; }
    public void setIndexedTime(Date indexedTime) { this.indexedTime = indexedTime; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
