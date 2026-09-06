package com.ruoyi.web.controller.ai;

import java.io.IOException;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.cost.ai.chat.dto.AiChatSendRequest;
import com.ruoyi.cost.ai.chat.dto.AiConversationCreateRequest;
import com.ruoyi.cost.ai.chat.dto.AiConversationUpdateRequest;
import com.ruoyi.cost.ai.chat.service.AiChatEvent;
import com.ruoyi.cost.ai.chat.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/ai/chat/conversations")
@Tag(name = "AI造价助手")
public class AiChatController extends BaseController
{
    private static final long SSE_TIMEOUT_MILLIS = 10 * 60 * 1000L;
    private final AiChatService chatService;

    public AiChatController(AiChatService chatService) { this.chatService = chatService; }

    @PreAuthorize("@ss.hasPermi('ai:chat:list')")
    @GetMapping("/list")
    @Operation(summary = "查询当前用户会话")
    public TableDataInfo list()
    {
        startPage();
        return getDataTable(chatService.selectConversations(getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('ai:chat:list')")
    @GetMapping("/project-options")
    @Operation(summary = "查询当前数据权限范围内的项目选项")
    public TableDataInfo projectOptions(String keyword)
    {
        startPage();
        return getDataTable(chatService.selectProjectOptions(keyword));
    }

    @PreAuthorize("@ss.hasPermi('ai:chat:list')")
    @GetMapping("/{id}")
    @Operation(summary = "查询当前用户会话详情")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(chatService.selectConversation(id, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('ai:chat:list')")
    @GetMapping("/{id}/messages")
    @Operation(summary = "分页查询会话消息")
    public TableDataInfo messages(@PathVariable Long id)
    {
        startPage();
        return getDataTable(chatService.selectMessages(id, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('ai:chat:use')")
    @Log(title = "AI造价助手会话", businessType = BusinessType.INSERT,
            isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping
    @Operation(summary = "新建会话")
    public AjaxResult add(@Valid @RequestBody AiConversationCreateRequest request)
    {
        return success(chatService.create(request, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('ai:chat:use')")
    @Log(title = "AI造价助手会话", businessType = BusinessType.UPDATE,
            isSaveRequestData = false, isSaveResponseData = false)
    @PutMapping
    @Operation(summary = "修改会话标题或项目上下文")
    public AjaxResult edit(@Valid @RequestBody AiConversationUpdateRequest request)
    {
        return toAjax(chatService.update(request, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('ai:chat:remove')")
    @Log(title = "AI造价助手会话", businessType = BusinessType.DELETE, isSaveResponseData = false)
    @DeleteMapping("/{id}")
    @Operation(summary = "删除当前用户会话")
    public AjaxResult remove(@PathVariable Long id)
    {
        return toAjax(chatService.delete(id, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('ai:chat:use')")
    @Log(title = "AI造价助手问答", businessType = BusinessType.OTHER,
            isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping(value = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE流式发送消息")
    public SseEmitter stream(@PathVariable Long id, @Valid @RequestBody AiChatSendRequest request)
    {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        chatService.stream(id, request, getUserId(), getUsername(), event -> send(emitter, event));
        return emitter;
    }

    private void send(SseEmitter emitter, AiChatEvent event)
    {
        try
        {
            emitter.send(SseEmitter.event().name(event.type()).data(event.data(), MediaType.APPLICATION_JSON));
            if ("done".equals(event.type()) || "error".equals(event.type())) emitter.complete();
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("SSE连接已断开", ex);
        }
    }
}
