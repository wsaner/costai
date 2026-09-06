package com.ruoyi.web.controller.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.lang.reflect.Method;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.cost.ai.log.domain.AiRequestLog;
import com.ruoyi.cost.ai.model.domain.AiModelConfig;
import com.ruoyi.cost.ai.model.dto.AiModelConfigSaveRequest;
import com.ruoyi.cost.ai.prompt.domain.AiPromptTemplate;
import com.ruoyi.cost.ai.prompt.dto.AiPromptTemplateSaveRequest;
import com.ruoyi.cost.ai.chat.dto.AiChatSendRequest;
import com.ruoyi.cost.ai.chat.dto.AiConversationCreateRequest;
import com.ruoyi.cost.ai.chat.dto.AiConversationUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class AiControllerPermissionTest
{
    @Test
    void modelEndpointsUseDedicatedPermissionsAndHideOperationBodies() throws Exception
    {
        assertPermission(AiModelConfigController.class.getMethod("list", AiModelConfig.class), "ai:model:list");
        assertPermission(AiModelConfigController.class.getMethod("getInfo", Long.class), "ai:model:query");
        Method add = AiModelConfigController.class.getMethod("add", AiModelConfigSaveRequest.class);
        Method edit = AiModelConfigController.class.getMethod("edit", AiModelConfigSaveRequest.class);
        assertPermission(add, "ai:model:add");
        assertPermission(edit, "ai:model:edit");
        assertPermission(AiModelConfigController.class.getMethod("remove", Long[].class), "ai:model:remove");
        assertPermission(AiModelConfigController.class.getMethod("test", Long.class), "ai:model:test");
        assertFalse(add.getAnnotation(Log.class).isSaveRequestData());
        assertFalse(edit.getAnnotation(Log.class).isSaveRequestData());
        assertFalse(add.getAnnotation(Log.class).isSaveResponseData());
        assertFalse(edit.getAnnotation(Log.class).isSaveResponseData());
    }

    @Test
    void promptAndLogEndpointsUseDedicatedPermissions() throws Exception
    {
        assertPermission(AiPromptTemplateController.class.getMethod("list", AiPromptTemplate.class), "ai:prompt:list");
        assertPermission(AiPromptTemplateController.class.getMethod("getInfo", Long.class), "ai:prompt:query");
        Method add = AiPromptTemplateController.class.getMethod("add", AiPromptTemplateSaveRequest.class);
        Method edit = AiPromptTemplateController.class.getMethod("edit", AiPromptTemplateSaveRequest.class);
        assertPermission(add, "ai:prompt:add");
        assertPermission(edit, "ai:prompt:edit");
        assertFalse(add.getAnnotation(Log.class).isSaveRequestData());
        assertFalse(edit.getAnnotation(Log.class).isSaveRequestData());
        assertPermission(AiPromptTemplateController.class.getMethod("remove", Long[].class), "ai:prompt:remove");
        assertPermission(AiRequestLogController.class.getMethod("list", AiRequestLog.class), "ai:log:list");
        assertPermission(AiRequestLogController.class.getMethod("getInfo", Long.class), "ai:log:query");
    }

    @Test
    void chatEndpointsUseOwnerScopedPermissionsAndDoNotLogMessageBodies() throws Exception
    {
        assertPermission(AiChatController.class.getMethod("list"), "ai:chat:list");
        assertPermission(AiChatController.class.getMethod("projectOptions", String.class), "ai:chat:list");
        assertPermission(AiChatController.class.getMethod("getInfo", Long.class), "ai:chat:list");
        assertPermission(AiChatController.class.getMethod("messages", Long.class), "ai:chat:list");
        Method add = AiChatController.class.getMethod("add", AiConversationCreateRequest.class);
        Method edit = AiChatController.class.getMethod("edit", AiConversationUpdateRequest.class);
        Method stream = AiChatController.class.getMethod("stream", Long.class, AiChatSendRequest.class);
        assertPermission(add, "ai:chat:use"); assertPermission(edit, "ai:chat:use");
        assertPermission(stream, "ai:chat:use");
        assertPermission(AiChatController.class.getMethod("remove", Long.class), "ai:chat:remove");
        assertFalse(add.getAnnotation(Log.class).isSaveRequestData());
        assertFalse(edit.getAnnotation(Log.class).isSaveRequestData());
        assertFalse(stream.getAnnotation(Log.class).isSaveRequestData());
        assertFalse(stream.getAnnotation(Log.class).isSaveResponseData());
    }

    private void assertPermission(Method method, String permission)
    {
        assertEquals("@ss.hasPermi('" + permission + "')",
                method.getAnnotation(PreAuthorize.class).value());
    }
}
