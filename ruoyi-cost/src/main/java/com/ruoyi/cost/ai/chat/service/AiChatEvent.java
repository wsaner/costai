package com.ruoyi.cost.ai.chat.service;

/** 与Web层解耦的流式事件。 */
public record AiChatEvent(String type, Object data) { }
