package com.ruoyi.cost.boq.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 用户确认映射后的正式导入请求。 */
public class CostBoqImportRequest
{
    @NotNull(message = "项目文件ID不能为空")
    private Long projectFileId;
    @NotBlank(message = "批次名称不能为空")
    @Size(max = 100, message = "批次名称不能超过100个字符")
    private String batchName;
    @NotBlank(message = "业务类型不能为空")
    private String businessType;
    @NotBlank(message = "Sheet不能为空")
    @Size(max = 100, message = "Sheet名称不能超过100个字符")
    private String sheetName;
    @NotNull(message = "表头行不能为空")
    @Min(value = 1, message = "表头行必须大于0")
    @Max(value = 1000, message = "表头行不能超过1000")
    private Integer headerRow;
    @NotEmpty(message = "字段映射不能为空")
    private Map<String, String> columnMappings = new LinkedHashMap<>();
    @Size(max = 32, message = "专业类型不能超过32个字符")
    private String professionalType;

    public Long getProjectFileId() { return projectFileId; }
    public void setProjectFileId(Long projectFileId) { this.projectFileId = projectFileId; }
    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public Integer getHeaderRow() { return headerRow; }
    public void setHeaderRow(Integer headerRow) { this.headerRow = headerRow; }
    public Map<String, String> getColumnMappings() { return columnMappings; }
    public void setColumnMappings(Map<String, String> columnMappings) { this.columnMappings = columnMappings; }
    public String getProfessionalType() { return professionalType; }
    public void setProfessionalType(String professionalType) { this.professionalType = professionalType; }
}
