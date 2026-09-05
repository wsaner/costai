package com.ruoyi.cost.review.rule.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.match.support.BoqTextNormalizer;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.rule.ReviewContext;
import com.ruoyi.cost.review.rule.ReviewIssueFactory;
import com.ruoyi.cost.review.rule.ReviewRule;
import com.ruoyi.cost.review.support.ReviewIssueLevel;
import com.ruoyi.cost.review.support.ReviewRuleCodes;

@Component
public class DuplicateItemRule implements ReviewRule
{
    private final ReviewIssueFactory factory;
    private final BoqTextNormalizer normalizer;
    public DuplicateItemRule(ReviewIssueFactory factory, BoqTextNormalizer normalizer)
    {
        this.factory = factory;
        this.normalizer = normalizer;
    }
    @Override public String getCode() { return ReviewRuleCodes.DUPLICATE_ITEM; }
    @Override public boolean supports(ReviewContext context) { return context.settings().enabled(getCode()); }

    @Override
    public List<CostReviewIssue> execute(ReviewContext context)
    {
        List<CostReviewIssue> issues = new ArrayList<>();
        find(context, context.leftItems(), "LEFT", issues);
        find(context, context.rightItems(), "RIGHT", issues);
        return issues;
    }

    private void find(ReviewContext context, List<CostBoqItem> items, String side,
            List<CostReviewIssue> issues)
    {
        Map<String, CostBoqItem> firstBySignature = new LinkedHashMap<>();
        for (CostBoqItem item : items)
        {
            String normalizedName = normalizer.normalizeText(item.getItemName());
            if (normalizedName.isEmpty()) continue;
            String signature = normalizer.normalizeCode(item.getItemCode()) + '\u0000'
                    + normalizedName + '\u0000' + normalizer.normalizeUnit(item.getUnit()) + '\u0000'
                    + normalizer.normalizeText(item.getItemFeature());
            CostBoqItem first = firstBySignature.putIfAbsent(signature, item);
            if (first != null)
            {
                issues.add(factory.item(context, item, side, getCode(), ReviewIssueLevel.MEDIUM,
                        "同批次存在高度一致清单", "编码、名称、单位和项目特征标准化后与另一清单一致。",
                        "itemId=" + item.getId(), "firstItemId=" + first.getId(), null, null,
                        item.getTotalPrice(), factory.evidence("batchId", item.getBatchId(),
                                "duplicateItemId", item.getId(), "firstItemId", first.getId(),
                                "normalizedSignature", signature)));
            }
        }
    }
}
