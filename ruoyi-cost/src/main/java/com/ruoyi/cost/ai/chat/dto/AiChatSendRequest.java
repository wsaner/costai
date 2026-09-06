package com.ruoyi.cost.ai.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiChatSendRequest
{
    @NotBlank(message = "消息不能为空")
    @Size(max = 4000, message = "单条消息不能超过4000个字符")
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
