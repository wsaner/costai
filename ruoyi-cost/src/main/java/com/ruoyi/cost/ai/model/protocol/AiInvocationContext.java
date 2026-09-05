package com.ruoyi.cost.ai.model.protocol;

/** 模型调用审计上下文，不包含 Prompt 正文。 */
public record AiInvocationContext(Long userId, String username, String businessType, String businessId)
{
    public static AiInvocationContext system(String businessType, String businessId)
    {
        return new AiInvocationContext(null, "system", businessType, businessId);
    }
}
