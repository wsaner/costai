package com.ruoyi.cost.ai.chat.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.ai.chat.domain.AiConversation;
import com.ruoyi.cost.ai.chat.domain.AiConversationMessage;
import com.ruoyi.cost.ai.chat.dto.AiChatSendRequest;
import com.ruoyi.cost.ai.chat.dto.AiConversationCreateRequest;
import com.ruoyi.cost.ai.chat.dto.AiConversationUpdateRequest;
import com.ruoyi.cost.ai.chat.mapper.AiChatMapper;
import com.ruoyi.cost.ai.chat.service.AiChatEvent;
import com.ruoyi.cost.ai.chat.service.AiChatService;
import com.ruoyi.cost.ai.chat.vo.AiChatContextVo;
import com.ruoyi.cost.ai.chat.vo.AiChatProjectOptionVo;
import com.ruoyi.cost.ai.model.protocol.AiChatRequest;
import com.ruoyi.cost.ai.model.protocol.AiChatResponse;
import com.ruoyi.cost.ai.model.protocol.AiInvocationContext;
import com.ruoyi.cost.ai.model.protocol.AiMessage;
import com.ruoyi.cost.ai.model.service.AiModelService;
import com.ruoyi.cost.ai.prompt.domain.AiPromptTemplate;
import com.ruoyi.cost.ai.prompt.service.AiPromptTemplateService;
import com.ruoyi.cost.ai.tool.GetProjectSummaryTool;
import com.ruoyi.cost.ai.tool.GetReviewIssuesTool;
import com.ruoyi.cost.ai.tool.ProjectToolInput;
import com.ruoyi.cost.ai.tool.SearchBoqTool;
import com.ruoyi.cost.project.domain.CostProject;
import com.ruoyi.cost.project.service.ICostProjectService;

@Service
public class AiChatServiceImpl implements AiChatService
{
    private static final int HISTORY_LIMIT = 20;
    private static final int HISTORY_CHAR_LIMIT = 30000;
    private static final Pattern QUOTED_KEYWORD = Pattern.compile("[\\\"“‘']([^\\\"”’']{2,40})[\\\"”’']");
    private static final Pattern ITEM_CODE = Pattern.compile("(?i)(?:编码|编号)\\s*[:：]?\\s*([a-z0-9][a-z0-9._/-]{3,39})");

    private final AiChatMapper mapper;
    private final ICostProjectService projectService;
    private final GetProjectSummaryTool projectSummaryTool;
    private final GetReviewIssuesTool reviewIssuesTool;
    private final SearchBoqTool searchBoqTool;
    private final AiPromptTemplateService promptService;
    private final AiModelService modelService;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    public AiChatServiceImpl(AiChatMapper mapper, ICostProjectService projectService,
            GetProjectSummaryTool projectSummaryTool, GetReviewIssuesTool reviewIssuesTool,
            SearchBoqTool searchBoqTool, AiPromptTemplateService promptService, AiModelService modelService,
            ObjectMapper objectMapper, @Qualifier("threadPoolTaskExecutor") Executor executor)
    {
        this.mapper = mapper; this.projectService = projectService; this.projectSummaryTool = projectSummaryTool;
        this.reviewIssuesTool = reviewIssuesTool; this.searchBoqTool = searchBoqTool;
        this.promptService = promptService; this.modelService = modelService;
        this.objectMapper = objectMapper; this.executor = executor;
    }

    @Override public List<AiConversation> selectConversations(Long userId) { return mapper.selectConversationList(userId); }

    @Override public AiConversation selectConversation(Long id, Long userId)
    {
        AiConversation value = mapper.selectConversation(id, userId);
        if (value == null) throw new ServiceException("会话不存在或无权访问");
        return value;
    }

    @Override public List<AiConversationMessage> selectMessages(Long conversationId, Long userId)
    {
        selectConversation(conversationId, userId);
        return mapper.selectMessages(conversationId, userId);
    }

