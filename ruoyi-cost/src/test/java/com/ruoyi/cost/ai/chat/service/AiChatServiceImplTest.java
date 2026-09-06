package com.ruoyi.cost.ai.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.ai.chat.domain.AiConversation;
import com.ruoyi.cost.ai.chat.domain.AiConversationMessage;
import com.ruoyi.cost.ai.chat.dto.AiChatSendRequest;
import com.ruoyi.cost.ai.chat.dto.AiConversationCreateRequest;
import com.ruoyi.cost.ai.chat.mapper.AiChatMapper;
import com.ruoyi.cost.ai.chat.service.impl.AiChatServiceImpl;
import com.ruoyi.cost.ai.model.protocol.AiChatRequest;
import com.ruoyi.cost.ai.model.protocol.AiChatResponse;
import com.ruoyi.cost.ai.model.protocol.AiTokenUsage;
import com.ruoyi.cost.ai.model.service.AiModelService;
import com.ruoyi.cost.ai.prompt.domain.AiPromptTemplate;
import com.ruoyi.cost.ai.prompt.service.AiPromptTemplateService;
import com.ruoyi.cost.ai.tool.GetProjectSummaryTool;
import com.ruoyi.cost.ai.tool.GetReviewIssuesTool;
import com.ruoyi.cost.ai.tool.SearchBoqTool;
import com.ruoyi.cost.project.domain.CostProject;
import com.ruoyi.cost.project.service.ICostProjectService;

class AiChatServiceImplTest
{
    private AiChatMapper mapper;
    private ICostProjectService projectService;
    private AiModelService modelService;
    private AiChatService service;

    @BeforeEach
    void setUp()
    {
        mapper = mock(AiChatMapper.class); projectService = mock(ICostProjectService.class);
        modelService = mock(AiModelService.class); AiPromptTemplateService promptService = mock(AiPromptTemplateService.class);
        AiPromptTemplate prompt = new AiPromptTemplate(); prompt.setSystemPrompt("system");
        prompt.setUserTemplate("{{userQuestion}}\n{{projectContext}}");
        when(promptService.selectActive("COST_CHAT_ASSISTANT")).thenReturn(prompt);
        GetProjectSummaryTool summary = new GetProjectSummaryTool(mapper);
        GetReviewIssuesTool issues = new GetReviewIssuesTool(mapper);
        SearchBoqTool boq = new SearchBoqTool(mapper);
        Executor sameThread = Runnable::run;
        service = new AiChatServiceImpl(mapper, projectService, summary, issues, boq, promptService,
                modelService, new ObjectMapper(), sameThread);
    }

