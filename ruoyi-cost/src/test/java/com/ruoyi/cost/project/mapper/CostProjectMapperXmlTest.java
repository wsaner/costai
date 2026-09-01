package com.ruoyi.cost.project.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class CostProjectMapperXmlTest
{
    @Test
    void mapperXmlParsesAndRegistersAllCoreStatements() throws Exception
    {
        String resource = "mapper/cost/project/CostProjectMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream(resource))
        {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }

        String namespace = CostProjectMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "selectCostProjectList"));
        assertTrue(configuration.hasStatement(namespace + "insertCostProject"));
        assertTrue(configuration.hasStatement(namespace + "updateCostProject"));
        assertTrue(configuration.hasStatement(namespace + "deleteCostProjectByIds"));
        assertTrue(configuration.hasStatement(namespace + "selectProjectStatistics"));
    }
}
