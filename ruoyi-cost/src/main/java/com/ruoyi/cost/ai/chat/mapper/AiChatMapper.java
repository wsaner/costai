package com.ruoyi.cost.ai.chat.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cost.ai.chat.domain.AiConversation;
import com.ruoyi.cost.ai.chat.domain.AiConversationMessage;

public interface AiChatMapper
{
    List<AiConversation> selectConversationList(@Param("userId") Long userId);
    AiConversation selectConversation(@Param("id") Long id, @Param("userId") Long userId);
    int insertConversation(AiConversation conversation);
    int updateConversation(AiConversation conversation);
    int claimGeneration(@Param("id") Long id, @Param("userId") Long userId);
    int releaseGeneration(@Param("id") Long id, @Param("userId") Long userId,
            @Param("messageIncrement") int messageIncrement, @Param("title") String title,
            @Param("operator") String operator);
    int deleteConversation(@Param("id") Long id, @Param("userId") Long userId, @Param("operator") String operator);
    int deleteMessages(@Param("conversationId") Long conversationId, @Param("userId") Long userId,
            @Param("operator") String operator);
    List<AiConversationMessage> selectMessages(@Param("conversationId") Long conversationId,
            @Param("userId") Long userId);
    List<AiConversationMessage> selectRecentMessages(@Param("conversationId") Long conversationId,
            @Param("userId") Long userId, @Param("limit") int limit);
    int insertMessage(AiConversationMessage message);
    int completeMessage(AiConversationMessage message);
    Map<String, Object> selectBoqSummary(@Param("projectId") Long projectId);
    Map<String, Object> selectReviewSummary(@Param("projectId") Long projectId);
    List<Map<String, Object>> selectReviewIssues(@Param("projectId") Long projectId, @Param("limit") int limit);
    List<Map<String, Object>> searchBoq(@Param("projectId") Long projectId, @Param("keyword") String keyword,
            @Param("limit") int limit);
}
