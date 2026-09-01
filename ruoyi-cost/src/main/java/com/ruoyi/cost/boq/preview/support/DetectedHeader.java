package com.ruoyi.cost.boq.preview.support;

import java.util.Map;

/** Sheet表头检测结果。 */
public record DetectedHeader(int rowIndex, Map<Integer, String> headers,
        Map<String, Integer> mappings, int score) { }
