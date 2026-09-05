package com.ruoyi.cost.ai.model.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cost.ai.model.domain.AiModelConfig;

public interface AiModelConfigMapper
{
    List<AiModelConfig> selectModelConfigList(AiModelConfig query);
    AiModelConfig selectModelConfigById(@Param("id") Long id);
    AiModelConfig selectDefaultEnabledConfig();
    int countByName(@Param("name") String name, @Param("excludeId") Long excludeId);
    int insertModelConfig(AiModelConfig config);
    int updateModelConfig(AiModelConfig config);
    int clearDefault(@Param("excludeId") Long excludeId, @Param("updateBy") String updateBy);
    int deleteModelConfig(@Param("id") Long id, @Param("updateBy") String updateBy);
}
