package com.ruoyi.cost.knowledge.parse;

import java.nio.file.Path;

public interface KnowledgeDocumentParser
{
    boolean supports(String extension);
    ParsedDocument parse(Path path) throws Exception;
}
