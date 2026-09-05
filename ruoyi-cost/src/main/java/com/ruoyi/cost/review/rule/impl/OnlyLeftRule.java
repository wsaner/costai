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
public class OnlyLeftRule implements ReviewRule
{
    private final ReviewIssueFactory factory;
    public OnlyLeftRule(ReviewIssueFactory factory) { this.factory = factory; }
    @Override public String getCode() { return ReviewRuleCodes.ONLY_LEFT; }
    @Override public boolean supports(ReviewContext context)
    {
        return context.settings().enabled(getCode()) && !context.compareRows().isEmpty();
    }
    @Override public List<CostReviewIssue> execute(ReviewContext context)
    {
        List<CostReviewIssue> issues = new ArrayList<>();
        for (CostBoqCompare row : context.compareRows())
        {
            if (!BoqMatchType.ONLY_LEFT.name().equals(row.getMatchType())) continue;
            issues.add(factory.compare(context, row, getCode(), ReviewIssueLevel.MEDIUM,
                    "清单仅在左侧批次存在", "该清单疑似被删除，或右侧批次存在漏项，请人工确认。",
                    row.getLeftTotalPrice() == null ? null : row.getLeftTotalPrice().toPlainString(),
                    "右侧不存在", row.getLeftTotalPrice(), null, row.getLeftTotalPrice(),
                    factory.evidence("compareResultId", row.getId(), "leftItemId", row.getLeftItemId(),
                            "matchType", row.getMatchType())));
        }
        return issues;
    }
}
