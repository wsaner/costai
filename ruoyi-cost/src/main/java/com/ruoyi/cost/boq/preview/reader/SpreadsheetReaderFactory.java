package com.ruoyi.cost.boq.preview.reader;

import java.util.List;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;

/** 按扩展名选择流式读取实现。 */
@Component
public class SpreadsheetReaderFactory
{
    private final List<StreamingWorkbookReader> readers;

    public SpreadsheetReaderFactory(List<StreamingWorkbookReader> readers)
    {
        this.readers = readers;
    }

    public StreamingWorkbookReader getReader(String extension)
    {
        return readers.stream().filter(reader -> reader.supports(extension)).findFirst()
                .orElseThrow(() -> new ServiceException("仅支持 xlsx、xls、csv 格式的清单预览"));
    }
}
