package com.ruoyi.cost.boq.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class CostBoqMapperXmlTest
{
    @Test
    void batchMapperXmlParsesAndRegistersStatements() throws Exception
    {
        Configuration configuration = parse("mapper/cost/boq/CostBoqBatchMapper.xml");
        String namespace = CostBoqBatchMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "selectBatchList"));
        assertTrue(configuration.hasStatement(namespace + "selectBatchById"));
        assertTrue(configuration.hasStatement(namespace + "insertBatch"));
        assertTrue(configuration.hasStatement(namespace + "updateImportResult"));
        assertTrue(configuration.hasStatement(namespace + "deleteBatch"));
    }

    @Test
    void itemMapperXmlParsesAndRegistersStatements() throws Exception
    {
        Configuration configuration = parse("mapper/cost/boq/CostBoqItemMapper.xml");
        String namespace = CostBoqItemMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "selectItemList"));
        assertTrue(configuration.hasStatement(namespace + "batchInsert"));
        assertTrue(configuration.hasStatement(namespace + "deleteByBatchId"));
    }

    @Test
    void errorMapperXmlParsesAndRegistersStatements() throws Exception
    {
        Configuration configuration = parse("mapper/cost/boq/CostBoqImportErrorMapper.xml");
        String namespace = CostBoqImportErrorMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "selectByBatchId"));
        assertTrue(configuration.hasStatement(namespace + "batchInsert"));
        assertTrue(configuration.hasStatement(namespace + "deleteByBatchId"));
    }

    private Configuration parse(String resource) throws Exception
    {
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream(resource))
        {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }
}
