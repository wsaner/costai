package com.ruoyi.cost.ai.model.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.ai.model.domain.AiModelConfig;
import com.ruoyi.cost.ai.model.dto.AiModelConfigSaveRequest;
import com.ruoyi.cost.ai.model.mapper.AiModelConfigMapper;
import com.ruoyi.cost.ai.model.protocol.AiModelCredential;
import com.ruoyi.cost.ai.model.security.AiSecretCipher;
import com.ruoyi.cost.ai.model.service.AiModelConfigService;
import com.ruoyi.cost.ai.model.support.AiModelConfigNormalizer;

@Service
public class AiModelConfigServiceImpl implements AiModelConfigService
{
    private final AiModelConfigMapper mapper;
    private final AiSecretCipher cipher;

    public AiModelConfigServiceImpl(AiModelConfigMapper mapper, AiSecretCipher cipher)
    {
        this.mapper = mapper;
        this.cipher = cipher;
    }

    @Override
    public List<AiModelConfig> selectList(AiModelConfig query)
    {
        return mapper.selectModelConfigList(query == null ? new AiModelConfig() : query);
    }

    @Override
    public AiModelConfig selectById(Long id)
    {
        AiModelConfig config = mapper.selectModelConfigById(id);
        if (config == null) throw new ServiceException("AI 模型配置不存在");
        return config;
    }

    @Override
    @Transactional
    public Long create(AiModelConfigSaveRequest request, String operator)
    {
        AiModelConfig config = toConfig(request, null, operator, true);
        ensureUniqueName(config.getName(), null);
        if ("Y".equals(config.getIsDefault())) mapper.clearDefault(null, operator);
        else if ("0".equals(config.getEnabled()) && mapper.selectDefaultEnabledConfig() == null)
            config.setIsDefault("Y");
        if (mapper.insertModelConfig(config) != 1) throw new ServiceException("新增 AI 模型配置失败");
        return config.getId();
    }

    @Override
    @Transactional
    public int update(AiModelConfigSaveRequest request, String operator)
    {
        if (request.getId() == null) throw new ServiceException("模型配置ID不能为空");
        AiModelConfig existing = selectById(request.getId());
        AiModelConfig config = toConfig(request, existing, operator, false);
        ensureUniqueName(config.getName(), config.getId());
        if ("Y".equals(config.getIsDefault())) mapper.clearDefault(config.getId(), operator);
        return mapper.updateModelConfig(config);
    }

    @Override
    @Transactional
    public int delete(Long id, String operator)
    {
        selectById(id);
        return mapper.deleteModelConfig(id, operator);
    }

    @Override
    public AiModelCredential resolveCredential(Long id, boolean requireEnabled)
    {
        AiModelConfig config = id == null ? mapper.selectDefaultEnabledConfig() : mapper.selectModelConfigById(id);
        if (config == null) throw new ServiceException(id == null ? "未配置已启用的默认 AI 模型" : "AI 模型配置不存在");
        if (requireEnabled && !"0".equals(config.getEnabled())) throw new ServiceException("AI 模型配置已停用");
        return new AiModelCredential(config, cipher.decrypt(config.getApiKeyEncrypted()));
    }

    private AiModelConfig toConfig(AiModelConfigSaveRequest request, AiModelConfig existing,
            String operator, boolean creating)
    {
        AiModelConfig config = new AiModelConfig();
        config.setId(request.getId());
        config.setName(AiModelConfigNormalizer.trim(request.getName()));
        config.setProviderType(AiModelConfigNormalizer.normalizeProvider(request.getProviderType()));
        config.setBaseUrl(AiModelConfigNormalizer.normalizeBaseUrl(request.getBaseUrl()));
        config.setChatModel(AiModelConfigNormalizer.trim(request.getChatModel()));
        String embedding = AiModelConfigNormalizer.trim(request.getEmbeddingModel());
        config.setEmbeddingModel(embedding.isEmpty() ? null : embedding);
        config.setTemperature(request.getTemperature() == null ? new BigDecimal("0.200") : request.getTemperature());
        config.setMaxTokens(request.getMaxTokens() == null ? 4096 : request.getMaxTokens());
        config.setTimeoutSeconds(request.getTimeoutSeconds() == null ? 60 : request.getTimeoutSeconds());
        config.setEnabled(StringUtils.isEmpty(request.getEnabled()) ? "1" : request.getEnabled());
        config.setIsDefault(StringUtils.isEmpty(request.getIsDefault()) ? "N" : request.getIsDefault());
        if ("Y".equals(config.getIsDefault()) && !"0".equals(config.getEnabled()))
            throw new ServiceException("默认模型必须处于启用状态");
        String newKey = request.getApiKey();
        if (Boolean.TRUE.equals(request.getClearApiKey()))
        {
            config.setApiKeyEncrypted(null);
            config.setApiKeyHint(null);
        }
        else if (StringUtils.isNotEmpty(newKey))
        {
            String trimmed = newKey.trim();
            config.setApiKeyEncrypted(cipher.encrypt(trimmed));
            config.setApiKeyHint(AiModelConfigNormalizer.keyHint(trimmed));
        }
        else if (!creating && existing != null)
        {
            config.setApiKeyEncrypted(existing.getApiKeyEncrypted());
            config.setApiKeyHint(existing.getApiKeyHint());
        }
        config.setRemark(request.getRemark());
        config.setCreateBy(operator);
        config.setUpdateBy(operator);
        return config;
    }

    private void ensureUniqueName(String name, Long excludeId)
    {
        if (mapper.countByName(name, excludeId) > 0) throw new ServiceException("配置名称已存在");
    }
}
