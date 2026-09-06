package com.ruoyi.cost.ai.chat.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import java.util.List;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class AiChatMapperXmlTest
{
    @Test
    void mapperXmlParsesAndRegistersAllChatStatements() throws Exception
    {
        String resource = "mapper/cost/ai/AiChatMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream(resource))
        {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String prefix = AiChatMapper.class.getName() + ".";
        for (String method : List.of("selectConversationList", "selectConversation", "insertConversation",
                "updateConversation", "claimGeneration", "releaseGeneration", "deleteConversation",
                "deleteMessages", "selectMessages", "selectRecentMessages", "insertMessage", "completeMessage",
                "selectBoqSummary", "selectReviewSummary", "selectReviewIssues", "searchBoq"))
            assertTrue(configuration.hasStatement(prefix + method), prefix + method);
    }
}
