package com.ruoyi.cost.review.rule.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.rule.ReviewContext;
import com.ruoyi.cost.review.rule.ReviewIssueFactory;
import com.ruoyi.cost.review.rule.ReviewRule;
import com.ruoyi.cost.review.support.ReviewIssueLevel;

abstract class AbstractDifferenceRule implements ReviewRule
{
    protected final ReviewIssueFactory factory;
    AbstractDifferenceRule(ReviewIssueFactory factory) { this.factory = factory; }
    @Override public boolean supports(ReviewContext context)
    {
        return context.settings().enabled(getCode()) && !context.compareRows().isEmpty();
    }

    @Override
    public List<CostReviewIssue> execute(ReviewContext context)
    {
        BigDecimal warningRate = context.settings().decimal(getCode(), "warningRate");
        BigDecimal highRate = context.settings().decimal(getCode(), "highRate");
        List<CostReviewIssue> issues = new ArrayList<>();
        for (CostBoqCompare row : context.compareRows())
        {
            BigDecimal left = left(row);
            BigDecimal right = right(row);
            if (left == null || right == null) continue;
            BigDecimal difference = left.subtract(right).setScale(valueScale(), RoundingMode.HALF_UP);
            BigDecimal rate = rate(difference, left, right);
            if (rate == null || rate.abs().compareTo(warningRate) < 0) continue;
            ReviewIssueLevel level = rate.abs().compareTo(highRate) >= 0
                    ? ReviewIssueLevel.HIGH : ReviewIssueLevel.MEDIUM;
            issues.add(factory.compare(context, row, getCode(), level, title(), description(),
                    left.toPlainString(), right.toPlainString(), difference, rate,
                    risk(row, difference), factory.evidence("compareResultId", row.getId(),
                            "leftItemId", row.getLeftItemId(), "rightItemId", row.getRightItemId(),
                            "leftValue", left, "rightValue", right, "difference", difference,
                            "differenceRate", rate, "warningRate", warningRate, "highRate", highRate)));
        }
        return issues;
    }

    protected BigDecimal rate(BigDecimal difference, BigDecimal left, BigDecimal right)
    {
        BigDecimal denominator = left.abs().compareTo(BigDecimal.ZERO) == 0 ? right.abs() : left.abs();
        if (denominator.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return difference.divide(denominator, 6, RoundingMode.HALF_UP);
    }

    protected BigDecimal multiply(BigDecimal first, BigDecimal second)
    {
        return first == null || second == null ? BigDecimal.ZERO : first.multiply(second);
    }

    protected abstract BigDecimal left(CostBoqCompare row);
    protected abstract BigDecimal right(CostBoqCompare row);
    protected abstract BigDecimal risk(CostBoqCompare row, BigDecimal difference);
    protected abstract String title();
    protected abstract String description();
    protected abstract int valueScale();
}
