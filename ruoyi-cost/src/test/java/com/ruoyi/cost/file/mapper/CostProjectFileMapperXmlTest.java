package com.ruoyi.cost.file.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class CostProjectFileMapperXmlTest
{
    @Test
    void mapperXmlParsesAndRegistersAllStatements() throws Exception
    {
        String resource = "mapper/cost/file/CostProjectFileMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream(resource))
        {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String namespace = CostProjectFileMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "selectByProjectId"));
        assertTrue(configuration.hasStatement(namespace + "selectById"));
        assertTrue(configuration.hasStatement(namespace + "countByProjectId"));
        assertTrue(configuration.hasStatement(namespace + "insertCostProjectFile"));
        assertTrue(configuration.hasStatement(namespace + "updateFileCategory"));
        assertTrue(configuration.hasStatement(namespace + "deleteCostProjectFile"));
    }
}
