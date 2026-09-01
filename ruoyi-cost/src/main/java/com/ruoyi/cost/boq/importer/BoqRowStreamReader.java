package com.ruoyi.cost.boq.importer;

import java.nio.file.Path;

/** 不在内存保留整本工作簿的清单行读取器。 */
public interface BoqRowStreamReader
{
    boolean supports(String extension);
    void stream(Path path, String sheetName, int headerRow, BoqRowConsumer consumer) throws Exception;
}
