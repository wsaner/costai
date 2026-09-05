package com.ruoyi.cost.ai.model.protocol;

/** 统一模型消息，仅允许调用方显式区分 system/user/assistant/tool。 */
public record AiMessage(String role, String content)
{
    public AiMessage
    {
        if (role == null || role.isBlank()) throw new IllegalArgumentException("消息角色不能为空");
        if (content == null) throw new IllegalArgumentException("消息内容不能为空");
    }
}
