package com.ruoyi.cost.ai.chat.service;

import java.util.List;
import java.util.function.Consumer;
import com.ruoyi.cost.ai.chat.domain.AiConversation;
import com.ruoyi.cost.ai.chat.domain.AiConversationMessage;
import com.ruoyi.cost.ai.chat.dto.AiChatSendRequest;
import com.ruoyi.cost.ai.chat.dto.AiConversationCreateRequest;
import com.ruoyi.cost.ai.chat.dto.AiConversationUpdateRequest;
import com.ruoyi.cost.ai.chat.vo.AiChatProjectOptionVo;

public interface AiChatService
{
    List<AiConversation> selectConversations(Long userId);
    AiConversation selectConversation(Long id, Long userId);
    List<AiConversationMessage> selectMessages(Long conversationId, Long userId);
    List<AiChatProjectOptionVo> selectProjectOptions(String keyword);
    Long create(AiConversationCreateRequest request, Long userId, String username);
    int update(AiConversationUpdateRequest request, Long userId, String username);
    int delete(Long id, Long userId, String username);
    void stream(Long conversationId, AiChatSendRequest request, Long userId, String username,
            Consumer<AiChatEvent> eventConsumer);
}
