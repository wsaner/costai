package com.ruoyi.cost.knowledge.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class KnowledgeChunk extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long knowledgeBaseId;
    private Long documentId;
    private String content;
    private Integer pageNumber;
    private String sectionTitle;
    private Integer chunkIndex;
    private Integer charCount;
    private String contentHash;
    private String metadataJson;
    private String vectorStore;
    private String vectorCollection;
    private String vectorId;
    private String indexStatus;
    private String documentName;
    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(Long knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
    public String getSectionTitle() { return sectionTitle; }
    public void setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public Integer getCharCount() { return charCount; }
    public void setCharCount(Integer charCount) { this.charCount = charCount; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public String getVectorStore() { return vectorStore; }
    public void setVectorStore(String vectorStore) { this.vectorStore = vectorStore; }
    public String getVectorCollection() { return vectorCollection; }
    public void setVectorCollection(String vectorCollection) { this.vectorCollection = vectorCollection; }
    public String getVectorId() { return vectorId; }
    public void setVectorId(String vectorId) { this.vectorId = vectorId; }
    public String getIndexStatus() { return indexStatus; }
    public void setIndexStatus(String indexStatus) { this.indexStatus = indexStatus; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
