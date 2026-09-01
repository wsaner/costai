package com.ruoyi.cost.file.vo;

import java.util.Date;

/** 项目文件AI解析状态。 */
public class CostProjectFileStatusVo
{
    private Long id;
    private String fileId;
    private String aiParseStatus;
    private String aiParseError;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getAiParseStatus() { return aiParseStatus; }
    public void setAiParseStatus(String aiParseStatus) { this.aiParseStatus = aiParseStatus; }
    public String getAiParseError() { return aiParseError; }
    public void setAiParseError(String aiParseError) { this.aiParseError = aiParseError; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
