package com.ruoyi.cost.review.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.dto.ReviewIssueHandleRequest;

class ReviewIssueWorkflowTest
{
    private final ReviewIssueWorkflow workflow = new ReviewIssueWorkflow();

    @Test
    void confirmsPendingIssueAndNormalizesLevelAndComment()
    {
        CostReviewIssue issue = issue("PENDING", "MEDIUM");
        ReviewIssueHandleRequest request = request("confirmed", "high", "  已复核原始清单  ");
        workflow.normalizeAndValidate(issue, request);
        assertEquals("CONFIRMED", request.getStatus());
        assertEquals("HIGH", request.getIssueLevel());
        assertEquals("已复核原始清单", request.getReviewComment());
    }

    @Test
    void retainsCurrentStatusAndLevelWhenOnlyCommentChanges()
    {
        CostReviewIssue issue = issue("IGNORED", "LOW");
        ReviewIssueHandleRequest request = request(null, null, "补充说明");
        workflow.normalizeAndValidate(issue, request);
        assertEquals("IGNORED", request.getStatus());
        assertEquals("LOW", request.getIssueLevel());
    }

    @Test
    void rejectsInvalidTransitionAndRiskLevel()
    {
        CostReviewIssue pending = issue("PENDING", "MEDIUM");
        assertThrows(ServiceException.class, () -> workflow.normalizeAndValidate(pending,
                request("RECTIFIED", "MEDIUM", "不能直接整改")));
        assertThrows(ServiceException.class, () -> workflow.normalizeAndValidate(pending,
                request("CONFIRMED", "EXTREME", "无效风险")));
    }

    private CostReviewIssue issue(String status, String level)
    {
        CostReviewIssue issue = new CostReviewIssue();
        issue.setStatus(status);
        issue.setIssueLevel(level);
        return issue;
    }

    private ReviewIssueHandleRequest request(String status, String level, String comment)
    {
        ReviewIssueHandleRequest request = new ReviewIssueHandleRequest();
        request.setStatus(status);
        request.setIssueLevel(level);
        request.setReviewComment(comment);
        return request;
    }
}
