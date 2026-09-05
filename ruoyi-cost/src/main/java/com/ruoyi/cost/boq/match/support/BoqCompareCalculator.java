package com.ruoyi.cost.boq.match.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import org.springframework.stereotype.Component;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;

/** 生成对比行并以BigDecimal计算左减右差异。 */
@Component
public class BoqCompareCalculator
{
    public CostBoqCompare create(Long projectId, Long leftBatchId, Long rightBatchId,
            CostBoqItem left, CostBoqItem right, BoqMatchType type, double score, String operator)
    {
        CostBoqCompare result = new CostBoqCompare();
        result.setProjectId(projectId);
        result.setLeftBatchId(leftBatchId);
        result.setRightBatchId(rightBatchId);
        result.setLeftItemId(left == null ? null : left.getId());
        result.setRightItemId(right == null ? null : right.getId());
        result.setMatchType(type.name());
        result.setMatchScore(BigDecimal.valueOf(score).setScale(6, RoundingMode.HALF_UP));
        if (left != null && right != null)
        {
            result.setQuantityDiff(difference(left.getQuantity(), right.getQuantity(), 8));
            result.setQuantityDiffRate(rate(result.getQuantityDiff(), left.getQuantity()));
            result.setUnitPriceDiff(difference(left.getUnitPrice(), right.getUnitPrice(), 8));
            result.setUnitPriceDiffRate(rate(result.getUnitPriceDiff(), left.getUnitPrice()));
            result.setTotalPriceDiff(difference(left.getTotalPrice(), right.getTotalPrice(), 6));
            result.setTotalPriceDiffRate(rate(result.getTotalPriceDiff(), left.getTotalPrice()));
        }
        result.setCreateBy(operator);
        result.setCreateTime(new Date());
        return result;
    }

    BigDecimal difference(BigDecimal left, BigDecimal right, int scale)
    {
        if (left == null || right == null) return null;
        return left.subtract(right).setScale(scale, RoundingMode.HALF_UP);
    }

    BigDecimal rate(BigDecimal difference, BigDecimal left)
    {
        if (difference == null || left == null || left.compareTo(BigDecimal.ZERO) == 0) return null;
        return difference.divide(left.abs(), 6, RoundingMode.HALF_UP);
    }
}
