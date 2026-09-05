package com.ruoyi.cost.review.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cost.review.domain.CostReviewRuleConfig;

public interface CostReviewRuleConfigMapper
{
    List<CostReviewRuleConfig> selectConfigList();
    CostReviewRuleConfig selectConfigById(@Param("id") Long id);
    int updateConfigValue(@Param("id") Long id, @Param("configValue") String configValue,
            @Param("updateBy") String updateBy);
}
