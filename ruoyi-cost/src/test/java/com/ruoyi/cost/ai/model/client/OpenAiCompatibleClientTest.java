package com.ruoyi.cost.ai.model.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleClientTest
{
    private HttpServer server;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> mode = new AtomicReference<>("chat");
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private OpenAiCompatibleClient client;
    private AiModelCredential credential;

    @BeforeEach
    void setUp() throws IOException
    {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", this::handleChat);
        server.createContext("/v1/embeddings", this::handleEmbedding);
        server.start();
        AiModelConfig config = new AiModelConfig();
        config.setId(1L);
        config.setName("test");
        config.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        config.setChatModel("chat-test");
        config.setEmbeddingModel("embedding-test");
        config.setTemperature(java.math.BigDecimal.valueOf(0.2));
        config.setMaxTokens(128);
        config.setTimeoutSeconds(5);
        credential = new AiModelCredential(config, "sk-unit-test-secret");
        client = new OpenAiCompatibleClient(objectMapper);
    }

    @AfterEach
    void tearDown()
    {
        if (server != null) server.stop(0);
    }

    @Test
    void supportsChatStructuredStreamAndEmbedding()
    {
        AiChatRequest chatRequest = chatRequest();
        AiChatResponse chat = client.chat(credential, chatRequest);
        assertEquals("hello", chat.content());
        assertEquals(9, chat.tokenUsage().totalTokens());
        assertEquals("Bearer sk-unit-test-secret", authorization.get());

        mode.set("structured");
        AiStructuredRequest structured = new AiStructuredRequest();
        structured.setMessages(chatRequest.getMessages());
        structured.setSchemaName("answer_schema");
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        structured.setJsonSchema(schema);
        assertEquals("{\"answer\":\"ok\"}", client.structuredChat(credential, structured).content());
        assertTrue(receivedBody.get().contains("\"json_schema\""));

        mode.set("stream");
        StringBuilder deltas = new StringBuilder();
        AiChatResponse stream = client.streamChat(credential, chatRequest, deltas::append);
        assertEquals("hello", deltas.toString());
        assertEquals("hello", stream.content());
        assertEquals(9, stream.tokenUsage().totalTokens());

        AiEmbeddingRequest embeddingRequest = new AiEmbeddingRequest();
        embeddingRequest.setInputs(List.of("first", "second"));
        AiEmbeddingResponse embedding = client.embedding(credential, embeddingRequest);
        assertEquals(List.of(0.1, 0.2), embedding.embeddings().get(0));
        assertEquals(2, embedding.embeddings().size());
        assertEquals(4, embedding.tokenUsage().totalTokens());
    }

    @Test
    void sanitizesProviderErrorAndNeverFollowsRedirects()
    {
        mode.set("error");
        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> client.chat(credential, chatRequest()));
        assertEquals("AI_HTTP_401", exception.getErrorCode());
        assertFalse(exception.getMessage().contains("sk-unit-test-secret"));
        assertTrue(exception.getMessage().contains("***"));
    }

    private AiChatRequest chatRequest()
    {
        AiChatRequest request = new AiChatRequest();
        request.setMessages(List.of(new AiMessage("user", "hello")));
        return request;
    }

    private void handleChat(HttpExchange exchange) throws IOException
    {
        receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        String selected = mode.get();
        if ("error".equals(selected))
        {
            respond(exchange, 401, "application/json",
                    "{\"error\":{\"message\":\"bad key sk-unit-test-secret\"}}");
            return;
        }
        if ("stream".equals(selected))
        {
            respond(exchange, 200, "text/event-stream",
                    "data: {\"id\":\"req-stream\",\"model\":\"chat-test\",\"choices\":[{\"delta\":{\"content\":\"hel\"},\"finish_reason\":null}]}\n\n"
                    + "data: {\"id\":\"req-stream\",\"model\":\"chat-test\",\"choices\":[{\"delta\":{\"content\":\"lo\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":4,\"completion_tokens\":5,\"total_tokens\":9}}\n\n"
                    + "data: [DONE]\n\n");
            return;
        }
        String content = "structured".equals(selected) ? "{\\\"answer\\\":\\\"ok\\\"}" : "hello";
        respond(exchange, 200, "application/json",
                "{\"id\":\"req-chat\",\"model\":\"chat-test\",\"choices\":[{\"message\":{\"content\":\""
                + content + "\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":4,\"completion_tokens\":5,\"total_tokens\":9}}");
    }

    private void handleEmbedding(HttpExchange exchange) throws IOException
    {
        respond(exchange, 200, "application/json",
                "{\"id\":\"req-embedding\",\"model\":\"embedding-test\",\"data\":["
                + "{\"index\":1,\"embedding\":[0.3,0.4]},{\"index\":0,\"embedding\":[0.1,0.2]}],"
                + "\"usage\":{\"prompt_tokens\":4,\"total_tokens\":4}}");
    }

    private void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException
    {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("x-request-id", "header-request-id");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