    @Override
    public List<AiChatProjectOptionVo> selectProjectOptions(String keyword)
    {
        CostProject query = new CostProject();
        if (StringUtils.isNotBlank(keyword)) query.setProjectName(keyword.trim());
        return projectService.selectCostProjectList(query).stream()
                .map(p -> new AiChatProjectOptionVo(p.getId(), p.getProjectCode(), p.getProjectName())).toList();
    }

    @Override public Long create(AiConversationCreateRequest request, Long userId, String username)
    {
        String mode = normalizeMode(request.getMode());
        CostProject project = validateProject(mode, request.getProjectId());
        AiConversation value = new AiConversation();
        value.setUserId(userId); value.setTitle(defaultTitle(request.getTitle())); value.setMode(mode);
        value.setProjectId(project == null ? null : project.getId());
        value.setProjectName(project == null ? null : project.getProjectName()); value.setCreateBy(username);
        mapper.insertConversation(value);
        return value.getId();
    }

    @Override public int update(AiConversationUpdateRequest request, Long userId, String username)
    {
        selectConversation(request.getId(), userId);
        String mode = normalizeMode(request.getMode());
        CostProject project = validateProject(mode, request.getProjectId());
        AiConversation value = new AiConversation(); value.setId(request.getId()); value.setUserId(userId);
        value.setTitle(defaultTitle(request.getTitle())); value.setMode(mode);
        value.setProjectId(project == null ? null : project.getId());
        value.setProjectName(project == null ? null : project.getProjectName()); value.setUpdateBy(username);
        int rows = mapper.updateConversation(value);
        if (rows == 0) throw new ServiceException("会话正在生成回答，暂不能修改");
        return rows;
    }

    @Override @Transactional
    public int delete(Long id, Long userId, String username)
    {
        selectConversation(id, userId);
        int rows = mapper.deleteConversation(id, userId, username);
        if (rows == 0) throw new ServiceException("会话正在生成回答，暂不能删除");
        mapper.deleteMessages(id, userId, username);
        return rows;
    }

    @Override
    @Transactional
    public void stream(Long conversationId, AiChatSendRequest request, Long userId, String username,
            Consumer<AiChatEvent> eventConsumer)
    {
        AiConversation conversation = selectConversation(conversationId, userId);
        CostProject project = validateProject(conversation.getMode(), conversation.getProjectId());
        if (mapper.claimGeneration(conversationId, userId) == 0)
            throw new ServiceException("当前会话已有回答正在生成");
        try
        {
            AiChatContextVo context = buildContext(conversation, project, request.getContent());
            AiChatRequest modelRequest = buildModelRequest(conversation, request.getContent(), context, userId, username);
            String sourcesJson = json(context.getSources());
            String toolCallsJson = json(context.getToolCalls());
            AiConversationMessage userMessage = newMessage(conversationId, userId, "USER", request.getContent().trim(),
                    "COMPLETED", username, null, null);
            mapper.insertMessage(userMessage);
            AiConversationMessage assistantMessage = newMessage(conversationId, userId, "ASSISTANT", "", "STREAMING",
                    username, sourcesJson, toolCallsJson);
            mapper.insertMessage(assistantMessage);
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("conversationId", conversationId); meta.put("userMessageId", userMessage.getId());
            meta.put("assistantMessageId", assistantMessage.getId());
            eventConsumer.accept(new AiChatEvent("meta", meta));
            eventConsumer.accept(new AiChatEvent("context", context));
            runAfterCommit(() -> generate(conversation, assistantMessage, modelRequest, context,
                    request.getContent(), username, eventConsumer));
        }
        catch (RuntimeException ex)
        {
            mapper.releaseGeneration(conversationId, userId, 0, null, username);
            throw ex;
        }
    }

