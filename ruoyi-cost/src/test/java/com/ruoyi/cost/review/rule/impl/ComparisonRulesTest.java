package com.ruoyi.cost.review.rule.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.rule.ReviewContext;
import com.ruoyi.cost.review.rule.ReviewIssueFactory;
import com.ruoyi.cost.review.rule.ReviewRuleTestSupport;

class ComparisonRulesTest
{
    private final ReviewIssueFactory factory = ReviewRuleTestSupport.factory();

    @Test
    void appliesWarningAndHighBoundariesWithZeroLeftFallback()
    {
        CostBoqCompare warning = ReviewRuleTestSupport.compare(1, "EXACT",
                "100", "90", "100", "90", "1000", "900");
        CostBoqCompare high = ReviewRuleTestSupport.compare(2, "EXACT",
                "100", "70", "100", "80", "1000", "800");
        CostBoqCompare zeroLeft = ReviewRuleTestSupport.compare(3, "EXACT",
                "0", "10", "0", "10", "0", "10");
        ReviewContext context = ReviewRuleTestSupport.context(List.of(), List.of(),
                List.of(warning, high, zeroLeft));

        List<CostReviewIssue> quantity = new QuantityDifferenceRule(factory).execute(context);
        assertEquals(3, quantity.size());
        assertEquals("MEDIUM", quantity.get(0).getIssueLevel());
        assertEquals("HIGH", quantity.get(1).getIssueLevel());
        assertEquals(new BigDecimal("-1.000000"), quantity.get(2).getDifferenceRate());

        List<CostReviewIssue> price = new UnitPriceDifferenceRule(factory).execute(context);
        assertEquals(3, price.size());
        assertEquals("UNIT_PRICE", price.get(0).getIssueType());
        assertEquals("MEDIUM", price.get(0).getIssueLevel());
        assertEquals("HIGH", price.get(1).getIssueLevel());

        List<CostReviewIssue> total = new TotalPriceDifferenceRule(factory).execute(context);
        assertEquals(3, total.size());
    }

    @Test
    void detectsOnlyLeftAndOnlyRightRows()
    {
        CostBoqCompare left = ReviewRuleTestSupport.compare(1, "ONLY_LEFT",
                "1", null, "2", null, "2", null);
        CostBoqCompare right = ReviewRuleTestSupport.compare(2, "ONLY_RIGHT",
                null, "1", null, "3", null, "3");
        ReviewContext context = ReviewRuleTestSupport.context(List.of(), List.of(), List.of(left, right));

        List<CostReviewIssue> leftIssues = new OnlyLeftRule(factory).execute(context);
        List<CostReviewIssue> rightIssues = new OnlyRightRule(factory).execute(context);
        assertEquals(1, leftIssues.size());
        assertEquals(new BigDecimal("2.000000"), leftIssues.get(0).getRiskAmount());
        assertEquals(1, rightIssues.size());
        assertEquals(new BigDecimal("-3"), rightIssues.get(0).getDifferenceValue());
        assertEquals(new BigDecimal("3.000000"), rightIssues.get(0).getRiskAmount());
    }
}
