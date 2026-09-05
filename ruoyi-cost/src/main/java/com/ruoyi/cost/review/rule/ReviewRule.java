package com.ruoyi.cost.review.rule;

import java.util.List;
import com.ruoyi.cost.review.domain.CostReviewIssue;

/** 造价确定性审核规则统一接口。 */
public interface ReviewRule
{
    String getCode();
    boolean supports(ReviewContext context);
    List<CostReviewIssue> execute(ReviewContext context);
}
