package com.ruoyi.cost.boq.preview.vo;

import java.util.ArrayList;
import java.util.List;

/** 预览列信息。 */
public class BoqPreviewColumnVo
{
    private int index;
    private String key;
    private String header;
    private List<String> sampleValues = new ArrayList<>();

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getHeader() { return header; }
    public void setHeader(String header) { this.header = header; }
    public List<String> getSampleValues() { return sampleValues; }
    public void setSampleValues(List<String> sampleValues) { this.sampleValues = sampleValues; }
}
