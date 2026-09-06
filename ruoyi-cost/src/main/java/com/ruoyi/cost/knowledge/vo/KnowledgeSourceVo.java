package com.ruoyi.cost.knowledge.vo;

public record KnowledgeSourceVo(Long chunkId, String documentName, Integer pageNumber,
        String sectionTitle, double score, String quote)
{
}
