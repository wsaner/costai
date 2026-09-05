package com.ruoyi.cost.review.rule;

import java.util.List;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;

/** 单次规则审核的只读输入快照。 */
public record ReviewContext(Long projectId, Long leftBatchId, Long rightBatchId,
        List<CostBoqItem> leftItems, List<CostBoqItem> rightItems,
        List<CostBoqCompare> compareRows, ReviewRuleSettings settings)
{
    public ReviewContext
    {
        leftItems = List.copyOf(leftItems);
        rightItems = List.copyOf(rightItems);
        compareRows = List.copyOf(compareRows);
    }
}
