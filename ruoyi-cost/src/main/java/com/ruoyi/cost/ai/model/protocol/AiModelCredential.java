package com.ruoyi.cost.ai.model.protocol;

import com.ruoyi.cost.ai.model.domain.AiModelConfig;

/** 仅在内存短暂存在的模型配置和解密密钥；不得序列化或记录日志。 */
public record AiModelCredential(AiModelConfig config, String apiKey)
{
}
