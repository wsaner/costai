package com.ruoyi.cost.review.rule.impl;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.review.rule.ReviewIssueFactory;
import com.ruoyi.cost.review.support.ReviewRuleCodes;

@Component
public class TotalPriceDifferenceRule extends AbstractDifferenceRule
{
    public TotalPriceDifferenceRule(ReviewIssueFactory factory) { super(factory); }
    @Override public String getCode() { return ReviewRuleCodes.TOTAL_PRICE_DIFF; }
    @Override protected BigDecimal left(CostBoqCompare row) { return row.getLeftTotalPrice(); }
    @Override protected BigDecimal right(CostBoqCompare row) { return row.getRightTotalPrice(); }
    @Override protected BigDecimal risk(CostBoqCompare row, BigDecimal difference) { return difference.abs(); }
    @Override protected String title() { return "左右清单合价差异超阈值"; }
    @Override protected String description() { return "左右批次已匹配清单的Excel原始合价差异率超过规则配置。"; }
    @Override protected int valueScale() { return 8; }
}
