package com.ruoyi.cost.review.rule.impl;

import java.math.BigDecimal;
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
public class NegativeUnitPriceRule implements ReviewRule
{
    private final ReviewIssueFactory factory;
    public NegativeUnitPriceRule(ReviewIssueFactory factory) { this.factory = factory; }
    @Override public String getCode() { return ReviewRuleCodes.NEGATIVE_UNIT_PRICE; }
    @Override public boolean supports(ReviewContext context) { return context.settings().enabled(getCode()); }

    @Override
    public List<CostReviewIssue> execute(ReviewContext context)
    {
        List<CostReviewIssue> issues = new ArrayList<>();
        for (ItemRuleSupport.SidedItem sided : ItemRuleSupport.all(context))
        {
            CostBoqItem item = sided.item();
            if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0)
            {
                issues.add(factory.item(context, item, sided.side(), getCode(), ReviewIssueLevel.HIGH,
                        "综合单价为负数", "清单综合单价小于0，请核查费用方向和原始报价。",
                        item.getUnitPrice().toPlainString(), ">= 0", item.getUnitPrice(), null,
                        item.getTotalPrice(), factory.evidence("batchId", item.getBatchId(),
                                "sourceSheet", item.getSourceSheet(), "sourceRow", item.getSourceRow(),
                                "unitPrice", item.getUnitPrice())));
            }
        }
        return issues;
    }
}
