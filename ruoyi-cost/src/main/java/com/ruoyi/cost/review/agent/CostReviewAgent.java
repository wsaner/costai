package com.ruoyi.cost.review.agent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.ai.agent.AiAgent;
import com.ruoyi.cost.ai.model.protocol.AiInvocationContext;
import com.ruoyi.cost.ai.model.protocol.AiMessage;
import com.ruoyi.cost.ai.model.protocol.AiStructuredRequest;
import com.ruoyi.cost.ai.model.protocol.AiStructuredResponse;
import com.ruoyi.cost.ai.model.service.AiModelService;
import com.ruoyi.cost.ai.prompt.domain.AiPromptTemplate;
import com.ruoyi.cost.ai.prompt.service.AiPromptTemplateService;
import org.springframework.stereotype.Component;

/**
 * 单问题造价语义审核 Agent。严格使用 JSON Schema Structured Output，不解析自然语言。
 */
@Component
public class CostReviewAgent implements AiAgent<CostReviewAgentContext, CostReviewAgentResult>
{
    public static final String PROMPT_CODE = "COST_REVIEW_AGENT";
    public static final String CONTEXT_PLACEHOLDER = "{{reviewContext}}";
    public static final int MAX_OUTPUT_TOKENS = 900;
    private static final int MAX_SYSTEM_PROMPT_CHARS = 20_000;
    private static final int MAX_USER_TEMPLATE_CHARS = 10_000;
    private static final Set<String> ISSUE_TYPES = Set.of("QUANTITY", "UNIT_PRICE", "TOTAL_PRICE",
            "DUPLICATE", "MISSING", "NEW_ITEM", "FEATURE", "DATA", "WRONG_ITEM", "OTHER");
    private static final Set<String> RISK_LEVELS = Set.of("INFO", "LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final Set<String> RESULT_FIELDS = Set.of("hasIssue", "issueType", "riskLevel",
            "title", "analysis", "suggestion", "confidence");

    private final AiModelService modelService;
    private final AiPromptTemplateService promptService;
    private final ObjectMapper objectMapper;

    public CostReviewAgent(AiModelService modelService, AiPromptTemplateService promptService,
            ObjectMapper objectMapper)
    {
        this.modelService = modelService;
        this.promptService = promptService;
        this.objectMapper = objectMapper;
    }

    @Override
    public CostReviewAgentResult execute(CostReviewAgentContext context)
    {
        validateContext(context);
        AiPromptTemplate prompt = promptService.selectActive(PROMPT_CODE);
        validatePrompt(prompt);

        AiStructuredRequest request = new AiStructuredRequest();
        request.setModelConfigId(context.modelConfigId());
        request.setTemperature(new BigDecimal("0.10"));
        request.setMaxTokens(MAX_OUTPUT_TOKENS);
        request.setSchemaName("cost_review_result");
        request.setJsonSchema(createSchema());
        request.setMessages(List.of(
                new AiMessage("system", prompt.getSystemPrompt()),
                new AiMessage("user", prompt.getUserTemplate().replace(
                        CONTEXT_PLACEHOLDER, context.reviewContextJson()))));
        request.setContext(new AiInvocationContext(context.userId(), context.username(),
                "COST_REVIEW_ISSUE", String.valueOf(context.issueId())));

        AiStructuredResponse response = modelService.structuredChat(request);
        CostReviewAgentResult result = parseAndValidate(response.data());
        result.setModel(response.model());
        result.setRequestId(response.requestId());
        result.setTokenUsage(response.tokenUsage());
        return result;
    }

    public ObjectNode createSchema()
    {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("hasIssue").put("type", "boolean");
        enumProperty(properties, "issueType", ISSUE_TYPES);
        enumProperty(properties, "riskLevel", RISK_LEVELS);
        stringProperty(properties, "title", 200);
        stringProperty(properties, "analysis", 4000);
        stringProperty(properties, "suggestion", 4000);
        properties.putObject("confidence").put("type", "number").put("minimum", 0).put("maximum", 1);
        ArrayNode required = schema.putArray("required");
        RESULT_FIELDS.forEach(required::add);
        return schema;
    }

