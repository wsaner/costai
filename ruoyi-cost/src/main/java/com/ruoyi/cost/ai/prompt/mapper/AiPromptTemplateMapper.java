package com.ruoyi.cost.ai.prompt.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cost.ai.prompt.domain.AiPromptTemplate;

public interface AiPromptTemplateMapper
{
    List<AiPromptTemplate> selectPromptList(AiPromptTemplate query);
    AiPromptTemplate selectPromptById(@Param("id") Long id);
    AiPromptTemplate selectActivePrompt(@Param("promptCode") String promptCode);
    int countCodeVersion(@Param("promptCode") String promptCode,
            @Param("version") Integer version, @Param("excludeId") Long excludeId);
    int insertPrompt(AiPromptTemplate prompt);
    int updatePrompt(AiPromptTemplate prompt);
    int disableOtherVersions(@Param("promptCode") String promptCode,
            @Param("excludeId") Long excludeId, @Param("updateBy") String updateBy);
    int deletePrompt(@Param("id") Long id, @Param("updateBy") String updateBy);
}
