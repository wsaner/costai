package com.ruoyi.cost.ai.prompt.service;

import java.util.List;
import com.ruoyi.cost.ai.prompt.domain.AiPromptTemplate;
import com.ruoyi.cost.ai.prompt.dto.AiPromptTemplateSaveRequest;

public interface AiPromptTemplateService
{
    List<AiPromptTemplate> selectList(AiPromptTemplate query);
    AiPromptTemplate selectById(Long id);
    AiPromptTemplate selectActive(String promptCode);
    Long create(AiPromptTemplateSaveRequest request, String operator);
    int update(AiPromptTemplateSaveRequest request, String operator);
    int delete(Long id, String operator);
}
