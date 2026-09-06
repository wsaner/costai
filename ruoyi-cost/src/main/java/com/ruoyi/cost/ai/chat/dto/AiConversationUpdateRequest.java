package com.ruoyi.cost.ai.chat.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AiConversationUpdateRequest extends AiConversationCreateRequest
{
    @NotNull(message = "会话ID不能为空")
    private Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
