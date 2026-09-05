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
public class NegativeQuantityRule implements ReviewRule
{
    private final ReviewIssueFactory factory;
    public NegativeQuantityRule(ReviewIssueFactory factory) { this.factory = factory; }
    @Override public String getCode() { return ReviewRuleCodes.NEGATIVE_QUANTITY; }
    @Override public boolean supports(ReviewContext context) { return context.settings().enabled(getCode()); }

    @Override
    public List<CostReviewIssue> execute(ReviewContext context)
    {
        List<CostReviewIssue> issues = new ArrayList<>();
        for (ItemRuleSupport.SidedItem sided : ItemRuleSupport.all(context))
        {
            CostBoqItem item = sided.item();
            if (item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) < 0)
            {
                issues.add(factory.item(context, item, sided.side(), getCode(), ReviewIssueLevel.HIGH,
                        "工程量为负数", "清单工程量小于0，请核查符号、扣减项表达方式及原始数据。",
                        item.getQuantity().toPlainString(), ">= 0", item.getQuantity(), null,
                        item.getTotalPrice(), factory.evidence("batchId", item.getBatchId(),
                                "sourceSheet", item.getSourceSheet(), "sourceRow", item.getSourceRow(),
                                "quantity", item.getQuantity())));
            }
        }
        return issues;
    }
}
