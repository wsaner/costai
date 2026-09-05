package com.ruoyi.cost.ai.model.service;

import java.util.function.Consumer;
import com.ruoyi.cost.ai.model.protocol.AiChatRequest;
import com.ruoyi.cost.ai.model.protocol.AiChatResponse;
import com.ruoyi.cost.ai.model.protocol.AiEmbeddingRequest;
import com.ruoyi.cost.ai.model.protocol.AiEmbeddingResponse;
import com.ruoyi.cost.ai.model.protocol.AiInvocationContext;
import com.ruoyi.cost.ai.model.protocol.AiStructuredRequest;
import com.ruoyi.cost.ai.model.protocol.AiStructuredResponse;

/**
 * 统一大模型能力入口。业务代码只依赖本接口，不直接调用具体厂商 API。
 */
public interface AiModelService
{
    AiChatResponse chat(AiChatRequest request);

    /**
     * 阻塞读取上游流并逐段回调。上层可将回调桥接至 SSE，但不得在数据库事务内调用。
     */
    AiChatResponse streamChat(AiChatRequest request, Consumer<String> deltaConsumer);

    AiStructuredResponse structuredChat(AiStructuredRequest request);

    AiEmbeddingResponse embedding(AiEmbeddingRequest request);

    /** 管理端连接测试，可在配置启用前调用。 */
    AiChatResponse testConnection(Long modelConfigId, AiInvocationContext context);
}
