package com.ruoyi.cost.review.dto;

import com.ruoyi.cost.review.agent.CostReviewAgentResult;
import com.ruoyi.cost.review.domain.CostReviewIssue;

/** 同时返回原始结构化结论和落库后的审核问题快照。 */
public record CostReviewAiAnalysisResponse(CostReviewAgentResult analysis, CostReviewIssue issue)
{
}
