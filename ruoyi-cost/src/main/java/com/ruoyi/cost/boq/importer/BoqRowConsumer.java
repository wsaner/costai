package com.ruoyi.cost.boq.importer;

import java.util.Map;

/** 全量导入逐行回调，sourceRow 为1基Excel行号。 */
@FunctionalInterface
public interface BoqRowConsumer
{
    void accept(int sourceRow, Map<Integer, String> values);
}
