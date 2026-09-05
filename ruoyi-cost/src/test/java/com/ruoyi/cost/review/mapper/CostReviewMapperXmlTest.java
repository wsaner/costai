package com.ruoyi.cost.review.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import java.util.List;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class CostReviewMapperXmlTest
{
    @Test
    void allReviewMapperXmlFilesParseAndRegisterStatements() throws Exception
    {
        assertStatements("mapper/cost/review/CostReviewRuleConfigMapper.xml",
                CostReviewRuleConfigMapper.class, "selectConfigList", "selectConfigById",
                "updateConfigValue");
        assertStatements("mapper/cost/review/CostReviewTaskMapper.xml",
                CostReviewTaskMapper.class, "insertTask", "finishSuccess", "finishFailed",
                "refreshStatistics", "selectTaskList", "selectTaskById", "deleteByBoqBatchId");
        assertStatements("mapper/cost/review/CostReviewIssueMapper.xml",
                CostReviewIssueMapper.class, "batchInsert", "selectIssueList", "selectIssueById",
                "updateIssueHandle", "updateAiAnalysis", "deleteByBoqBatchId");
    }

    private void assertStatements(String resource, Class<?> mapper, String... methods) throws Exception
    {
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream(resource))
        {
            new XMLMapperBuilder(input, configuration, resource,
                    configuration.getSqlFragments()).parse();
        }
        String namespace = mapper.getName() + ".";
        for (String method : List.of(methods))
            assertTrue(configuration.hasStatement(namespace + method), namespace + method);
    }
}
