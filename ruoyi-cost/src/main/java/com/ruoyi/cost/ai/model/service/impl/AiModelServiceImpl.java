package com.ruoyi.cost.ai.model.service.impl;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.cost.ai.log.domain.AiRequestLog;
import com.ruoyi.cost.ai.log.service.AiRequestLogService;
import com.ruoyi.cost.ai.model.client.OpenAiCompatibleClient;
import com.ruoyi.cost.ai.model.protocol.AiChatRequest;
import com.ruoyi.cost.ai.model.protocol.AiChatResponse;
import com.ruoyi.cost.ai.model.protocol.AiEmbeddingRequest;
import com.ruoyi.cost.ai.model.protocol.AiEmbeddingResponse;
import com.ruoyi.cost.ai.model.protocol.AiInvocationContext;
import com.ruoyi.cost.ai.model.protocol.AiMessage;
import com.ruoyi.cost.ai.model.protocol.AiModelCredential;
import com.ruoyi.cost.ai.model.protocol.AiProviderException;
import com.ruoyi.cost.ai.model.protocol.AiStructuredRequest;
import com.ruoyi.cost.ai.model.protocol.AiStructuredResponse;
import com.ruoyi.cost.ai.model.protocol.AiTokenUsage;
import com.ruoyi.cost.ai.model.service.AiModelConfigService;
import com.ruoyi.cost.ai.model.service.AiModelService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiModelServiceImpl implements AiModelService
{
    private static final Set<String> ALLOWED_ROLES = Set.of("system", "user", "assistant", "tool");
    private static final int MAX_MESSAGES = 200;
    private static final int MAX_MESSAGE_CHARS = 200_000;
    private static final int MAX_EMBEDDING_INPUTS = 2_048;
    private static final int MAX_EMBEDDING_CHARS = 100_000;

    private final AiModelConfigService modelConfigService;
    private final OpenAiCompatibleClient client;
    private final AiRequestLogService requestLogService;
    private final ObjectMapper objectMapper;

    public AiModelServiceImpl(AiModelConfigService modelConfigService, OpenAiCompatibleClient client,
            AiRequestLogService requestLogService, ObjectMapper objectMapper)
    {
        this.modelConfigService = modelConfigService;
        this.client = client;
        this.requestLogService = requestLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiChatResponse chat(AiChatRequest request)
    {
        validateChatRequest(request);
        return executeChat(request, "CHAT", credential -> client.chat(credential, request));
    }

    @Override
    public AiChatResponse streamChat(AiChatRequest request, Consumer<String> deltaConsumer)
    {
        validateChatRequest(request);
        if (deltaConsumer == null) throw new ServiceException("流式响应处理器不能为空");
        return executeChat(request, "STREAM_CHAT",
                credential -> client.streamChat(credential, request, deltaConsumer));
    }

    @Override
    public AiStructuredResponse structuredChat(AiStructuredRequest request)
    {
        validateChatRequest(request);
        if (!StringUtils.hasText(request.getSchemaName())
                || !request.getSchemaName().matches("[A-Za-z0-9_-]{1,64}"))
        {
            throw new ServiceException("结构化输出 Schema 名称只能包含字母、数字、下划线和短横线");
        }
        if (request.getJsonSchema() == null || !request.getJsonSchema().isObject())
        {
            throw new ServiceException("结构化输出 JSON Schema 必须是对象");
        }
        AiChatResponse response = executeChat(request, "STRUCTURED_CHAT",
                credential -> client.structuredChat(credential, request));
        try
        {
            JsonNode data = objectMapper.readTree(response.content());
            if (data == null) throw new IllegalArgumentException("empty JSON");
            return new AiStructuredResponse(data, response.content(), response.model(), response.requestId(),
                    response.finishReason(), response.tokenUsage());
        }
        catch (Exception ex)
        {
            throw new ServiceException("模型未按约定返回有效 JSON");
        }
    }

    @Override
    public AiEmbeddingResponse embedding(AiEmbeddingRequest request)
    {
        validateEmbeddingRequest(request);
        long started = System.nanoTime();
        AiModelCredential credential = null;
        try
        {
            credential = modelConfigService.resolveCredential(request.getModelConfigId(), true);
            if (!StringUtils.hasText(credential.config().getEmbeddingModel()))
            {
                throw new ServiceException("当前模型配置未设置 Embedding 模型");
            }
            AiEmbeddingResponse response = client.embedding(credential, request);
            record(request.getContext(), credential, "EMBEDDING", response.requestId(), response.tokenUsage(),
                    elapsedMillis(started), true, null, null);
            return response;
        }
        catch (AiProviderException ex)
        {
            record(request.getContext(), credential, "EMBEDDING", null, AiTokenUsage.EMPTY,
                    elapsedMillis(started), false, ex.getErrorCode(), ex.getMessage());
            throw new ServiceException(ex.getMessage());
        }
        catch (ServiceException ex)
        {
            record(request.getContext(), credential, "EMBEDDING", null, AiTokenUsage.EMPTY,
                    elapsedMillis(started), false, "AI_CONFIG_ERROR", ex.getMessage());
            throw ex;
        }
        catch (RuntimeException ex)
        {
            record(request.getContext(), credential, "EMBEDDING", null, AiTokenUsage.EMPTY,
                    elapsedMillis(started), false, "AI_INTERNAL_ERROR", "模型调用发生内部错误");
            throw new ServiceException("模型调用失败，请稍后重试");
        }
    }

    @Override
    public AiChatResponse testConnection(Long modelConfigId, AiInvocationContext context)
    {
        if (modelConfigId == null) throw new ServiceException("模型配置ID不能为空");
        AiChatRequest request = new AiChatRequest();
        request.setModelConfigId(modelConfigId);
        request.setMessages(List.of(new AiMessage("user", "Reply with OK only.")));
        request.setMaxTokens(8);
        request.setTemperature(java.math.BigDecimal.ZERO);
        request.setContext(context);
        validateChatRequest(request);
        long started = System.nanoTime();
        AiModelCredential credential = null;
        try
        {
            credential = modelConfigService.resolveCredential(modelConfigId, false);
            AiChatResponse response = client.chat(credential, request);
            record(context, credential, "CONNECTION_TEST", response.requestId(), response.tokenUsage(),
                    elapsedMillis(started), true, null, null);
            return response;
        }
        catch (AiProviderException ex)
        {
            record(context, credential, "CONNECTION_TEST", null, AiTokenUsage.EMPTY,
                    elapsedMillis(started), false, ex.getErrorCode(), ex.getMessage());
            throw new ServiceException(ex.getMessage());
        }
        catch (ServiceException ex)
        {
            record(context, credential, "CONNECTION_TEST", null, AiTokenUsage.EMPTY,
                    elapsedMillis(started), false, "AI_CONFIG_ERROR", ex.getMessage());
            throw ex;
        }
        catch (RuntimeException ex)
        {
            record(context, credential, "CONNECTION_TEST", null, AiTokenUsage.EMPTY,
                    elapsedMillis(started), false, "AI_INTERNAL_ERROR", "模型调用发生内部错误");
            throw new ServiceException("模型连接测试失败，请稍后重试");
        }
    }

    private AiChatResponse executeChat(AiChatRequest request, String requestType,
            ChatExecutor executor)
    {
        long started = System.nanoTime();
        AiModelCredential credential = null;
        try
        {
            credential = modelConfigService.resolveCredential(request.getModelConfigId(), true);
            if (!StringUtils.hasText(credential.config().getChatModel()))
            {
                throw new ServiceException("当前模型配置未设置对话模型");
            }
            AiChatResponse response = executor.execute(credential);
            record(request.getContext(), credential, requestType, response.requestId(), response.tokenUsage(),
                    elapsedMillis(started), true, null, null);
            return response;
        }
        catch (AiProviderException ex)
        {
            record(request.getContext(), credential, requestType, null, AiTokenUsage.EMPTY,
                    elapsedMillis(started), false, ex.getErrorCode(), ex.getMessage());
            throw new ServiceException(ex.getMessage());
        }
        catch (ServiceException ex)
        {
            record(request.getContext(), credential, requestType, null, AiTokenUsage.EMPTY,
                    elapsedMillis(started), false, "AI_CONFIG_ERROR", ex.getMessage());
            throw ex;
        }
        catch (RuntimeException ex)
        {
            record(request.getContext(), credential, requestType, null, AiTokenUsage.EMPTY,
                    elapsedMillis(started), false, "AI_INTERNAL_ERROR", "模型调用发生内部错误");
            throw new ServiceException("模型调用失败，请稍后重试");
        }
    }

    private void validateChatRequest(AiChatRequest request)
    {
        if (request == null) throw new ServiceException("模型请求不能为空");
        List<AiMessage> messages = request.getMessages();
        if (messages == null || messages.isEmpty()) throw new ServiceException("模型消息不能为空");
        if (messages.size() > MAX_MESSAGES) throw new ServiceException("模型消息数量不能超过 " + MAX_MESSAGES);
        int totalChars = 0;
        for (AiMessage message : messages)
        {
            if (message == null || !ALLOWED_ROLES.contains(message.role()))
            {
                throw new ServiceException("模型消息角色不受支持");
            }
            totalChars += message.content().length();
            if (totalChars > MAX_MESSAGE_CHARS)
            {
                throw new ServiceException("模型消息内容过长");
            }
        }
        if (request.getTemperature() != null
                && (request.getTemperature().signum() < 0
                || request.getTemperature().compareTo(java.math.BigDecimal.valueOf(2)) > 0))
        {
            throw new ServiceException("temperature 必须在 0 到 2 之间");
        }
        if (request.getMaxTokens() != null
                && (request.getMaxTokens() < 1 || request.getMaxTokens() > 1_000_000))
        {
            throw new ServiceException("maxTokens 必须在 1 到 1000000 之间");
        }
    }

    private void validateEmbeddingRequest(AiEmbeddingRequest request)
    {
        if (request == null || request.getInputs() == null || request.getInputs().isEmpty())
        {
            throw new ServiceException("Embedding 输入不能为空");
        }
        if (request.getInputs().size() > MAX_EMBEDDING_INPUTS)
        {
            throw new ServiceException("Embedding 单次输入数量不能超过 " + MAX_EMBEDDING_INPUTS);
        }
        int totalChars = 0;
        for (String input : request.getInputs())
        {
            if (!StringUtils.hasText(input)) throw new ServiceException("Embedding 输入不能包含空文本");
            totalChars += input.length();
            if (totalChars > MAX_EMBEDDING_CHARS)
            {
                throw new ServiceException("Embedding 输入内容过长");
            }
        }
    }

    private void record(AiInvocationContext context, AiModelCredential credential, String requestType,
            String requestId, AiTokenUsage usage, long durationMs, boolean success,
            String errorCode, String errorMessage)
    {
        AiRequestLog log = new AiRequestLog();
        AiInvocationContext resolved = resolveContext(context);
        log.setUserId(resolved.userId());
        log.setCreateBy(resolved.username());
        log.setBusinessType(resolved.businessType());
        log.setBusinessId(resolved.businessId());
        log.setRequestType(requestType);
        log.setRequestId(requestId);
        log.setDurationMs(durationMs);
        log.setSuccess(success ? "Y" : "N");
        log.setErrorCode(errorCode);
        log.setErrorMessage(errorMessage);
        AiTokenUsage safeUsage = usage == null ? AiTokenUsage.EMPTY : usage;
        log.setPromptTokens(safeUsage.promptTokens());
        log.setCompletionTokens(safeUsage.completionTokens());
        log.setTotalTokens(safeUsage.totalTokens());
        if (credential != null)
        {
            log.setModelConfigId(credential.config().getId());
            log.setModelConfigName(credential.config().getName());
            log.setProviderType(credential.config().getProviderType());
            log.setModelName("EMBEDDING".equals(requestType)
                    ? credential.config().getEmbeddingModel() : credential.config().getChatModel());
        }
        requestLogService.record(log);
    }

    private AiInvocationContext resolveContext(AiInvocationContext context)
    {
        if (context != null) return context;
        try
        {
            return new AiInvocationContext(SecurityUtils.getUserId(), SecurityUtils.getUsername(), null, null);
        }
        catch (RuntimeException ignored)
        {
            return AiInvocationContext.system(null, null);
        }
    }

    private long elapsedMillis(long started)
    {
        return (System.nanoTime() - started) / 1_000_000L;
    }

    @FunctionalInterface
    private interface ChatExecutor
    {
        AiChatResponse execute(AiModelCredential credential);
    }
}
