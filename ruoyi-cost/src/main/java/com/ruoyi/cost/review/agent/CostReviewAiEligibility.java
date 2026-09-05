package com.ruoyi.cost.review.agent;

import java.util.Set;
import com.ruoyi.cost.review.domain.CostReviewIssue;

/** 控制哪些规则候选值得消耗 Token 进行语义复核。 */
public final class CostReviewAiEligibility
{
    private static final Set<String> SEMANTIC_RULES = Set.of("QUANTITY_DIFF", "UNIT_PRICE_DIFF",
            "TOTAL_PRICE_DIFF", "DUPLICATE_ITEM", "ONLY_LEFT", "ONLY_RIGHT");
    private static final Set<String> SEMANTIC_TYPES = Set.of("FEATURE", "MISSING", "NEW_ITEM",
            "WRONG_ITEM", "OTHER");

    private CostReviewAiEligibility() {}

    public static Decision evaluate(CostReviewIssue issue)
    {
        if (issue == null) return new Decision(false, "审核问题不存在");
        if ((issue.getRuleCode() != null && SEMANTIC_RULES.contains(issue.getRuleCode()))
                || (issue.getIssueType() != null && SEMANTIC_TYPES.contains(issue.getIssueType())))
            return new Decision(true, null);
        return new Decision(false, "该问题属于确定性数据校验，无需消耗AI Token复核");
    }

    public record Decision(boolean eligible, String reason) {}
}
