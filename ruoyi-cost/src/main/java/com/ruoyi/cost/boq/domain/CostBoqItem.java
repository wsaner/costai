package com.ruoyi.cost.boq.domain;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.domain.BaseEntity;

/** 工程量清单明细。 */
public class CostBoqItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long projectId;
    private Long batchId;
    private String sequenceNo;
    private String itemCode;
    private String itemName;
    private String itemFeature;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private BigDecimal calculatedTotalPrice;
    private BigDecimal laborPrice;
    private BigDecimal materialPrice;
    private BigDecimal machinePrice;
    private BigDecimal managementFee;
    private BigDecimal profit;
    private BigDecimal tax;
    private String professionalType;
    private String category;
    private Long parentId;
    private Integer level;
    private String sourceSheet;
    private Integer sourceRow;
    @JsonIgnore
    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(String sequenceNo) { this.sequenceNo = sequenceNo; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getItemFeature() { return itemFeature; }
    public void setItemFeature(String itemFeature) { this.itemFeature = itemFeature; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public BigDecimal getCalculatedTotalPrice() { return calculatedTotalPrice; }
    public void setCalculatedTotalPrice(BigDecimal calculatedTotalPrice) { this.calculatedTotalPrice = calculatedTotalPrice; }
    public BigDecimal getLaborPrice() { return laborPrice; }
    public void setLaborPrice(BigDecimal laborPrice) { this.laborPrice = laborPrice; }
    public BigDecimal getMaterialPrice() { return materialPrice; }
    public void setMaterialPrice(BigDecimal materialPrice) { this.materialPrice = materialPrice; }
    public BigDecimal getMachinePrice() { return machinePrice; }
    public void setMachinePrice(BigDecimal machinePrice) { this.machinePrice = machinePrice; }
    public BigDecimal getManagementFee() { return managementFee; }
    public void setManagementFee(BigDecimal managementFee) { this.managementFee = managementFee; }
    public BigDecimal getProfit() { return profit; }
    public void setProfit(BigDecimal profit) { this.profit = profit; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    public String getProfessionalType() { return professionalType; }
    public void setProfessionalType(String professionalType) { this.professionalType = professionalType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getSourceSheet() { return sourceSheet; }
    public void setSourceSheet(String sourceSheet) { this.sourceSheet = sourceSheet; }
    public Integer getSourceRow() { return sourceRow; }
    public void setSourceRow(Integer sourceRow) { this.sourceRow = sourceRow; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
