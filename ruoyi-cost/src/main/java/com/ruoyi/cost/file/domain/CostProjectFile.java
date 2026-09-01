package com.ruoyi.cost.file.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 项目文件对象 cost_project_file。
 */
public class CostProjectFile extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String fileId;
    private String originalName;
    private String fileName;
    private String fileExt;
    private String mimeType;
    private Long fileSize;
    private String storagePath;
    private String fileCategory;
    private String fileHash;
    private String aiParseStatus;
    private String aiParseText;
    private String aiParseError;
    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileExt() { return fileExt; }
    public void setFileExt(String fileExt) { this.fileExt = fileExt; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    @JsonIgnore
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getFileCategory() { return fileCategory; }
    public void setFileCategory(String fileCategory) { this.fileCategory = fileCategory; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public String getAiParseStatus() { return aiParseStatus; }
    public void setAiParseStatus(String aiParseStatus) { this.aiParseStatus = aiParseStatus; }
    @JsonIgnore
    public String getAiParseText() { return aiParseText; }
    public void setAiParseText(String aiParseText) { this.aiParseText = aiParseText; }
    public String getAiParseError() { return aiParseError; }
    public void setAiParseError(String aiParseError) { this.aiParseError = aiParseError; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
