package com.ruoyi.cost.review.rule.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.match.support.BoqTextNormalizer;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.rule.ReviewRuleTestSupport;

class DuplicateItemRuleTest
{
    @Test
    void detectsNormalizedDuplicateOnlyWithinEachSideAndPreservesModelDifferences()
    {
        CostBoqItem first = ReviewRuleTestSupport.item(1, 10, "0101 01", "C30 混凝土",
                "泵送（P6）", "平方米", "1", "1", "1");
        CostBoqItem duplicate = ReviewRuleTestSupport.item(2, 10, "０１０１ ０１", "c30混凝土",
                "泵送 P6", "m2", "2", "1", "2");
        CostBoqItem differentModel = ReviewRuleTestSupport.item(3, 10, "0101 01", "C30混凝土",
                "泵送P8", "m2", "1", "1", "1");
        CostBoqItem otherSide = ReviewRuleTestSupport.item(4, 20, "0101 01", "C30混凝土",
                "泵送P6", "m2", "1", "1", "1");

        List<CostReviewIssue> issues = new DuplicateItemRule(ReviewRuleTestSupport.factory(),
                new BoqTextNormalizer()).execute(ReviewRuleTestSupport.context(
                        List.of(first, duplicate, differentModel), List.of(otherSide), List.of()));

        assertEquals(1, issues.size());
        assertEquals(2L, issues.get(0).getBoqItemId());
        assertEquals("LEFT", issues.get(0).getItemSide());
    }
}
