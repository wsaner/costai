package com.ruoyi.cost.boq.preview.support;

import java.util.ArrayList;
import java.util.List;

/** 工作簿有限样本，不持有完整 Workbook。 */
public class WorkbookSample
{
    private final List<SheetSample> sheets = new ArrayList<>();

    public List<SheetSample> getSheets() { return sheets; }
}
