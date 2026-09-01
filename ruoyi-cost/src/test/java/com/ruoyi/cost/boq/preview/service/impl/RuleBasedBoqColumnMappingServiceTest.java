package com.ruoyi.cost.boq.preview.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RuleBasedBoqColumnMappingServiceTest
{
    private final RuleBasedBoqColumnMappingService service = new RuleBasedBoqColumnMappingService();

    @Test
    void mapsCommonAliasesAndHeadersWithUnitSuffixes()
    {
        Map<Integer, String> headers = new LinkedHashMap<>();
        headers.put(0, "清单编号");
        headers.put(1, "工程名称");
        headers.put(2, "计量单位");
        headers.put(3, "数量");
        headers.put(4, "综合单价（元）");
        headers.put(5, "总金额(元)");

        Map<String, Integer> result = service.suggest(headers);

        assertEquals(0, result.get("itemCode"));
        assertEquals(1, result.get("itemName"));
        assertEquals(2, result.get("unit"));
        assertEquals(3, result.get("quantity"));
        assertEquals(4, result.get("unitPrice"));
        assertEquals(5, result.get("totalPrice"));
    }

    @Test
    void doesNotAssignOneSourceColumnToTwoFields()
    {
        Map<String, Integer> result = service.suggest(Map.of(0, "金额"));
        assertEquals(1, result.size());
        assertFalse(result.containsKey("unitPrice"));
    }
}
