package com.ruoyi.cost.review.rule.impl;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.review.rule.ReviewIssueFactory;
import com.ruoyi.cost.review.support.ReviewRuleCodes;

@Component
public class QuantityDifferenceRule extends AbstractDifferenceRule
{
    public QuantityDifferenceRule(ReviewIssueFactory factory) { super(factory); }
    @Override public String getCode() { return ReviewRuleCodes.QUANTITY_DIFF; }
    @Override protected BigDecimal left(CostBoqCompare row) { return row.getLeftQuantity(); }
    @Override protected BigDecimal right(CostBoqCompare row) { return row.getRightQuantity(); }
    @Override protected BigDecimal risk(CostBoqCompare row, BigDecimal difference)
    {
        BigDecimal price = row.getRightUnitPrice() != null ? row.getRightUnitPrice() : row.getLeftUnitPrice();
        return multiply(difference.abs(), price);
    }
    @Override protected String title() { return "左右清单工程量差异超阈值"; }
    @Override protected String description() { return "左右批次已匹配清单的工程量差异率超过规则配置。"; }
    @Override protected int valueScale() { return 8; }
}
