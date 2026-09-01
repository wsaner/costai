package com.ruoyi.cost.boq.preview.dto;

import jakarta.validation.constraints.Size;

/** 重新预览指定 Sheet。 */
public class BoqPreviewRequest
{
    private String sheetName;

    @Size(max = 100, message = "Sheet名称长度不能超过100个字符")
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
}
