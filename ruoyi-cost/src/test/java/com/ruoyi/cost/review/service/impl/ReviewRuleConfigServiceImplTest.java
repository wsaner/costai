package com.ruoyi.cost.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.review.domain.CostReviewRuleConfig;
import com.ruoyi.cost.review.mapper.CostReviewRuleConfigMapper;

@ExtendWith(MockitoExtension.class)
class ReviewRuleConfigServiceImplTest
{
    @Mock CostReviewRuleConfigMapper mapper;

    @Test
    void normalizesValidDecimalAndUpdates()
    {
        CostReviewRuleConfig warning = config(1L, "QUANTITY_DIFF", "warningRate", "DECIMAL", "0.10");
        CostReviewRuleConfig high = config(2L, "QUANTITY_DIFF", "highRate", "DECIMAL", "0.30");
        when(mapper.selectConfigById(1L)).thenReturn(warning);
        when(mapper.selectConfigList()).thenReturn(List.of(warning, high));
        when(mapper.updateConfigValue(1L, "0.2", "admin")).thenReturn(1);

        assertEquals(1, new ReviewRuleConfigServiceImpl(mapper).updateConfig(1L, "0.2000", "admin"));
        verify(mapper).updateConfigValue(1L, "0.2", "admin");
    }

    @Test
    void rejectsHighThresholdBelowWarningAndInvalidBoolean()
    {
        CostReviewRuleConfig warning = config(1L, "UNIT_PRICE_DIFF", "warningRate", "DECIMAL", "0.10");
        CostReviewRuleConfig high = config(2L, "UNIT_PRICE_DIFF", "highRate", "DECIMAL", "0.20");
        when(mapper.selectConfigById(2L)).thenReturn(high);
        when(mapper.selectConfigList()).thenReturn(List.of(warning, high));
        ReviewRuleConfigServiceImpl service = new ReviewRuleConfigServiceImpl(mapper);
        assertThrows(ServiceException.class, () -> service.updateConfig(2L, "0.05", "admin"));

        CostReviewRuleConfig enabled = config(3L, "ONLY_LEFT", "enabled", "BOOLEAN", "true");
        when(mapper.selectConfigById(3L)).thenReturn(enabled);
        assertThrows(ServiceException.class, () -> service.updateConfig(3L, "yes", "admin"));
    }

    @Test
    void rejectsNegativeAndRateAboveOne()
    {
        CostReviewRuleConfig tolerance = config(1L, "TOTAL_CALC_ERROR",
                "absoluteTolerance", "DECIMAL", "0.05");
        when(mapper.selectConfigById(1L)).thenReturn(tolerance);
        ReviewRuleConfigServiceImpl service = new ReviewRuleConfigServiceImpl(mapper);
        assertThrows(ServiceException.class, () -> service.updateConfig(1L, "-0.01", "admin"));

        CostReviewRuleConfig rate = config(2L, "QUANTITY_DIFF", "warningRate", "DECIMAL", "0.1");
        when(mapper.selectConfigById(2L)).thenReturn(rate);
        assertThrows(ServiceException.class, () -> service.updateConfig(2L, "1.1", "admin"));
    }

    private CostReviewRuleConfig config(Long id, String rule, String key, String type, String value)
    {
        CostReviewRuleConfig config = new CostReviewRuleConfig();
        config.setId(id);
        config.setRuleCode(rule);
        config.setConfigKey(key);
        config.setValueType(type);
        config.setConfigValue(value);
        return config;
    }
}
