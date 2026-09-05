package com.ruoyi.cost.review.rule.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.boq.match.support.BoqMatchType;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.rule.ReviewContext;
import com.ruoyi.cost.review.rule.ReviewIssueFactory;
import com.ruoyi.cost.review.rule.ReviewRule;
import com.ruoyi.cost.review.support.ReviewIssueLevel;
import com.ruoyi.cost.review.support.ReviewRuleCodes;

@Component
public class OnlyRightRule implements ReviewRule
{
    private final ReviewIssueFactory factory;
    public OnlyRightRule(ReviewIssueFactory factory) { this.factory = factory; }
    @Override public String getCode() { return ReviewRuleCodes.ONLY_RIGHT; }
    @Override public boolean supports(ReviewContext context)
    {
        return context.settings().enabled(getCode()) && !context.compareRows().isEmpty();
    }
    @Override public List<CostReviewIssue> execute(ReviewContext context)
    {
        List<CostReviewIssue> issues = new ArrayList<>();
        for (CostBoqCompare row : context.compareRows())
        {
            if (!BoqMatchType.ONLY_RIGHT.name().equals(row.getMatchType())) continue;
            issues.add(factory.compare(context, row, getCode(), ReviewIssueLevel.MEDIUM,
                    "清单仅在右侧批次存在", "该清单疑似新增，或左侧批次存在漏项，请人工确认。",
                    "左侧不存在", row.getRightTotalPrice() == null ? null : row.getRightTotalPrice().toPlainString(),
                    row.getRightTotalPrice() == null ? null : row.getRightTotalPrice().negate(), null,
                    row.getRightTotalPrice(), factory.evidence("compareResultId", row.getId(),
                            "rightItemId", row.getRightItemId(), "matchType", row.getMatchType())));
        }
        return issues;
    }
}
