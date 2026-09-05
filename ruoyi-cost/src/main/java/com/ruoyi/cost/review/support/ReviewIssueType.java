package com.ruoyi.cost.review.support;

import java.util.Map;

/** 面向业务展示的问题大类，详细规则编码保留在 ruleCode。 */
public final class ReviewIssueType
{
    public static final String QUANTITY = "QUANTITY";
    public static final String UNIT_PRICE = "UNIT_PRICE";
    public static final String TOTAL_PRICE = "TOTAL_PRICE";
    public static final String DUPLICATE = "DUPLICATE";
    public static final String MISSING = "MISSING";
    public static final String NEW_ITEM = "NEW_ITEM";
    public static final String FEATURE = "FEATURE";
    public static final String DATA = "DATA";
    public static final String OTHER = "OTHER";

    private static final Map<String, String> RULE_TYPES = Map.of(
            ReviewRuleCodes.NEGATIVE_QUANTITY, QUANTITY,
            ReviewRuleCodes.QUANTITY_DIFF, QUANTITY,
            ReviewRuleCodes.NEGATIVE_UNIT_PRICE, UNIT_PRICE,
            ReviewRuleCodes.ZERO_UNIT_PRICE, UNIT_PRICE,
            ReviewRuleCodes.UNIT_PRICE_DIFF, UNIT_PRICE,
            ReviewRuleCodes.TOTAL_CALC_ERROR, TOTAL_PRICE,
            ReviewRuleCodes.TOTAL_PRICE_DIFF, TOTAL_PRICE,
            ReviewRuleCodes.DUPLICATE_ITEM, DUPLICATE,
            ReviewRuleCodes.ONLY_LEFT, MISSING,
            ReviewRuleCodes.ONLY_RIGHT, NEW_ITEM);

    private ReviewIssueType() {}
    public static String fromRule(String ruleCode) { return RULE_TYPES.getOrDefault(ruleCode, OTHER); }
}
