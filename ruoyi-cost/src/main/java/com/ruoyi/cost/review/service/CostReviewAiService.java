package com.ruoyi.cost.review.service;

import com.ruoyi.cost.review.dto.CostReviewAiAnalysisResponse;
import com.ruoyi.cost.review.dto.CostReviewAiRequest;

public interface CostReviewAiService
{
    CostReviewAiAnalysisResponse analyzeIssue(Long issueId, CostReviewAiRequest request,
            Long userId, String username);
}
