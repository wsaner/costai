package com.ruoyi.cost.project.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 造价项目金额计算器，统一服务端计算规则。 */
public final class CostProjectAmountCalculator
{
    public static final int RATE_SCALE = 6;

    private CostProjectAmountCalculator()
    {
    }

    /**
     * 计算核减率。按小数存储，例如0.125000表示12.5%。
     */
    public static BigDecimal calculateReductionRate(BigDecimal submittedAmount, BigDecimal reductionAmount)
    {
        if (submittedAmount == null || reductionAmount == null || submittedAmount.signum() <= 0)
        {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        return reductionAmount.divide(submittedAmount, RATE_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal amountOrZero(BigDecimal amount)
    {
        return amount == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amount.setScale(2, RoundingMode.HALF_UP);
    }
}
