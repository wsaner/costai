package com.ruoyi.cost.ai.chat.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/** AI造价助手会话。 */
public class AiConversation extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private String title;
    private String mode;
    private Long projectId;
    private String projectName;
    private Integer messageCount;
    private Date lastMessageTime;
    private String generating;
    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }
    public Date getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(Date lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    public String getGenerating() { return generating; }
    public void setGenerating(String generating) { this.generating = generating; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
