package com.ruoyi.cost.ai.model.service;

import java.util.List;
import com.ruoyi.cost.ai.model.domain.AiModelConfig;
import com.ruoyi.cost.ai.model.dto.AiModelConfigSaveRequest;
import com.ruoyi.cost.ai.model.protocol.AiModelCredential;

public interface AiModelConfigService
{
    List<AiModelConfig> selectList(AiModelConfig query);
    AiModelConfig selectById(Long id);
    Long create(AiModelConfigSaveRequest request, String operator);
    int update(AiModelConfigSaveRequest request, String operator);
    int delete(Long id, String operator);
    AiModelCredential resolveCredential(Long id, boolean requireEnabled);
}
