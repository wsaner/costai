package com.ruoyi.cost.review.service;

import java.util.List;
import com.ruoyi.cost.review.domain.CostReviewTask;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.dto.ReviewStartRequest;
import com.ruoyi.cost.review.dto.ReviewIssueHandleRequest;

public interface CostReviewService
{
    CostReviewTask startReview(ReviewStartRequest request, String operator);
    List<CostReviewTask> selectTaskList(CostReviewTask query);
    CostReviewTask selectTaskById(Long reviewTaskId);
    List<CostReviewIssue> selectIssueList(CostReviewIssue query);
    CostReviewIssue selectIssueById(Long issueId);
    CostReviewIssue handleIssue(Long issueId, ReviewIssueHandleRequest request,
            Long reviewerUserId, String reviewer);
}
