package com.ruoyi.cost.review.rule.impl;

import java.util.ArrayList;
import java.util.List;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.review.rule.ReviewContext;

final class ItemRuleSupport
{
    private ItemRuleSupport() {}

    static List<SidedItem> all(ReviewContext context)
    {
        List<SidedItem> result = new ArrayList<>(context.leftItems().size() + context.rightItems().size());
        context.leftItems().forEach(item -> result.add(new SidedItem("LEFT", item)));
        context.rightItems().forEach(item -> result.add(new SidedItem("RIGHT", item)));
        return result;
    }

    record SidedItem(String side, CostBoqItem item) {}
}
