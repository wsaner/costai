package com.ruoyi.cost.ai.agent;

/**
 * AI Agent 的最小统一契约。Agent 只编排受控上下文与模型能力，不直接暴露厂商客户端。
 */
public interface AiAgent<C, R>
{
    R execute(C context);
}
