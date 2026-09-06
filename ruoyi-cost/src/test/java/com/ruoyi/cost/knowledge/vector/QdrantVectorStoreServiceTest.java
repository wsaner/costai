package com.ruoyi.cost.knowledge.vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QdrantVectorStoreServiceTest
{
    private HttpServer server;
    private final List<String> requests = new ArrayList<>();
    private final AtomicBoolean collectionExists = new AtomicBoolean(false);

    @BeforeEach
    void start() throws Exception
    {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle); server.start();
    }

    @AfterEach void stop() { server.stop(0); }

    @Test
    void shouldCreateUpsertQueryAndDeleteThroughStableBoundary()
    {
        QdrantProperties properties = new QdrantProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort()); properties.setApiKey("secret-test-key");
        QdrantVectorStoreService service = new QdrantVectorStoreService(properties, new ObjectMapper());
        service.saveVectors("costai_kb_1", 3, List.of(new VectorRecord(7L, List.of(0.1,0.2,0.3), Map.of("documentId",2L))));
        List<VectorSearchResult> hits = service.searchSimilar("costai_kb_1", List.of(0.1,0.2,0.3), 5, java.math.BigDecimal.valueOf(0.5));
        service.deleteDocumentVectors("costai_kb_1", 2L);
        assertEquals(1, hits.size()); assertEquals("7", hits.get(0).id()); assertEquals(0.88, hits.get(0).score(), 0.001);
        assertTrue(requests.stream().anyMatch(value -> value.startsWith("PUT /collections/costai_kb_1 ")));
        assertTrue(requests.stream().anyMatch(value -> value.contains("/points/query")));
        assertTrue(requests.stream().anyMatch(value -> value.contains("/points/delete")));
        assertTrue(requests.stream().allMatch(value -> value.endsWith(" auth=true")));
    }

    private void handle(HttpExchange exchange) throws IOException
    {
        String path = exchange.getRequestURI().getPath(); String method = exchange.getRequestMethod();
        requests.add(method + " " + path + " auth=" + "secret-test-key".equals(exchange.getRequestHeaders().getFirst("api-key")));
        String response; int status = 200;
        if ("GET".equals(method) && path.equals("/collections/costai_kb_1") && !collectionExists.get()) { status=404; response="{}"; }
        else if ("PUT".equals(method) && path.equals("/collections/costai_kb_1")) { collectionExists.set(true); response="{\"result\":true}"; }
        else if (path.endsWith("/points/query")) response="{\"result\":{\"points\":[{\"id\":7,\"score\":0.88,\"payload\":{\"documentId\":2}}]}}";
        else response="{\"result\":{\"status\":\"completed\"}}";
        byte[] bytes=response.getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(status,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
    }
}
