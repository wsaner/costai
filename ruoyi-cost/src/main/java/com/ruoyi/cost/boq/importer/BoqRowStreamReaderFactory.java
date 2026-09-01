package com.ruoyi.cost.boq.importer;

import java.util.List;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;

/** 按扩展名选择全量流读取器。 */
@Component
public class BoqRowStreamReaderFactory
{
    private final List<BoqRowStreamReader> readers;

    public BoqRowStreamReaderFactory(List<BoqRowStreamReader> readers)
    {
        this.readers = readers;
    }

    public BoqRowStreamReader getReader(String extension)
    {
        return readers.stream().filter(reader -> reader.supports(extension)).findFirst()
                .orElseThrow(() -> new ServiceException("正式导入仅支持 xlsx、xls、csv 格式"));
    }
}
