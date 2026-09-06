package com.ruoyi.cost.knowledge.parse;

import java.util.List;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;

@Component
public class DocumentParserRegistry
{
    private final List<KnowledgeDocumentParser> parsers;
    public DocumentParserRegistry(List<KnowledgeDocumentParser> parsers) { this.parsers = parsers; }

    public KnowledgeDocumentParser require(String extension)
    {
        return parsers.stream().filter(parser -> parser.supports(extension)).findFirst()
                .orElseThrow(() -> new ServiceException("知识库暂不支持该文件格式，仅支持PDF、DOCX、TXT"));
    }
}
