package com.ruoyi.cost.review.service;

import java.util.List;
import com.ruoyi.cost.review.domain.CostReviewRuleConfig;
import com.ruoyi.cost.review.rule.ReviewRuleSettings;

public interface ReviewRuleConfigService
{
    List<CostReviewRuleConfig> selectConfigList();
    ReviewRuleSettings loadSettings();
    int updateConfig(Long id, String configValue, String operator);
}
