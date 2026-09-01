package com.ruoyi.cost.boq.preview.service;

import java.util.Map;

/** 工程量清单列映射服务；后续 AI 映射实现可在此接口后扩展。 */
public interface BoqColumnMappingService
{
    /** 返回“标准字段编码 -> 零基列号”的确定性建议。 */
    Map<String, Integer> suggest(Map<Integer, String> headers);
}
