package com.ruoyi.cost.project.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CostProjectAmountCalculatorTest
{
    @Test
    void calculatesReductionRateWithSixDecimalPlaces()
    {
        assertEquals(new BigDecimal("0.411765"), CostProjectAmountCalculator.calculateReductionRate(
                new BigDecimal("850.00"), new BigDecimal("350.00")));
    }

    @Test
    void roundsHalfUpAndSupportsHugeAmounts()
    {
        assertEquals(new BigDecimal("0.333333"), CostProjectAmountCalculator.calculateReductionRate(
                new BigDecimal("999999999999999999.99"), new BigDecimal("333333333333333333.33")));
        assertEquals(new BigDecimal("12.35"), CostProjectAmountCalculator.amountOrZero(new BigDecimal("12.345")));
    }

    @Test
    void returnsZeroForNullZeroOrNegativeDenominator()
    {
        assertEquals(new BigDecimal("0.000000"), CostProjectAmountCalculator.calculateReductionRate(null, BigDecimal.ONE));
        assertEquals(new BigDecimal("0.000000"), CostProjectAmountCalculator.calculateReductionRate(BigDecimal.ZERO, BigDecimal.ONE));
        assertEquals(new BigDecimal("0.000000"), CostProjectAmountCalculator.calculateReductionRate(new BigDecimal("-1"), BigDecimal.ONE));
        assertEquals(new BigDecimal("0.00"), CostProjectAmountCalculator.amountOrZero(null));
    }
}