    @Test
    void projectConversationMustReuseProjectDataPermissionCheck()
    {
        AiConversationCreateRequest request = new AiConversationCreateRequest();
        request.setMode("PROJECT"); request.setProjectId(99L);
        when(projectService.selectCostProjectById(99L)).thenThrow(new ServiceException("项目不存在或无权访问"));
        assertThrows(ServiceException.class, () -> service.create(request, 7L, "tester"));
        verify(mapper, never()).insertConversation(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void streamPersistsMessagesAndEmitsStructuredEvents()
    {
        AiConversation conversation = conversation("GENERAL", null);
        when(mapper.selectConversation(1L, 7L)).thenReturn(conversation);
        when(mapper.claimGeneration(1L, 7L)).thenReturn(1);
        when(mapper.selectRecentMessages(1L, 7L, 20)).thenReturn(Collections.emptyList());
        final long[] id = {10};
        org.mockito.Mockito.doAnswer(inv -> { ((AiConversationMessage) inv.getArgument(0)).setId(id[0]++); return 1; })
                .when(mapper).insertMessage(any());
        when(modelService.streamChat(any(), any())).thenAnswer(inv -> {
            ((Consumer<String>) inv.getArgument(1)).accept("审核");
            ((Consumer<String>) inv.getArgument(1)).accept("建议");
            return new AiChatResponse("审核建议", "test-model", "req-1", "stop", new AiTokenUsage(8, 2, 10));
        });
        AiChatSendRequest request = new AiChatSendRequest(); request.setContent("请分析{{projectContext}}风险");
        List<AiChatEvent> events = new ArrayList<>();
        service.stream(1L, request, 7L, "tester", events::add);
        assertEquals(List.of("meta", "context", "delta", "delta", "usage", "done"),
                events.stream().map(AiChatEvent::type).toList());
        ArgumentCaptor<AiConversationMessage> messageCaptor = ArgumentCaptor.forClass(AiConversationMessage.class);
        verify(mapper).completeMessage(messageCaptor.capture());
        assertEquals("COMPLETED", messageCaptor.getValue().getStatus());
        assertEquals("审核建议", messageCaptor.getValue().getContent());
        verify(mapper).releaseGeneration(1L, 7L, 2, "请分析{{projectContext}}风险", "tester");
        ArgumentCaptor<AiChatRequest> requestCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(modelService).streamChat(requestCaptor.capture(), any());
        String renderedUserMessage = requestCaptor.getValue().getMessages()
                .get(requestCaptor.getValue().getMessages().size() - 1).content();
        assertTrue(renderedUserMessage.contains("请分析{{projectContext}}风险"));
        assertTrue(renderedUserMessage.contains("未选择项目"));
    }

    @Test
    void projectContextIsBoundedAndIncludesThreeControlledTools()
    {
        CostProject project = new CostProject(); project.setId(9L); project.setProjectName("医院项目");
        AiConversation conversation = conversation("PROJECT", 9L); conversation.setMessageCount(2);
        when(mapper.selectConversation(1L, 7L)).thenReturn(conversation);
        when(projectService.selectCostProjectById(9L)).thenReturn(project);
        when(mapper.claimGeneration(1L, 7L)).thenReturn(1);
        when(mapper.selectRecentMessages(anyLong(), anyLong(), anyInt())).thenReturn(Collections.emptyList());
        when(mapper.selectBoqSummary(9L)).thenReturn(Map.of("itemCount", 100));
        when(mapper.selectReviewIssues(9L, 10)).thenReturn(List.of(Map.of("issueTitle", "单价异常")));
        when(mapper.searchBoq(9L, "C30混凝土", 10)).thenReturn(List.of(Map.of("itemName", "C30混凝土")));
        final long[] id = {20};
        org.mockito.Mockito.doAnswer(inv -> { ((AiConversationMessage) inv.getArgument(0)).setId(id[0]++); return 1; })
                .when(mapper).insertMessage(any());
        when(modelService.streamChat(any(), any())).thenReturn(new AiChatResponse("ok", "m", "r", "stop", AiTokenUsage.EMPTY));
        AiChatSendRequest request = new AiChatSendRequest(); request.setContent("请核查“C30混凝土”清单");
        List<AiChatEvent> events = new ArrayList<>(); service.stream(1L, request, 7L, "tester", events::add);
        AiChatEvent contextEvent = events.stream().filter(e -> "context".equals(e.type())).findFirst().orElseThrow();
        com.ruoyi.cost.ai.chat.vo.AiChatContextVo context = (com.ruoyi.cost.ai.chat.vo.AiChatContextVo) contextEvent.data();
        assertEquals(List.of("getProjectSummary", "getReviewIssues", "searchBoq"),
                context.getToolCalls().stream().map(x -> x.get("name")).toList());
        verify(mapper).selectReviewIssues(9L, 10); verify(mapper).searchBoq(9L, "C30混凝土", 10);
    }

    @Test
    void concurrentGenerationIsRejected()
    {
        when(mapper.selectConversation(1L, 7L)).thenReturn(conversation("GENERAL", null));
        when(mapper.claimGeneration(1L, 7L)).thenReturn(0);
        AiChatSendRequest request = new AiChatSendRequest(); request.setContent("问题");
        assertThrows(ServiceException.class, () -> service.stream(1L, request, 7L, "tester", event -> {}));
        verify(modelService, never()).streamChat(any(), any());
    }

    private AiConversation conversation(String mode, Long projectId)
    {
        AiConversation value = new AiConversation(); value.setId(1L); value.setUserId(7L);
        value.setTitle("新会话"); value.setMode(mode); value.setProjectId(projectId); value.setMessageCount(0); return value;
    }
}
