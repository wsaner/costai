package com.ruoyi.cost.boq.match.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.boq.match.support.BoqCompareCalculator;
import com.ruoyi.cost.boq.match.support.BoqMatchType;
import com.ruoyi.cost.boq.match.support.BoqTextNormalizer;

class BoqMatchEngineTest
{
    private final BoqMatchEngine engine = new BoqMatchEngine(
            new BoqTextNormalizer(), new BoqCompareCalculator());

    @Test
    void appliesCodeThenNormalizedNameThenFuzzyAndUnmatchedStrategies()
    {
        List<CostBoqItem> left = List.of(
                item(1L, "010101", "名称不同也按编码优先", "C30", "m³", 1),
                item(2L, null, " 现浇混凝土（墙） ", "C30", "平方米", 2),
                item(3L, null, "预拌C30混凝土浇筑", "泵送", "m3", 3),
                item(4L, null, "左侧孤立清单", null, "项", 4));
        List<CostBoqItem> right = List.of(
                item(11L, " ０１０１０１ ", "任意名称", "C25", "t", 1),
                item(12L, null, "现浇混凝土墙", "C30", "㎡", 2),
                item(13L, null, "预拌C30混凝土浇注", "泵送", "立方米", 3),
                item(14L, null, "右侧孤立项目", null, "项", 4));

        List<CostBoqCompare> rows = engine.match(7L, 8L, 9L, left, right,
                Collections.emptySet(), Collections.emptySet(), "admin");
        Map<Long, CostBoqCompare> byLeft = rows.stream().filter(row -> row.getLeftItemId() != null)
                .collect(Collectors.toMap(CostBoqCompare::getLeftItemId, Function.identity()));

        assertEquals(BoqMatchType.EXACT.name(), byLeft.get(1L).getMatchType());
        assertEquals(11L, byLeft.get(1L).getRightItemId());
        assertEquals(BoqMatchType.EXACT.name(), byLeft.get(2L).getMatchType());
        assertEquals(12L, byLeft.get(2L).getRightItemId());
        assertEquals(BoqMatchType.HIGH_SIMILARITY.name(), byLeft.get(3L).getMatchType());
        assertEquals(13L, byLeft.get(3L).getRightItemId());
        assertEquals(BoqMatchType.ONLY_LEFT.name(), byLeft.get(4L).getMatchType());
        assertTrue(rows.stream().anyMatch(row -> row.getLeftItemId() == null
                && Long.valueOf(14L).equals(row.getRightItemId())
                && BoqMatchType.ONLY_RIGHT.name().equals(row.getMatchType())));
    }

    @Test
    void reservedManualItemsAreNeverRematched()
    {
        List<CostBoqCompare> rows = engine.match(7L, 8L, 9L,
                List.of(item(1L, "A", "混凝土", null, "m3", 1),
                        item(2L, "B", "钢筋", null, "t", 2)),
                List.of(item(11L, "A", "混凝土", null, "m3", 1),
                        item(12L, "B", "钢筋", null, "t", 2)),
                Set.of(1L), Set.of(11L), "admin");

        assertEquals(1, rows.size());
        assertEquals(2L, rows.get(0).getLeftItemId());
        assertEquals(12L, rows.get(0).getRightItemId());
    }

    @Test
    void exactDuplicateCodesRemainOneToOne()
    {
        List<CostBoqCompare> rows = engine.match(7L, 8L, 9L,
                List.of(item(1L, "A", "混凝土墙", null, "m3", 1),
                        item(2L, "A", "混凝土梁", null, "m3", 2)),
                List.of(item(11L, "A", "混凝土梁", null, "m3", 1),
                        item(12L, "A", "混凝土墙", null, "m3", 2)),
                Collections.emptySet(), Collections.emptySet(), "admin");

        assertEquals(2, rows.size());
        assertEquals(2, rows.stream().map(CostBoqCompare::getRightItemId).distinct().count());
    }

    private CostBoqItem item(Long id, String code, String name, String feature, String unit, int row)
    {
        CostBoqItem item = new CostBoqItem();
        item.setId(id);
        item.setItemCode(code);
        item.setItemName(name);
        item.setItemFeature(feature);
        item.setUnit(unit);
        item.setSourceRow(row);
        return item;
    }
}
