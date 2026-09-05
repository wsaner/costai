package com.ruoyi.cost.review.agent;

/** 单个审核问题的有限上下文；reviewContextJson 只包含左右各一条清单。 */
public record CostReviewAgentContext(Long issueId, Long modelConfigId, Long userId,
        String username, String reviewContextJson)
{
}
