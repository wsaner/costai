package com.ruoyi.cost.boq.preview.support;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** 流式读取保留的有限 Sheet 样本。 */
public class SheetSample
{
    private final int index;
    private final String name;
    private final NavigableMap<Integer, Map<Integer, String>> rows = new TreeMap<>();
    private boolean mergedCells;

    public SheetSample(int index, String name)
    {
        this.index = index;
        this.name = name;
    }

    public int getIndex() { return index; }
    public String getName() { return name; }
    public NavigableMap<Integer, Map<Integer, String>> getRows() { return rows; }
    public boolean isMergedCells() { return mergedCells; }
    public void setMergedCells(boolean mergedCells) { this.mergedCells = mergedCells; }
}
