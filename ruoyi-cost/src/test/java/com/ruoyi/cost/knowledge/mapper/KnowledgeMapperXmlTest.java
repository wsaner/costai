package com.ruoyi.cost.knowledge.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import java.util.List;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class KnowledgeMapperXmlTest
{
    @Test
    void mapperXmlParsesAndRegistersKnowledgeStatements() throws Exception
    {
        String resource = "mapper/cost/knowledge/KnowledgeMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream(resource))
        {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String prefix = KnowledgeMapper.class.getName() + ".";
        for (String method : List.of("selectBaseList","insertBase","selectDocumentList","insertDocument",
                "claimDocument","selectChunksByIds","insertChunks","markChunksSuccess","refreshBaseCounts"))
            assertTrue(configuration.hasStatement(prefix + method), prefix + method);
    }
}
