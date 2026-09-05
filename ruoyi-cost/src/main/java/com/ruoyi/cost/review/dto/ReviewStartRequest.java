package com.ruoyi.cost.review.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 启动两个已匹配清单批次的确定性规则审核。 */
public class ReviewStartRequest
{
    @NotNull(message = "项目ID不能为空")
    private Long projectId;
    @NotNull(message = "左侧批次不能为空")
    private Long leftBatchId;
    @NotNull(message = "右侧批次不能为空")
    private Long rightBatchId;
    @Size(max = 200, message = "任务名称不能超过200个字符")
    private String taskName;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getLeftBatchId() { return leftBatchId; }
    public void setLeftBatchId(Long leftBatchId) { this.leftBatchId = leftBatchId; }
    public Long getRightBatchId() { return rightBatchId; }
    public void setRightBatchId(Long rightBatchId) { this.rightBatchId = rightBatchId; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
}
