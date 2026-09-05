package com.ruoyi.cost.ai.model.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.cost.ai.model.domain.AiModelConfig;
import com.ruoyi.cost.ai.model.protocol.AiChatRequest;
import com.ruoyi.cost.ai.model.protocol.AiChatResponse;
import com.ruoyi.cost.ai.model.protocol.AiEmbeddingRequest;
import com.ruoyi.cost.ai.model.protocol.AiEmbeddingResponse;
import com.ruoyi.cost.ai.model.protocol.AiMessage;
import com.ruoyi.cost.ai.model.protocol.AiModelCredential;
import com.ruoyi.cost.ai.model.protocol.AiProviderException;
import com.ruoyi.cost.ai.model.protocol.AiStructuredRequest;
import com.ruoyi.cost.ai.model.protocol.AiTokenUsage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/** OpenAI Chat Completions / Embeddings 协议适配器。 */
@Component
public class OpenAiCompatibleClient
{
    private static final int MAX_ERROR_BODY_BYTES = 8192;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public OpenAiCompatibleClient(ObjectMapper objectMapper)
    {
        this(objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    OpenAiCompatibleClient(ObjectMapper objectMapper, HttpClient httpClient)
    {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public AiChatResponse chat(AiModelCredential credential, AiChatRequest request)
    {
        return executeChat(credential, request, null);
    }

    public AiChatResponse structuredChat(AiModelCredential credential, AiStructuredRequest request)
    {
        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_schema");
        ObjectNode jsonSchema = responseFormat.putObject("json_schema");
        jsonSchema.put("name", request.getSchemaName());
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", request.getJsonSchema());
        AiChatResponse response = executeChat(credential, request, responseFormat);
        try
        {
            if (objectMapper.readTree(response.content()) == null)
            {
                throw new IOException("empty JSON");
            }
        }
        catch (Exception ex)
        {
            throw new AiProviderException("AI_INVALID_STRUCTURED_RESPONSE",
                    "模型未按约定返回有效 JSON", ex);
        }
        return response;
    }

    public AiChatResponse streamChat(AiModelCredential credential, AiChatRequest request,
            Consumer<String> deltaConsumer)
    {
        AiModelConfig config = credential.config();
        ObjectNode body = createChatBody(config, request, null);
        body.put("stream", true);
        body.putObject("stream_options").put("include_usage", true);
        HttpRequest httpRequest = buildRequest(credential, "/chat/completions", body, "text/event-stream");
        long started = System.nanoTime();
        try
        {
            HttpResponse<InputStream> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (!isSuccessful(response.statusCode()))
            {
                throw providerHttpError(response, credential.apiKey());
            }
            String requestId = response.headers().firstValue("x-request-id").orElse(null);
            String responseModel = config.getChatModel();
            String finishReason = null;
            AiTokenUsage usage = AiTokenUsage.EMPTY;
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) continue;
                    JsonNode chunk = objectMapper.readTree(data);
                    if (!StringUtils.hasText(requestId)) requestId = textOrNull(chunk.get("id"));
                    String chunkModel = textOrNull(chunk.get("model"));
                    if (StringUtils.hasText(chunkModel)) responseModel = chunkModel;
                    JsonNode choices = chunk.path("choices");
                    if (choices.isArray() && !choices.isEmpty())
                    {
                        JsonNode choice = choices.get(0);
                        String delta = extractContent(choice.path("delta").get("content"));
                        if (StringUtils.hasText(delta))
                        {
                            content.append(delta);
                            try
                            {
                                deltaConsumer.accept(delta);
                            }
                            catch (RuntimeException ex)
                            {
                                throw new AiProviderException("AI_STREAM_CONSUMER_ERROR",
                                        "流式响应处理失败", ex);
                            }
                        }
                        String reason = textOrNull(choice.get("finish_reason"));
                        if (StringUtils.hasText(reason)) finishReason = reason;
                    }
                    if (chunk.hasNonNull("usage")) usage = parseUsage(chunk.get("usage"));
                }
            }
            return new AiChatResponse(content.toString(), responseModel, requestId, finishReason, usage);
        }
        catch (AiProviderException ex)
        {
            throw ex;
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            throw new AiProviderException("AI_INTERRUPTED", "模型请求已中断", ex);
        }
        catch (Exception ex)
        {
            throw transportError(ex, started);
        }
    }

