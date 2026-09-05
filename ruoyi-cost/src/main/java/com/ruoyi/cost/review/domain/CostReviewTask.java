package com.ruoyi.cost.review.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.domain.BaseEntity;

/** 一次可追溯的造价审核任务。 */
public class CostReviewTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long projectId;
    private Long leftBatchId;
    private Long rightBatchId;
    private String taskName;
    private String leftBatchName;
    private String rightBatchName;
    private String status;
    private String ruleVersion;
    @JsonIgnore private String configSnapshotJson;
    private Integer leftItemCount;
    private Integer rightItemCount;
    private Integer compareCount;
    private Integer issueCount;
    private Integer mediumCount;
    private Integer highCount;
    private Integer criticalCount;
    private BigDecimal riskAmount;
    private BigDecimal reductionAmount;
    private Integer quantityIssueCount;
    private Integer unitPriceIssueCount;
    private Integer missingIssueCount;
    private Integer duplicateIssueCount;
    private String startedBy;
    private Date startTime;
    private Date finishTime;
    private String errorMessage;
    @JsonIgnore private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getLeftBatchId() { return leftBatchId; }
    public void setLeftBatchId(Long leftBatchId) { this.leftBatchId = leftBatchId; }
    public Long getRightBatchId() { return rightBatchId; }
    public void setRightBatchId(Long rightBatchId) { this.rightBatchId = rightBatchId; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getLeftBatchName() { return leftBatchName; }
    public void setLeftBatchName(String leftBatchName) { this.leftBatchName = leftBatchName; }
    public String getRightBatchName() { return rightBatchName; }
    public void setRightBatchName(String rightBatchName) { this.rightBatchName = rightBatchName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }
    public String getConfigSnapshotJson() { return configSnapshotJson; }
    public void setConfigSnapshotJson(String value) { this.configSnapshotJson = value; }
    public Integer getLeftItemCount() { return leftItemCount; }
    public void setLeftItemCount(Integer value) { this.leftItemCount = value; }
    public Integer getRightItemCount() { return rightItemCount; }
    public void setRightItemCount(Integer value) { this.rightItemCount = value; }
    public Integer getCompareCount() { return compareCount; }
    public void setCompareCount(Integer value) { this.compareCount = value; }
    public Integer getIssueCount() { return issueCount; }
    public void setIssueCount(Integer value) { this.issueCount = value; }
    public Integer getMediumCount() { return mediumCount; }
    public void setMediumCount(Integer value) { this.mediumCount = value; }
    public Integer getHighCount() { return highCount; }
    public void setHighCount(Integer value) { this.highCount = value; }
    public Integer getCriticalCount() { return criticalCount; }
    public void setCriticalCount(Integer value) { this.criticalCount = value; }
    public BigDecimal getRiskAmount() { return riskAmount; }
    public void setRiskAmount(BigDecimal value) { this.riskAmount = value; }
    public BigDecimal getReductionAmount() { return reductionAmount; }
    public void setReductionAmount(BigDecimal value) { this.reductionAmount = value; }
    public Integer getQuantityIssueCount() { return quantityIssueCount; }
    public void setQuantityIssueCount(Integer value) { this.quantityIssueCount = value; }
    public Integer getUnitPriceIssueCount() { return unitPriceIssueCount; }
    public void setUnitPriceIssueCount(Integer value) { this.unitPriceIssueCount = value; }
    public Integer getMissingIssueCount() { return missingIssueCount; }
    public void setMissingIssueCount(Integer value) { this.missingIssueCount = value; }
    public Integer getDuplicateIssueCount() { return duplicateIssueCount; }
    public void setDuplicateIssueCount(Integer value) { this.duplicateIssueCount = value; }
    public String getStartedBy() { return startedBy; }
    public void setStartedBy(String value) { this.startedBy = value; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date value) { this.startTime = value; }
    public Date getFinishTime() { return finishTime; }
    public void setFinishTime(Date value) { this.finishTime = value; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String value) { this.errorMessage = value; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String value) { this.delFlag = value; }
}
