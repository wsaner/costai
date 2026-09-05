package com.ruoyi.cost.review.rule.impl;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.review.rule.ReviewIssueFactory;
import com.ruoyi.cost.review.support.ReviewRuleCodes;

@Component
public class UnitPriceDifferenceRule extends AbstractDifferenceRule
{
    public UnitPriceDifferenceRule(ReviewIssueFactory factory) { super(factory); }
    @Override public String getCode() { return ReviewRuleCodes.UNIT_PRICE_DIFF; }
    @Override protected BigDecimal left(CostBoqCompare row) { return row.getLeftUnitPrice(); }
    @Override protected BigDecimal right(CostBoqCompare row) { return row.getRightUnitPrice(); }
    @Override protected BigDecimal risk(CostBoqCompare row, BigDecimal difference)
    {
        BigDecimal quantity = row.getRightQuantity() != null ? row.getRightQuantity() : row.getLeftQuantity();
        return multiply(difference.abs(), quantity);
    }
    @Override protected String title() { return "左右清单综合单价差异超阈值"; }
    @Override protected String description() { return "左右批次已匹配清单的综合单价差异率超过规则配置。"; }
    @Override protected int valueScale() { return 8; }
}
