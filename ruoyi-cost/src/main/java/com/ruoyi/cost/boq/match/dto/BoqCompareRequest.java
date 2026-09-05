package com.ruoyi.cost.boq.match.dto;

import jakarta.validation.constraints.NotNull;

/** 首次匹配或重新匹配请求。 */
public class BoqCompareRequest
{
    @NotNull(message = "项目ID不能为空")
    private Long projectId;
    @NotNull(message = "左侧批次不能为空")
    private Long leftBatchId;
    @NotNull(message = "右侧批次不能为空")
    private Long rightBatchId;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getLeftBatchId() { return leftBatchId; }
    public void setLeftBatchId(Long leftBatchId) { this.leftBatchId = leftBatchId; }
    public Long getRightBatchId() { return rightBatchId; }
    public void setRightBatchId(Long rightBatchId) { this.rightBatchId = rightBatchId; }
}
