package com.ruoyi.cost.review.rule.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.rule.ReviewContext;
import com.ruoyi.cost.review.rule.ReviewIssueFactory;
import com.ruoyi.cost.review.rule.ReviewRule;
import com.ruoyi.cost.review.support.ReviewIssueLevel;
import com.ruoyi.cost.review.support.ReviewRuleCodes;

@Component
public class ZeroUnitPriceRule implements ReviewRule
{
    private final ReviewIssueFactory factory;
    public ZeroUnitPriceRule(ReviewIssueFactory factory) { this.factory = factory; }
    @Override public String getCode() { return ReviewRuleCodes.ZERO_UNIT_PRICE; }
    @Override public boolean supports(ReviewContext context) { return context.settings().enabled(getCode()); }

    @Override
    public List<CostReviewIssue> execute(ReviewContext context)
    {
        List<String> exemptions = context.settings().stringList(getCode(), "exemptKeywords");
        List<CostReviewIssue> issues = new ArrayList<>();
        for (ItemRuleSupport.SidedItem sided : ItemRuleSupport.all(context))
        {
            CostBoqItem item = sided.item();
            if (item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) > 0
                    && item.getUnitPrice() != null && item.getUnitPrice().compareTo(BigDecimal.ZERO) == 0
                    && !isExempt(item, exemptions))
            {
                issues.add(factory.item(context, item, sided.side(), getCode(), ReviewIssueLevel.MEDIUM,
                        "有工程量但综合单价为0", "该清单不属于配置的特殊清单，但存在正工程量和零综合单价。",
                        "quantity=" + item.getQuantity().toPlainString() + ", unitPrice=0",
                        "unitPrice > 0 或命中豁免关键词", BigDecimal.ZERO, null,
                        item.getTotalPrice(), factory.evidence("batchId", item.getBatchId(),
                                "quantity", item.getQuantity(), "unitPrice", item.getUnitPrice(),
                                "exemptKeywords", exemptions)));
            }
        }
        return issues;
    }

    private boolean isExempt(CostBoqItem item, List<String> exemptions)
    {
        String text = (StringUtils.defaultString(item.getItemName()) + " "
                + StringUtils.defaultString(item.getItemFeature())).toLowerCase(Locale.ROOT);
        return exemptions.stream().map(value -> value.toLowerCase(Locale.ROOT)).anyMatch(text::contains);
    }
}
