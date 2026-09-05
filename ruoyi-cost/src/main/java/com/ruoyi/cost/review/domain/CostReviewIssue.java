package com.ruoyi.cost.review.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.domain.BaseEntity;

/** 确定性审核规则发现的问题。 */
public class CostReviewIssue extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long reviewTaskId;
    private Long projectId;
    private Long leftItemId;
    private Long rightItemId;
    private Long boqItemId;
    private Long compareResultId;
    private String itemSide;
    private String itemCodeSnapshot;
    private String itemNameSnapshot;
    private String issueType;
    private String issueLevel;
    private String issueTitle;
    private String issueDescription;
    private String originalValue;
    private String referenceValue;
    private BigDecimal differenceValue;
    private BigDecimal differenceRate;
    private BigDecimal riskAmount = BigDecimal.ZERO;
    private String ruleCode;
    private String evidenceJson;
    private String aiAnalysis;
    private String aiSuggestion;
    private BigDecimal aiConfidence;
    private String aiHasIssue;
    private String aiIssueType;
    private String aiIssueLevel;
    private String aiTitle;
    private String aiModel;
    private String aiRequestId;
    private Long aiAnalyzedUserId;
    private String aiAnalyzedBy;
    private Date aiAnalyzedTime;
    private Boolean aiEligible;
    private String aiEligibilityReason;
    private String status;
    private Long reviewerUserId;
    private String reviewer;
    private String reviewComment;
    private Date reviewTime;
    private String keyword;
    @JsonIgnore
    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReviewTaskId() { return reviewTaskId; }
    public void setReviewTaskId(Long reviewTaskId) { this.reviewTaskId = reviewTaskId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getLeftItemId() { return leftItemId; }
    public void setLeftItemId(Long leftItemId) { this.leftItemId = leftItemId; }
    public Long getRightItemId() { return rightItemId; }
    public void setRightItemId(Long rightItemId) { this.rightItemId = rightItemId; }
    public Long getBoqItemId() { return boqItemId; }
    public void setBoqItemId(Long boqItemId) { this.boqItemId = boqItemId; }
    public Long getCompareResultId() { return compareResultId; }
    public void setCompareResultId(Long compareResultId) { this.compareResultId = compareResultId; }
    public String getItemSide() { return itemSide; }
    public void setItemSide(String itemSide) { this.itemSide = itemSide; }
    public String getItemCodeSnapshot() { return itemCodeSnapshot; }
    public void setItemCodeSnapshot(String itemCodeSnapshot) { this.itemCodeSnapshot = itemCodeSnapshot; }
    public String getItemNameSnapshot() { return itemNameSnapshot; }
    public void setItemNameSnapshot(String itemNameSnapshot) { this.itemNameSnapshot = itemNameSnapshot; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public String getIssueLevel() { return issueLevel; }
    public void setIssueLevel(String issueLevel) { this.issueLevel = issueLevel; }
    public String getIssueTitle() { return issueTitle; }
    public void setIssueTitle(String issueTitle) { this.issueTitle = issueTitle; }
    public String getIssueDescription() { return issueDescription; }
    public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }
    public String getOriginalValue() { return originalValue; }
    public void setOriginalValue(String originalValue) { this.originalValue = originalValue; }
    public String getReferenceValue() { return referenceValue; }
    public void setReferenceValue(String referenceValue) { this.referenceValue = referenceValue; }
    public BigDecimal getDifferenceValue() { return differenceValue; }
    public void setDifferenceValue(BigDecimal differenceValue) { this.differenceValue = differenceValue; }
    public BigDecimal getDifferenceRate() { return differenceRate; }
    public void setDifferenceRate(BigDecimal differenceRate) { this.differenceRate = differenceRate; }
    public BigDecimal getRiskAmount() { return riskAmount; }
    public void setRiskAmount(BigDecimal riskAmount) { this.riskAmount = riskAmount; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getEvidenceJson() { return evidenceJson; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public String getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(String aiAnalysis) { this.aiAnalysis = aiAnalysis; }
    public String getAiSuggestion() { return aiSuggestion; }
    public void setAiSuggestion(String aiSuggestion) { this.aiSuggestion = aiSuggestion; }
    public BigDecimal getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(BigDecimal aiConfidence) { this.aiConfidence = aiConfidence; }
    public String getAiHasIssue() { return aiHasIssue; }
    public void setAiHasIssue(String aiHasIssue) { this.aiHasIssue = aiHasIssue; }
    public String getAiIssueType() { return aiIssueType; }
    public void setAiIssueType(String aiIssueType) { this.aiIssueType = aiIssueType; }
    public String getAiIssueLevel() { return aiIssueLevel; }
    public void setAiIssueLevel(String aiIssueLevel) { this.aiIssueLevel = aiIssueLevel; }
    public String getAiTitle() { return aiTitle; }
    public void setAiTitle(String aiTitle) { this.aiTitle = aiTitle; }
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
    public String getAiRequestId() { return aiRequestId; }
    public void setAiRequestId(String aiRequestId) { this.aiRequestId = aiRequestId; }
    public Long getAiAnalyzedUserId() { return aiAnalyzedUserId; }
    public void setAiAnalyzedUserId(Long aiAnalyzedUserId) { this.aiAnalyzedUserId = aiAnalyzedUserId; }
    public String getAiAnalyzedBy() { return aiAnalyzedBy; }
    public void setAiAnalyzedBy(String aiAnalyzedBy) { this.aiAnalyzedBy = aiAnalyzedBy; }
    public Date getAiAnalyzedTime() { return aiAnalyzedTime; }
    public void setAiAnalyzedTime(Date aiAnalyzedTime) { this.aiAnalyzedTime = aiAnalyzedTime; }
    public Boolean getAiEligible() { return aiEligible; }
    public void setAiEligible(Boolean aiEligible) { this.aiEligible = aiEligible; }
    public String getAiEligibilityReason() { return aiEligibilityReason; }
    public void setAiEligibilityReason(String aiEligibilityReason) { this.aiEligibilityReason = aiEligibilityReason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getReviewerUserId() { return reviewerUserId; }
    public void setReviewerUserId(Long reviewerUserId) { this.reviewerUserId = reviewerUserId; }
    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
    public Date getReviewTime() { return reviewTime; }
    public void setReviewTime(Date reviewTime) { this.reviewTime = reviewTime; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
