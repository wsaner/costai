package com.ruoyi.cost.ai.prompt.service.impl;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.ai.prompt.domain.AiPromptTemplate;
import com.ruoyi.cost.ai.prompt.dto.AiPromptTemplateSaveRequest;
import com.ruoyi.cost.ai.prompt.mapper.AiPromptTemplateMapper;
import com.ruoyi.cost.ai.prompt.service.AiPromptTemplateService;

@Service
public class AiPromptTemplateServiceImpl implements AiPromptTemplateService
{
    private final AiPromptTemplateMapper mapper;

    public AiPromptTemplateServiceImpl(AiPromptTemplateMapper mapper) { this.mapper = mapper; }

    @Override
    public List<AiPromptTemplate> selectList(AiPromptTemplate query)
    {
        return mapper.selectPromptList(query == null ? new AiPromptTemplate() : query);
    }

    @Override
    public AiPromptTemplate selectById(Long id)
    {
        AiPromptTemplate prompt = mapper.selectPromptById(id);
        if (prompt == null) throw new ServiceException("Prompt 模板不存在");
        return prompt;
    }

    @Override
    public AiPromptTemplate selectActive(String promptCode)
    {
        AiPromptTemplate prompt = mapper.selectActivePrompt(normalizeCode(promptCode));
        if (prompt == null) throw new ServiceException("未找到已启用的 Prompt 模板");
        return prompt;
    }

    @Override
    @Transactional
    public Long create(AiPromptTemplateSaveRequest request, String operator)
    {
        AiPromptTemplate prompt = toPrompt(request, operator);
        ensureUnique(prompt, null);
        if ("0".equals(prompt.getEnabled())) mapper.disableOtherVersions(prompt.getPromptCode(), null, operator);
        if (mapper.insertPrompt(prompt) != 1) throw new ServiceException("新增 Prompt 模板失败");
        return prompt.getId();
    }

    @Override
    @Transactional
    public int update(AiPromptTemplateSaveRequest request, String operator)
    {
        if (request.getId() == null) throw new ServiceException("Prompt模板ID不能为空");
        selectById(request.getId());
        AiPromptTemplate prompt = toPrompt(request, operator);
        ensureUnique(prompt, prompt.getId());
        if ("0".equals(prompt.getEnabled()))
            mapper.disableOtherVersions(prompt.getPromptCode(), prompt.getId(), operator);
        return mapper.updatePrompt(prompt);
    }

    @Override
    @Transactional
    public int delete(Long id, String operator)
    {
        selectById(id);
        return mapper.deletePrompt(id, operator);
    }

    private AiPromptTemplate toPrompt(AiPromptTemplateSaveRequest request, String operator)
    {
        AiPromptTemplate prompt = new AiPromptTemplate();
        prompt.setId(request.getId());
        prompt.setPromptCode(normalizeCode(request.getPromptCode()));
        prompt.setPromptName(request.getPromptName().trim());
        prompt.setSystemPrompt(request.getSystemPrompt());
        prompt.setUserTemplate(request.getUserTemplate());
        prompt.setVersion(request.getVersion());
        prompt.setEnabled(StringUtils.isEmpty(request.getEnabled()) ? "1" : request.getEnabled());
        prompt.setRemark(request.getRemark());
        prompt.setCreateBy(operator);
        prompt.setUpdateBy(operator);
        return prompt;
    }

    private void ensureUnique(AiPromptTemplate prompt, Long excludeId)
    {
        if (mapper.countCodeVersion(prompt.getPromptCode(), prompt.getVersion(), excludeId) > 0)
            throw new ServiceException("Prompt 编码和版本号已存在");
    }

    private static String normalizeCode(String value)
    {
        if (value == null || value.isBlank()) throw new ServiceException("Prompt编码不能为空");
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
