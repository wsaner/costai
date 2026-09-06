package com.ruoyi.cost.knowledge.dto;

import jakarta.validation.constraints.NotNull;

public class KnowledgeDocumentAttachRequest
{
    @NotNull
    private Long projectFileId;
    public Long getProjectFileId() { return projectFileId; }
    public void setProjectFileId(Long projectFileId) { this.projectFileId = projectFileId; }
}