    /** 保证消息占位事务提交后才发起外部模型网络调用。单元测试无事务同步时直接执行。 */
    private void runAfterCommit(Runnable task)
    {
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
            {
                @Override public void afterCommit() { executor.execute(task); }
            });
        }
        else executor.execute(task);
    }

    private void generate(AiConversation conversation, AiConversationMessage message, AiChatRequest request,
            AiChatContextVo context, String question, String username, Consumer<AiChatEvent> eventConsumer)
    {
        try
        {
            AiChatResponse response = modelService.streamChat(request,
                    delta -> eventConsumer.accept(new AiChatEvent("delta", Map.of("text", delta))));
            message.setContent(response.content()); message.setStatus("COMPLETED"); message.setModel(response.model());
            message.setRequestId(response.requestId()); message.setTokenUsageJson(json(response.tokenUsage()));
            message.setSourcesJson(json(context.getSources())); message.setToolCallsJson(json(context.getToolCalls()));
            message.setUpdateBy(username); mapper.completeMessage(message);
            String title = conversation.getMessageCount() == null || conversation.getMessageCount() == 0
                    ? titleFromQuestion(question) : null;
            mapper.releaseGeneration(conversation.getId(), conversation.getUserId(), 2, title, username);
            eventConsumer.accept(new AiChatEvent("usage", response.tokenUsage()));
            eventConsumer.accept(new AiChatEvent("done", Map.of("messageId", message.getId(), "status", "COMPLETED")));
        }
        catch (RuntimeException ex)
        {
            message.setStatus("FAILED"); message.setErrorMessage("AI回答生成失败，请检查模型配置或稍后重试");
            message.setUpdateBy(username); mapper.completeMessage(message);
            mapper.releaseGeneration(conversation.getId(), conversation.getUserId(), 2, null, username);
            safeEvent(eventConsumer, new AiChatEvent("error", Map.of("message", message.getErrorMessage())));
        }
    }

    private AiChatContextVo buildContext(AiConversation conversation, CostProject project, String question)
    {
        AiChatContextVo context = new AiChatContextVo();
        if (project == null) return context;
        ProjectToolInput summaryInput = new ProjectToolInput(project.getId(), null, 1, project);
        Map<String, Object> summary = projectSummaryTool.execute(summaryInput);
        context.getData().put("projectSummary", summary);
        addTool(context, projectSummaryTool.getName(), 1, "项目档案、清单汇总和最近审核统计");
        List<Map<String, Object>> issues = reviewIssuesTool.execute(new ProjectToolInput(project.getId(), null, 10, project));
        context.getData().put("reviewIssues", issues);
        addTool(context, reviewIssuesTool.getName(), issues.size(), "按风险等级排序，最多10条");
        String keyword = extractKeyword(question);
        if (keyword != null)
        {
            List<Map<String, Object>> items = searchBoqTool.execute(new ProjectToolInput(project.getId(), keyword, 10, project));
            context.getData().put("boqMatches", items);
            context.getData().put("boqKeyword", keyword);
            addTool(context, searchBoqTool.getName(), items.size(), "关键词检索，最多10条");
        }
        context.getSources().add(source("PROJECT", "项目档案：《" + project.getProjectName() + "》", project.getId()));
        if (!issues.isEmpty()) context.getSources().add(source("REVIEW", "项目审核问题（限量）", project.getId()));
        if (keyword != null) context.getSources().add(source("BOQ", "工程量清单检索：" + keyword, project.getId()));
        return context;
    }

    private AiChatRequest buildModelRequest(AiConversation conversation, String question, AiChatContextVo context,
            Long userId, String username)
    {
        AiPromptTemplate prompt = promptService.selectActive("COST_CHAT_ASSISTANT");
        if (prompt == null) throw new ServiceException("未配置启用的AI造价助手Prompt模板");
        List<AiMessage> messages = new ArrayList<>(); messages.add(new AiMessage("system", prompt.getSystemPrompt()));
        int chars = 0;
        List<AiConversationMessage> recent = mapper.selectRecentMessages(conversation.getId(), userId, HISTORY_LIMIT);
        List<AiConversationMessage> selected = new ArrayList<>();
        for (int index = recent.size() - 1; index >= 0; index--)
        {
            AiConversationMessage old = recent.get(index);
            if (old.getContent() == null || chars + old.getContent().length() > HISTORY_CHAR_LIMIT) continue;
            selected.add(0, old); chars += old.getContent().length();
        }
        for (AiConversationMessage old : selected)
            messages.add(new AiMessage("USER".equals(old.getRole()) ? "user" : "assistant", old.getContent()));
        String projectContext = context.getData().isEmpty() ? "未选择项目。" : json(context.getData());
        String userContent = renderPrompt(prompt.getUserTemplate(), question.trim(), projectContext);
        messages.add(new AiMessage("user", userContent));
        AiChatRequest request = new AiChatRequest(); request.setMessages(messages);
        request.setContext(new AiInvocationContext(userId, username, "COST_CHAT", String.valueOf(conversation.getId())));
        return request;
    }

    /** 只替换模板原文中的占位符，不再次解释用户问题或检索数据中出现的占位符。 */
    private String renderPrompt(String template, String question, String projectContext)
    {
        StringBuilder output = new StringBuilder(template.length() + question.length() + projectContext.length());
        int cursor = 0;
        while (cursor < template.length())
        {
            int questionAt = template.indexOf("{{userQuestion}}", cursor);
            int contextAt = template.indexOf("{{projectContext}}", cursor);
            int next = questionAt < 0 ? contextAt : contextAt < 0 ? questionAt : Math.min(questionAt, contextAt);
            if (next < 0) { output.append(template, cursor, template.length()); break; }
            output.append(template, cursor, next);
            if (next == questionAt) { output.append(question); cursor = next + "{{userQuestion}}".length(); }
            else { output.append(projectContext); cursor = next + "{{projectContext}}".length(); }
        }
        return output.toString();
    }

    private CostProject validateProject(String mode, Long projectId)
    {
        if (!"PROJECT".equals(mode)) return null;
        if (projectId == null) throw new ServiceException("项目问答必须选择项目");
        return projectService.selectCostProjectById(projectId);
    }
    private String normalizeMode(String mode)
    {
        String value = StringUtils.isBlank(mode) ? "GENERAL" : mode.trim().toUpperCase(Locale.ROOT);
        if (!"GENERAL".equals(value) && !"PROJECT".equals(value)) throw new ServiceException("会话模式无效");
        return value;
    }
    private String defaultTitle(String title) { return StringUtils.isBlank(title) ? "新会话" : title.trim(); }
    private String titleFromQuestion(String value)
    {
        String title = value == null ? "新会话" : value.trim().replaceAll("\\s+", " ");
        return title.length() <= 30 ? title : title.substring(0, 30) + "…";
    }
    private AiConversationMessage newMessage(Long conversationId, Long userId, String role, String content,
            String status, String username, String sources, String tools)
    {
        AiConversationMessage value = new AiConversationMessage(); value.setConversationId(conversationId);
        value.setUserId(userId); value.setRole(role); value.setContent(content); value.setStatus(status);
        value.setCreateBy(username); value.setSourcesJson(sources); value.setToolCallsJson(tools); return value;
    }
    private String extractKeyword(String question)
    {
        Matcher quoted = QUOTED_KEYWORD.matcher(question); if (quoted.find()) return quoted.group(1).trim();
        Matcher code = ITEM_CODE.matcher(question); if (code.find()) return code.group(1).trim();
        return null;
    }
    private void addTool(AiChatContextVo context, String name, int resultCount, String scope)
    {
        Map<String, Object> call = new LinkedHashMap<>(); call.put("name", name);
        call.put("resultCount", resultCount); call.put("scope", scope); context.getToolCalls().add(call);
    }
    private Map<String, Object> source(String type, String title, Long businessId)
    {
        Map<String, Object> value = new LinkedHashMap<>(); value.put("type", type); value.put("title", title);
        value.put("businessId", businessId); return value;
    }
    private String json(Object value)
    {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new ServiceException("AI上下文序列化失败"); }
    }
    private void safeEvent(Consumer<AiChatEvent> consumer, AiChatEvent event)
    {
        try { consumer.accept(event); } catch (RuntimeException ignored) { }
    }
}
