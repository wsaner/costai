package com.ruoyi.cost.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 人工处理审核问题。 */
public class ReviewIssueHandleRequest
{
    private String status;
    private String issueLevel;
    @NotBlank(message = "审核意见不能为空")
    @Size(max = 2000, message = "审核意见不能超过2000个字符")
    private String reviewComment;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIssueLevel() { return issueLevel; }
    public void setIssueLevel(String issueLevel) { this.issueLevel = issueLevel; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
}
