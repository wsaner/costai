package com.ruoyi.cost.knowledge.vector;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.stereotype.Service;

/** Qdrant REST适配器；API Key仅写入请求头，不进入日志或异常。 */
@Service
public class QdrantVectorStoreService implements VectorStoreService
{
    private static final Pattern COLLECTION = Pattern.compile("[A-Za-z0-9_-]{1,128}");
    private static final int UPSERT_BATCH_SIZE = 64;
    private final QdrantProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient client;

    public QdrantVectorStoreService(QdrantProperties properties, ObjectMapper objectMapper)
    {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds()))).build();
    }

    @Override
    public void saveVectors(String collection, int dimension, List<VectorRecord> records)
    {
        validateCollection(collection);
        ensureCollection(collection, dimension);
        for (int from = 0; from < records.size(); from += UPSERT_BATCH_SIZE)
        {
            List<Map<String, Object>> points = new ArrayList<>();
            for (VectorRecord record : records.subList(from, Math.min(records.size(), from + UPSERT_BATCH_SIZE)))
            {
                if (record.vector() == null || record.vector().size() != dimension)
                    throw new ServiceException("Embedding向量维度不一致");
                points.add(Map.of("id", record.id(), "vector", record.vector(), "payload", record.payload()));
            }
            request("PUT", "/collections/" + encode(collection) + "/points?wait=true", Map.of("points", points), 200);
        }
    }

    @Override
    public List<VectorSearchResult> searchSimilar(String collection, List<Double> query, int topK, BigDecimal threshold)
    {
        validateCollection(collection);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("limit", topK);
        body.put("with_payload", true);
        if (threshold != null) body.put("score_threshold", threshold);
        JsonNode response = request("POST", "/collections/" + encode(collection) + "/points/query", body, 200);
        List<VectorSearchResult> results = new ArrayList<>();
        JsonNode points = response.path("result").path("points");
        if (points.isArray()) for (JsonNode point : points)
        {
            Map<String, Object> payload = objectMapper.convertValue(point.path("payload"), new TypeReference<>() { });
            results.add(new VectorSearchResult(point.path("id").asText(), point.path("score").asDouble(), payload));
        }
        return results;
    }

    @Override
    public void deleteDocumentVectors(String collection, Long documentId)
    {
        validateCollection(collection);
        Map<String, Object> match = Map.of("key", "documentId", "match", Map.of("value", documentId));
        request("POST", "/collections/" + encode(collection) + "/points/delete?wait=true",
                Map.of("filter", Map.of("must", List.of(match))), 200, 404);
    }

    @Override
    public void deleteCollection(String collection)
    {
        validateCollection(collection);
        request("DELETE", "/collections/" + encode(collection), null, 200, 404);
    }

    private void ensureCollection(String collection, int dimension)
    {
        JsonNode existing = request("GET", "/collections/" + encode(collection), null, 200, 404);
        if (existing == null)
        {
            request("PUT", "/collections/" + encode(collection),
                    Map.of("vectors", Map.of("size", dimension, "distance", "Cosine")), 200);
            return;
        }
        int current = existing.path("result").path("config").path("params").path("vectors").path("size").asInt(-1);
        if (current > 0 && current != dimension) throw new ServiceException("知识库向量维度与当前Embedding模型不一致，请重建索引");
    }

    private JsonNode request(String method, String path, Object body, int... accepted)
    {
        try
        {
            URI uri = baseUri().resolve(path);
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds()))).header("Accept", "application/json");
            if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) builder.header("api-key", properties.getApiKey());
            String json = body == null ? "" : objectMapper.writeValueAsString(body);
            builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json))
                    .header("Content-Type", "application/json");
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            for (int status : accepted) if (response.statusCode() == status)
                return status == 404 ? null : objectMapper.readTree(response.body());
            throw new ServiceException("向量数据库请求失败，HTTP状态码：" + response.statusCode());
        }
        catch (ServiceException e) { throw e; }
        catch (Exception e) { throw new ServiceException("无法连接向量数据库：" + safeMessage(e)); }
    }

    private URI baseUri()
    {
        String value = properties.getBaseUrl();
        URI uri;
        try { uri = URI.create(value.endsWith("/") ? value : value + "/"); }
        catch (Exception e) { throw new ServiceException("Qdrant地址配置无效"); }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null)
            throw new ServiceException("Qdrant地址配置无效");
        return uri;
    }

    private void validateCollection(String collection)
    {
        if (collection == null || !COLLECTION.matcher(collection).matches()) throw new ServiceException("向量集合名称无效");
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String safeMessage(Exception e)
    {
        String message = e.getMessage();
        return message == null || message.length() > 200 ? e.getClass().getSimpleName() : message;
    }
}
