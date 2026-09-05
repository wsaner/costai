package com.ruoyi.cost.review.agent;

import java.math.BigDecimal;
import com.ruoyi.cost.ai.model.protocol.AiTokenUsage;

/** CostReviewAgent 的结构化结论；所有字段均为建议，不代表人工审核结论。 */
public class CostReviewAgentResult
{
    private boolean hasIssue;
    private String issueType;
    private String riskLevel;
    private String title;
    private String analysis;
    private String suggestion;
    private BigDecimal confidence;
    private String model;
    private String requestId;
    private AiTokenUsage tokenUsage;

    public boolean isHasIssue() { return hasIssue; }
    public void setHasIssue(boolean hasIssue) { this.hasIssue = hasIssue; }
    public String getHasIssueFlag() { return hasIssue ? "Y" : "N"; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public AiTokenUsage getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(AiTokenUsage tokenUsage) { this.tokenUsage = tokenUsage; }
}
