package com.ruoyi.cost.ai.tool;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyi.cost.ai.chat.mapper.AiChatMapper;

@Component
public class GetReviewIssuesTool implements AiTool<ProjectToolInput, List<Map<String, Object>>>
{
    private final AiChatMapper mapper;
    public GetReviewIssuesTool(AiChatMapper mapper) { this.mapper = mapper; }
    @Override public String getName() { return "getReviewIssues"; }
    @Override public List<Map<String, Object>> execute(ProjectToolInput input)
    {
        requireAuthorizedProject(input);
        return mapper.selectReviewIssues(input.projectId(), Math.min(Math.max(input.limit(), 1), 20));
    }

    private void requireAuthorizedProject(ProjectToolInput input)
    {
        if (input.project() == null || !input.project().getId().equals(input.projectId()))
            throw new IllegalArgumentException("工具调用缺少已授权的项目上下文");
    }
}
