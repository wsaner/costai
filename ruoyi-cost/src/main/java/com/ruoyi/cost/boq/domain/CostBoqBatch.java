package com.ruoyi.cost.boq.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/** 工程量清单导入批次。 */
public class CostBoqBatch extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long projectId;
    private String batchName;
    private String businessType;
    private Long sourceFileId;
    private String sourceFileName;
    private String sheetName;
    private Integer headerRow;
    private String fieldMappingJson;
    private String professionalType;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private BigDecimal totalAmount;
    private String importStatus;
    private String errorSummary;
    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public Long getSourceFileId() { return sourceFileId; }
    public void setSourceFileId(Long sourceFileId) { this.sourceFileId = sourceFileId; }
    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public Integer getHeaderRow() { return headerRow; }
    public void setHeaderRow(Integer headerRow) { this.headerRow = headerRow; }
    public String getFieldMappingJson() { return fieldMappingJson; }
    public void setFieldMappingJson(String fieldMappingJson) { this.fieldMappingJson = fieldMappingJson; }
    public String getProfessionalType() { return professionalType; }
    public void setProfessionalType(String professionalType) { this.professionalType = professionalType; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public Integer getSuccessCount() { return successCount; }
    public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getImportStatus() { return importStatus; }
    public void setImportStatus(String importStatus) { this.importStatus = importStatus; }
    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
