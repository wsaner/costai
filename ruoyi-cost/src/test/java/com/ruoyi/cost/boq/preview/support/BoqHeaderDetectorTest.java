package com.ruoyi.cost.boq.preview.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ruoyi.cost.boq.preview.service.impl.RuleBasedBoqColumnMappingService;

class BoqHeaderDetectorTest
{
    private final BoqHeaderDetector detector = new BoqHeaderDetector(new RuleBasedBoqColumnMappingService());

    @Test
    void skipsTitleAndDetectsLaterHeaderRow()
    {
        SheetSample sheet = new SheetSample(0, "清单");
        sheet.getRows().put(0, Map.of(0, "分部分项工程量清单与计价表"));
        Map<Integer, String> header = new LinkedHashMap<>();
        header.put(0, "项目编码");
        header.put(1, "项目名称");
        header.put(2, "单位");
        header.put(3, "工程量");
        header.put(4, "综合单价");
        sheet.getRows().put(3, header);

        DetectedHeader detected = detector.detect(sheet);

        assertNotNull(detected);
        assertEquals(3, detected.rowIndex());
        assertEquals(5, detected.mappings().size());
    }

    @Test
    void rejectsUnrelatedSpreadsheet()
    {
        SheetSample sheet = new SheetSample(0, "其他");
        sheet.getRows().put(0, Map.of(0, "姓名", 1, "电话", 2, "地址"));
        assertNull(detector.detect(sheet));
    }
}
