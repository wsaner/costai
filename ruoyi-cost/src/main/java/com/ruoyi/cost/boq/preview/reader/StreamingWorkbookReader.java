package com.ruoyi.cost.boq.preview.reader;

import java.nio.file.Path;
import com.ruoyi.cost.boq.preview.support.WorkbookSample;

/** 有界内存工作簿样本读取器。 */
public interface StreamingWorkbookReader
{
    boolean supports(String extension);

    WorkbookSample read(Path path) throws Exception;
}
