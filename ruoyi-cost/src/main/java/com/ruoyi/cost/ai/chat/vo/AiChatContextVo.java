package com.ruoyi.cost.ai.chat.vo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 发送给模型的有限项目上下文，同时供前端展示工具与引用。 */
public class AiChatContextVo
{
    private final Map<String, Object> data = new LinkedHashMap<>();
    private final List<Map<String, Object>> toolCalls = new ArrayList<>();
    private final List<Map<String, Object>> sources = new ArrayList<>();

    public Map<String, Object> getData() { return data; }
    public List<Map<String, Object>> getToolCalls() { return toolCalls; }
    public List<Map<String, Object>> getSources() { return sources; }
}
