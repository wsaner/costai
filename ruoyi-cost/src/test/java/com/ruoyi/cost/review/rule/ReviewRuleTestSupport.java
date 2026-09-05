package com.ruoyi.cost.review.rule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.review.domain.CostReviewRuleConfig;
import com.ruoyi.cost.review.support.ReviewRuleCodes;

public final class ReviewRuleTestSupport
{
    private ReviewRuleTestSupport() {}

    public static ReviewIssueFactory factory()
    {
        return new ReviewIssueFactory(new ObjectMapper());
    }

    public static ReviewContext context(List<CostBoqItem> left, List<CostBoqItem> right,
            List<CostBoqCompare> compares)
    {
        List<CostReviewRuleConfig> configs = new ArrayList<>();
        for (String code : List.of(ReviewRuleCodes.NEGATIVE_QUANTITY,
                ReviewRuleCodes.NEGATIVE_UNIT_PRICE, ReviewRuleCodes.ZERO_UNIT_PRICE,
                ReviewRuleCodes.TOTAL_CALC_ERROR, ReviewRuleCodes.DUPLICATE_ITEM,
                ReviewRuleCodes.QUANTITY_DIFF, ReviewRuleCodes.UNIT_PRICE_DIFF,
                ReviewRuleCodes.TOTAL_PRICE_DIFF, ReviewRuleCodes.ONLY_LEFT,
                ReviewRuleCodes.ONLY_RIGHT))
        {
            configs.add(config(code, "enabled", "true"));
        }
        configs.add(config(ReviewRuleCodes.ZERO_UNIT_PRICE, "exemptKeywords", "暂估价,甲供"));
        configs.add(config(ReviewRuleCodes.TOTAL_CALC_ERROR, "absoluteTolerance", "0.05"));
        configs.add(config(ReviewRuleCodes.TOTAL_CALC_ERROR, "relativeTolerance", "0.001"));
        configs.add(config(ReviewRuleCodes.TOTAL_CALC_ERROR, "highRate", "0.05"));
        addDifference(configs, ReviewRuleCodes.QUANTITY_DIFF, "0.10", "0.30");
        addDifference(configs, ReviewRuleCodes.UNIT_PRICE_DIFF, "0.10", "0.20");
        addDifference(configs, ReviewRuleCodes.TOTAL_PRICE_DIFF, "0.10", "0.20");
        return new ReviewContext(1L, 10L, 20L, left, right, compares,
                ReviewRuleSettings.from(configs));
    }

    public static CostBoqItem item(long id, long batchId, String code, String name,
            String feature, String unit, String quantity, String unitPrice, String totalPrice)
    {
        CostBoqItem item = new CostBoqItem();
        item.setId(id);
        item.setBatchId(batchId);
        item.setItemCode(code);
        item.setItemName(name);
        item.setItemFeature(feature);
        item.setUnit(unit);
        item.setQuantity(decimal(quantity));
        item.setUnitPrice(decimal(unitPrice));
        item.setTotalPrice(decimal(totalPrice));
        return item;
    }

    public static CostBoqCompare compare(long id, String type, String leftQuantity,
            String rightQuantity, String leftPrice, String rightPrice,
            String leftTotal, String rightTotal)
    {
        CostBoqCompare row = new CostBoqCompare();
        row.setId(id);
        row.setMatchType(type);
        row.setLeftItemId(leftQuantity == null ? null : id * 10);
        row.setRightItemId(rightQuantity == null ? null : id * 10 + 1);
        row.setLeftItemCode("L" + id);
        row.setRightItemCode("R" + id);
        row.setLeftItemName("左清单" + id);
        row.setRightItemName("右清单" + id);
        row.setLeftQuantity(decimal(leftQuantity));
        row.setRightQuantity(decimal(rightQuantity));
        row.setLeftUnitPrice(decimal(leftPrice));
        row.setRightUnitPrice(decimal(rightPrice));
        row.setLeftTotalPrice(decimal(leftTotal));
        row.setRightTotalPrice(decimal(rightTotal));
        return row;
    }

    private static void addDifference(List<CostReviewRuleConfig> configs, String code,
            String warning, String high)
    {
        configs.add(config(code, "warningRate", warning));
        configs.add(config(code, "highRate", high));
    }

    private static CostReviewRuleConfig config(String code, String key, String value)
    {
        CostReviewRuleConfig config = new CostReviewRuleConfig();
        config.setRuleCode(code);
        config.setConfigKey(key);
        config.setConfigValue(value);
        return config;
    }

    private static BigDecimal decimal(String value)
    {
        return value == null ? null : new BigDecimal(value);
    }
}
