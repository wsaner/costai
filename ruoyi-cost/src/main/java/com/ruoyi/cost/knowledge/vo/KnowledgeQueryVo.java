package com.ruoyi.cost.knowledge.vo;

import java.util.List;
import com.ruoyi.cost.ai.model.protocol.AiTokenUsage;

public record KnowledgeQueryVo(String answer, List<KnowledgeSourceVo> sources,
        AiTokenUsage tokenUsage)
{
}
