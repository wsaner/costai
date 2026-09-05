package com.ruoyi.cost.boq.match.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class CostBoqCompareMapperXmlTest
{
    @Test
    void mapperXmlParsesAndRegistersAllComparisonStatements() throws Exception
    {
        String resource = "mapper/cost/boq/match/CostBoqCompareMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream(resource))
        {
            new XMLMapperBuilder(input, configuration, resource,
                    configuration.getSqlFragments()).parse();
        }
        String namespace = CostBoqCompareMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "selectCompareList"));
        assertTrue(configuration.hasStatement(namespace + "selectPairRows"));
        assertTrue(configuration.hasStatement(namespace + "selectSummary"));
        assertTrue(configuration.hasStatement(namespace + "batchInsert"));
        assertTrue(configuration.hasStatement(namespace + "deleteNonManualByPair"));
        assertTrue(configuration.hasStatement(namespace + "deleteByItemReferences"));
        assertTrue(configuration.hasStatement(namespace + "deleteByBatchId"));
    }
}