    public AiEmbeddingResponse embedding(AiModelCredential credential, AiEmbeddingRequest request)
    {
        AiModelConfig config = credential.config();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.getEmbeddingModel());
        ArrayNode input = body.putArray("input");
        request.getInputs().forEach(input::add);
        HttpRequest httpRequest = buildRequest(credential, "/embeddings", body, "application/json");
        long started = System.nanoTime();
        try
        {
            HttpResponse<InputStream> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (!isSuccessful(response.statusCode()))
            {
                throw providerHttpError(response, credential.apiKey());
            }
            JsonNode root;
            try (InputStream inputStream = response.body())
            {
                root = objectMapper.readTree(inputStream);
            }
            List<JsonNode> data = new ArrayList<>();
            root.path("data").forEach(data::add);
            data.sort(Comparator.comparingInt(node -> node.path("index").asInt()));
            List<List<Double>> embeddings = new ArrayList<>(data.size());
            for (JsonNode item : data)
            {
                JsonNode vectorNode = item.path("embedding");
                if (!vectorNode.isArray())
                {
                    throw new AiProviderException("AI_INVALID_RESPONSE", "模型服务返回了无效的向量数据");
                }
                List<Double> vector = new ArrayList<>(vectorNode.size());
                vectorNode.forEach(value -> vector.add(value.asDouble()));
                embeddings.add(vector);
            }
            if (embeddings.size() != request.getInputs().size())
            {
                throw new AiProviderException("AI_INVALID_RESPONSE", "模型服务返回的向量数量与输入不一致");
            }
            String requestId = response.headers().firstValue("x-request-id")
                    .orElse(textOrNull(root.get("id")));
            return new AiEmbeddingResponse(embeddings,
                    textOrDefault(root.get("model"), config.getEmbeddingModel()), requestId,
                    parseUsage(root.get("usage")));
        }
        catch (AiProviderException ex)
        {
            throw ex;
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            throw new AiProviderException("AI_INTERRUPTED", "模型请求已中断", ex);
        }
        catch (Exception ex)
        {
            throw transportError(ex, started);
        }
    }

    private AiChatResponse executeChat(AiModelCredential credential, AiChatRequest request,
            JsonNode responseFormat)
    {
        AiModelConfig config = credential.config();
        ObjectNode body = createChatBody(config, request, responseFormat);
        HttpRequest httpRequest = buildRequest(credential, "/chat/completions", body, "application/json");
        long started = System.nanoTime();
        try
        {
            HttpResponse<InputStream> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (!isSuccessful(response.statusCode()))
            {
                throw providerHttpError(response, credential.apiKey());
            }
            JsonNode root;
            try (InputStream input = response.body())
            {
                root = objectMapper.readTree(input);
            }
            JsonNode choice = root.path("choices").path(0);
            String content = extractContent(choice.path("message").get("content"));
            if (content == null)
            {
                throw new AiProviderException("AI_INVALID_RESPONSE", "模型服务未返回有效的文本内容");
            }
            String requestId = response.headers().firstValue("x-request-id")
                    .orElse(textOrNull(root.get("id")));
            return new AiChatResponse(content, textOrDefault(root.get("model"), config.getChatModel()),
                    requestId, textOrNull(choice.get("finish_reason")), parseUsage(root.get("usage")));
        }
        catch (AiProviderException ex)
        {
            throw ex;
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            throw new AiProviderException("AI_INTERRUPTED", "模型请求已中断", ex);
        }
        catch (Exception ex)
        {
            throw transportError(ex, started);
        }
    }

    private ObjectNode createChatBody(AiModelConfig config, AiChatRequest request, JsonNode responseFormat)
    {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.getChatModel());
        ArrayNode messages = body.putArray("messages");
        for (AiMessage message : request.getMessages())
        {
            ObjectNode item = messages.addObject();
            item.put("role", message.role());
            item.put("content", message.content());
        }
        body.put("temperature", request.getTemperature() == null
                ? config.getTemperature() : request.getTemperature());
        body.put("max_tokens", request.getMaxTokens() == null
                ? config.getMaxTokens() : request.getMaxTokens());
        if (responseFormat != null) body.set("response_format", responseFormat);
        return body;
    }

    private HttpRequest buildRequest(AiModelCredential credential, String path, JsonNode body, String accept)
    {
        AiModelConfig config = credential.config();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + path))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Accept", accept)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        if (StringUtils.hasText(credential.apiKey()))
        {
            builder.header("Authorization", "Bearer " + credential.apiKey());
        }
        return builder.build();
    }

    private AiProviderException providerHttpError(HttpResponse<InputStream> response, String apiKey)
    {
        String providerMessage = null;
        try (InputStream body = response.body())
        {
            byte[] bytes = body.readNBytes(MAX_ERROR_BODY_BYTES);
            JsonNode root = objectMapper.readTree(bytes);
            providerMessage = textOrNull(root.path("error").get("message"));
        }
        catch (Exception ignored)
        {
            // 上游错误正文不可解析时，仅返回 HTTP 状态，避免泄露原始响应。
        }
        providerMessage = sanitizeProviderMessage(providerMessage, apiKey);
        String message = "模型服务请求失败（HTTP " + response.statusCode() + "）";
        if (StringUtils.hasText(providerMessage)) message += "：" + providerMessage;
        return new AiProviderException("AI_HTTP_" + response.statusCode(), message);
    }

    private String sanitizeProviderMessage(String message, String apiKey)
    {
        if (!StringUtils.hasText(message)) return null;
        String safe = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (StringUtils.hasText(apiKey)) safe = safe.replace(apiKey, "***");
        return safe.length() > 500 ? safe.substring(0, 500) : safe;
    }

    private AiProviderException transportError(Exception ex, long started)
    {
        long elapsedMs = Duration.ofNanos(System.nanoTime() - started).toMillis();
        if (ex instanceof java.net.http.HttpTimeoutException)
        {
            return new AiProviderException("AI_TIMEOUT", "模型服务请求超时", ex);
        }
        return new AiProviderException("AI_NETWORK_ERROR",
                "无法连接模型服务（已等待 " + elapsedMs + " 毫秒）", ex);
    }

    private boolean isSuccessful(int statusCode)
    {
        return statusCode >= 200 && statusCode < 300;
    }

    private AiTokenUsage parseUsage(JsonNode usage)
    {
        if (usage == null || !usage.isObject()) return AiTokenUsage.EMPTY;
        return new AiTokenUsage(usage.path("prompt_tokens").asInt(0),
                usage.path("completion_tokens").asInt(0), usage.path("total_tokens").asInt(0));
    }

    private String extractContent(JsonNode content)
    {
        if (content == null || content.isNull()) return null;
        if (content.isTextual()) return content.asText();
        if (!content.isArray()) return content.asText(null);
        StringBuilder result = new StringBuilder();
        for (JsonNode part : content)
        {
            if (part.isTextual()) result.append(part.asText());
            else if (part.hasNonNull("text")) result.append(part.get("text").asText());
        }
        return result.toString();
    }

    private String textOrNull(JsonNode node)
    {
        return node == null || node.isNull() ? null : node.asText(null);
    }

    private String textOrDefault(JsonNode node, String defaultValue)
    {
        String value = textOrNull(node);
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
