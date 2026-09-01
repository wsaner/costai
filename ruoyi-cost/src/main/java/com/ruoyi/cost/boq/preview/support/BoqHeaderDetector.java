package com.ruoyi.cost.boq.preview.support;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.boq.preview.service.BoqColumnMappingService;

/** 在前50个物理行中选择最可能的清单表头。 */
@Component
public class BoqHeaderDetector
{
    private static final int MAX_HEADER_ROW = 50;
    private final BoqColumnMappingService mappingService;

    public BoqHeaderDetector(BoqColumnMappingService mappingService)
    {
        this.mappingService = mappingService;
    }

    public DetectedHeader detect(SheetSample sheet)
    {
        DetectedHeader best = null;
        for (Map.Entry<Integer, Map<Integer, String>> row : sheet.getRows().entrySet())
        {
            if (row.getKey() >= MAX_HEADER_ROW)
            {
                break;
            }
            Map<Integer, String> headers = cleanHeaders(row.getValue());
            Map<String, Integer> mappings = mappingService.suggest(headers);
            boolean hasName = mappings.containsKey("itemName");
            boolean hasBusinessValue = mappings.containsKey("itemCode")
                    || mappings.containsKey("quantity") || mappings.containsKey("unitPrice")
                    || mappings.containsKey("totalPrice");
            if (mappings.size() < 3 || !hasName || !hasBusinessValue)
            {
                continue;
            }
            int score = mappings.size() * 100 - row.getKey();
            if (best == null || score > best.score())
            {
                best = new DetectedHeader(row.getKey(), headers, mappings, score);
            }
        }
        return best;
    }

    private Map<Integer, String> cleanHeaders(Map<Integer, String> source)
    {
        Map<Integer, String> result = new LinkedHashMap<>();
        source.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String header = StringUtils.trim(entry.getValue());
            if (StringUtils.isNotBlank(header))
            {
                result.put(entry.getKey(), header);
            }
        });
        return result;
    }
}
