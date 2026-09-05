package com.ruoyi.cost.review.rule.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.rule.ReviewContext;
import com.ruoyi.cost.review.rule.ReviewIssueFactory;
import com.ruoyi.cost.review.rule.ReviewRuleTestSupport;

class ItemValidationRulesTest
{
    private final ReviewIssueFactory factory = ReviewRuleTestSupport.factory();

    @Test
    void detectsNegativeQuantityNegativePriceAndNonExemptZeroPrice()
    {
        CostBoqItem negativeQuantity = item(1, "-1", "10", "-10", "混凝土");
        CostBoqItem negativePrice = item(2, "1", "-10", "-10", "钢筋");
        CostBoqItem zeroPrice = item(3, "1", "0", "0", "普通清单");
        CostBoqItem exempt = item(4, "1", "0", "0", "暂估价材料");
        ReviewContext context = ReviewRuleTestSupport.context(
                List.of(negativeQuantity, negativePrice, zeroPrice, exempt), List.of(), List.of());

        List<CostReviewIssue> quantityIssues = new NegativeQuantityRule(factory).execute(context);
        assertEquals(1, quantityIssues.size());
        assertEquals("QUANTITY", quantityIssues.get(0).getIssueType());
        assertEquals(1L, quantityIssues.get(0).getLeftItemId());
        List<CostReviewIssue> priceIssues = new NegativeUnitPriceRule(factory).execute(context);
        assertEquals(1, priceIssues.size());
        assertEquals("UNIT_PRICE", priceIssues.get(0).getIssueType());
        List<CostReviewIssue> zeroIssues = new ZeroUnitPriceRule(factory).execute(context);
        assertEquals(1, zeroIssues.size());
        assertEquals(3L, zeroIssues.get(0).getBoqItemId());
    }

    @Test
    void totalCalculationHonorsAbsoluteAndRelativeToleranceAndBigDecimalPrecision()
    {
        CostBoqItem withinAbsolute = item(1, "3", "0.10", "0.31", "舍入容差");
        CostBoqItem withinRelative = item(2, "1000000000000", "1.000000", "1000000000100", "超大金额");
        CostBoqItem mismatch = item(3, "3", "10.00", "31.00", "合价错误");
        CostBoqItem zeroCalculated = item(4, "0", "0", "1", "零基数异常");
        CostBoqItem missing = item(5, null, "10", "10", "空值");
        ReviewContext context = ReviewRuleTestSupport.context(
                List.of(withinAbsolute, withinRelative, mismatch, zeroCalculated, missing),
                List.of(), List.of());

        List<CostReviewIssue> issues = new TotalCalculationRule(factory).execute(context);
        assertEquals(2, issues.size());
        assertTrue(issues.stream().anyMatch(issue -> issue.getBoqItemId().equals(3L)));
        assertTrue(issues.stream().anyMatch(issue -> issue.getBoqItemId().equals(4L)));
        assertEquals("1.00000000", issues.stream().filter(issue -> issue.getBoqItemId().equals(3L))
                .findFirst().orElseThrow().getDifferenceValue().toPlainString());
    }

    private CostBoqItem item(long id, String quantity, String price, String total, String name)
    {
        return ReviewRuleTestSupport.item(id, 10L, "C" + id, name, "", "m2",
                quantity, price, total);
    }
}
