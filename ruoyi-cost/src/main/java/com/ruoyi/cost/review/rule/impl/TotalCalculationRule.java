package com.ruoyi.cost.review.rule.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.rule.ReviewContext;
import com.ruoyi.cost.review.rule.ReviewIssueFactory;
import com.ruoyi.cost.review.rule.ReviewRule;
import com.ruoyi.cost.review.support.ReviewIssueLevel;
import com.ruoyi.cost.review.support.ReviewRuleCodes;

@Component
public class TotalCalculationRule implements ReviewRule
{
    private final ReviewIssueFactory factory;
    public TotalCalculationRule(ReviewIssueFactory factory) { this.factory = factory; }
    @Override public String getCode() { return ReviewRuleCodes.TOTAL_CALC_ERROR; }
    @Override public boolean supports(ReviewContext context) { return context.settings().enabled(getCode()); }

    @Override
    public List<CostReviewIssue> execute(ReviewContext context)
    {
        BigDecimal absoluteTolerance = context.settings().decimal(getCode(), "absoluteTolerance");
        BigDecimal relativeTolerance = context.settings().decimal(getCode(), "relativeTolerance");
        BigDecimal highRate = context.settings().decimal(getCode(), "highRate");
        List<CostReviewIssue> issues = new ArrayList<>();
        for (ItemRuleSupport.SidedItem sided : ItemRuleSupport.all(context))
        {
            CostBoqItem item = sided.item();
            if (item.getQuantity() == null || item.getUnitPrice() == null || item.getTotalPrice() == null) continue;
            BigDecimal calculated = item.getQuantity().multiply(item.getUnitPrice())
                    .setScale(6, RoundingMode.HALF_UP);
            BigDecimal difference = item.getTotalPrice().subtract(calculated)
                    .setScale(8, RoundingMode.HALF_UP);
            BigDecimal rate = calculated.compareTo(BigDecimal.ZERO) == 0 ? null
                    : difference.divide(calculated.abs(), 6, RoundingMode.HALF_UP);
            if (difference.abs().compareTo(absoluteTolerance) <= 0
                    || rate != null && rate.abs().compareTo(relativeTolerance) <= 0) continue;
            ReviewIssueLevel level = rate == null || rate.abs().compareTo(highRate) >= 0
                    ? ReviewIssueLevel.HIGH : ReviewIssueLevel.MEDIUM;
            issues.add(factory.item(context, item, sided.side(), getCode(), level,
                    "Excel合价与计算值不一致", "工程量×综合单价与Excel原始合价的偏差超过配置容差。",
                    item.getTotalPrice().toPlainString(), calculated.toPlainString(), difference, rate,
                    difference, factory.evidence("batchId", item.getBatchId(),
                            "quantity", item.getQuantity(), "unitPrice", item.getUnitPrice(),
                            "excelTotalPrice", item.getTotalPrice(), "calculatedTotalPrice", calculated,
                            "absoluteTolerance", absoluteTolerance,
                            "relativeTolerance", relativeTolerance)));
        }
        return issues;
    }
}