    CostReviewAgentResult parseAndValidate(JsonNode data)
    {
        if (data == null || !data.isObject()) throw invalidResult();
        Set<String> actual = new HashSet<>();
        Iterator<String> fields = data.fieldNames();
        fields.forEachRemaining(actual::add);
        if (!actual.equals(RESULT_FIELDS)) throw invalidResult();
        if (!data.path("hasIssue").isBoolean() || !data.path("issueType").isTextual()
                || !data.path("riskLevel").isTextual() || !data.path("title").isTextual()
                || !data.path("analysis").isTextual() || !data.path("suggestion").isTextual()
                || !data.path("confidence").isNumber()) throw invalidResult();

        CostReviewAgentResult result = new CostReviewAgentResult();
        result.setHasIssue(data.get("hasIssue").booleanValue());
        result.setIssueType(enumValue(data.get("issueType").textValue(), ISSUE_TYPES));
        result.setRiskLevel(enumValue(data.get("riskLevel").textValue(), RISK_LEVELS));
        result.setTitle(requiredText(data.get("title").textValue(), 200));
        result.setAnalysis(requiredText(data.get("analysis").textValue(), 4000));
        result.setSuggestion(requiredText(data.get("suggestion").textValue(), 4000));
        BigDecimal confidence = data.get("confidence").decimalValue();
        if (confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0)
            throw invalidResult();
        result.setConfidence(confidence.setScale(Math.min(4, Math.max(0, confidence.scale())), RoundingMode.HALF_UP));
        if (!result.isHasIssue()
                && (!"OTHER".equals(result.getIssueType()) || !"INFO".equals(result.getRiskLevel())))
            throw new ServiceException("模型无问题结论必须使用 OTHER/INFO");
        return result;
    }

    private void validateContext(CostReviewAgentContext context)
    {
        if (context == null || context.issueId() == null) throw new ServiceException("AI审核问题ID不能为空");
        if (context.reviewContextJson() == null || context.reviewContextJson().isBlank())
            throw new ServiceException("AI审核上下文不能为空");
        if (context.reviewContextJson().length() > CostReviewContextBuilder.MAX_CONTEXT_CHARS)
            throw new ServiceException("AI审核上下文超过安全限制");
    }

    private void validatePrompt(AiPromptTemplate prompt)
    {
        if (prompt.getSystemPrompt() == null || prompt.getSystemPrompt().isBlank()
                || prompt.getSystemPrompt().length() > MAX_SYSTEM_PROMPT_CHARS)
            throw new ServiceException("CostReviewAgent System Prompt 配置无效");
        if (prompt.getUserTemplate() == null || !prompt.getUserTemplate().contains(CONTEXT_PLACEHOLDER)
                || prompt.getUserTemplate().length() > MAX_USER_TEMPLATE_CHARS)
            throw new ServiceException("CostReviewAgent 用户模板必须包含 {{reviewContext}}");
    }

    private void enumProperty(ObjectNode properties, String name, Set<String> values)
    {
        ObjectNode property = properties.putObject(name);
        property.put("type", "string");
        ArrayNode allowed = property.putArray("enum");
        values.stream().sorted().forEach(allowed::add);
    }

    private void stringProperty(ObjectNode properties, String name, int maxLength)
    {
        properties.putObject(name).put("type", "string").put("minLength", 1).put("maxLength", maxLength);
    }

    private String enumValue(String value, Set<String> allowed)
    {
        if (value == null || !allowed.contains(value)) throw invalidResult();
        return value;
    }

    private String requiredText(String value, int maxLength)
    {
        if (value == null || value.isBlank() || value.length() > maxLength) throw invalidResult();
        return value.trim();
    }

    private ServiceException invalidResult()
    {
        return new ServiceException("模型返回的造价审核结构不符合约定");
    }
}
