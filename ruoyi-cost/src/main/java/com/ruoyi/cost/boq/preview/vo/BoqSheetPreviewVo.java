package com.ruoyi.cost.boq.preview.vo;

/** Sheet扫描摘要。 */
public class BoqSheetPreviewVo
{
    private int index;
    private String name;
    private Integer detectedHeaderRow;
    private int sampledRowCount;
    private int recognizedFieldCount;

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDetectedHeaderRow() { return detectedHeaderRow; }
    public void setDetectedHeaderRow(Integer detectedHeaderRow) { this.detectedHeaderRow = detectedHeaderRow; }
    public int getSampledRowCount() { return sampledRowCount; }
    public void setSampledRowCount(int sampledRowCount) { this.sampledRowCount = sampledRowCount; }
    public int getRecognizedFieldCount() { return recognizedFieldCount; }
    public void setRecognizedFieldCount(int recognizedFieldCount) { this.recognizedFieldCount = recognizedFieldCount; }
}
