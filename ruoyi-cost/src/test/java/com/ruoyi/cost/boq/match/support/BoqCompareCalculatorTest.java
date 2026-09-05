package com.ruoyi.cost.boq.match.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;

class BoqCompareCalculatorTest
{
    private final BoqCompareCalculator calculator = new BoqCompareCalculator();

    @Test
    void calculatesBigDecimalDifferencesAndRatesUsingAbsoluteLeftValue()
    {
        CostBoqCompare result = calculator.create(1L, 2L, 3L,
                item(11L, "1200", "86.125", "103350"),
                item(12L, "850", "71", "60350"),
                BoqMatchType.EXACT, 1D, "admin");

        assertEquals(new BigDecimal("350.00000000"), result.getQuantityDiff());
        assertEquals(new BigDecimal("0.291667"), result.getQuantityDiffRate());
        assertEquals(new BigDecimal("15.12500000"), result.getUnitPriceDiff());
        assertEquals(new BigDecimal("0.175617"), result.getUnitPriceDiffRate());
        assertEquals(new BigDecimal("43000.000000"), result.getTotalPriceDiff());
    }

    @Test
    void handlesNullZeroAndNegativeMoneyWithoutFloatingPointCalculation()
    {
        assertNull(calculator.difference(null, BigDecimal.ONE, 8));
        assertNull(calculator.rate(BigDecimal.ONE, BigDecimal.ZERO));
        assertEquals(new BigDecimal("-0.500000"),
                calculator.rate(new BigDecimal("-5"), new BigDecimal("-10")));
        assertEquals(new BigDecimal("999999999999999999.123457"),
                calculator.difference(new BigDecimal("1000000000000000000.1234567"),
                        BigDecimal.ONE, 6));
    }

    private CostBoqItem item(Long id, String quantity, String unitPrice, String totalPrice)
    {
        CostBoqItem item = new CostBoqItem();
        item.setId(id);
        item.setQuantity(new BigDecimal(quantity));
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setTotalPrice(new BigDecimal(totalPrice));
        return item;
    }
}
