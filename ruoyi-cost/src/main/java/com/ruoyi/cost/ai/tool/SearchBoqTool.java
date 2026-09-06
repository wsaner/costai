package com.ruoyi.cost.ai.tool;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyi.cost.ai.chat.mapper.AiChatMapper;

@Component
public class SearchBoqTool implements AiTool<ProjectToolInput, List<Map<String, Object>>>
{
    private final AiChatMapper mapper;
    public SearchBoqTool(AiChatMapper mapper) { this.mapper = mapper; }
    @Override public String getName() { return "searchBoq"; }
    @Override public List<Map<String, Object>> execute(ProjectToolInput input)
    {
        requireAuthorizedProject(input);
        if (input.keyword() == null || input.keyword().isBlank()) return Collections.emptyList();
        String keyword = input.keyword().trim();
        if (keyword.length() > 80) keyword = keyword.substring(0, 80);
        return mapper.searchBoq(input.projectId(), keyword, Math.min(Math.max(input.limit(), 1), 20));
    }
    private void requireAuthorizedProject(ProjectToolInput input)
    {
        if (input.project() == null || !input.project().getId().equals(input.projectId()))
            throw new IllegalArgumentException("工具调用缺少已授权的项目上下文");
    }
}
