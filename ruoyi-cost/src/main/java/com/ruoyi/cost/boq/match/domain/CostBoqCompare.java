package com.ruoyi.cost.boq.match.domain;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.domain.BaseEntity;

/** 工程量清单对比结果。 */
public class CostBoqCompare extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private Long leftBatchId;
    private Long rightBatchId;
    private Long leftItemId;
    private Long rightItemId;
    private String matchType;
    private BigDecimal matchScore;
    private BigDecimal quantityDiff;
    private BigDecimal quantityDiffRate;
    private BigDecimal unitPriceDiff;
    private BigDecimal unitPriceDiffRate;
    private BigDecimal totalPriceDiff;
    private BigDecimal totalPriceDiffRate;
    private String keyword;

    private String leftItemCode;
    private String leftItemName;
    private String leftItemFeature;
    private String leftUnit;
    private BigDecimal leftQuantity;
    private BigDecimal leftUnitPrice;
    private BigDecimal leftTotalPrice;
    private String rightItemCode;
    private String rightItemName;
    private String rightItemFeature;
    private String rightUnit;
    private BigDecimal rightQuantity;
    private BigDecimal rightUnitPrice;
    private BigDecimal rightTotalPrice;

    @JsonIgnore
    private String delFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getLeftBatchId() { return leftBatchId; }
    public void setLeftBatchId(Long leftBatchId) { this.leftBatchId = leftBatchId; }
    public Long getRightBatchId() { return rightBatchId; }
    public void setRightBatchId(Long rightBatchId) { this.rightBatchId = rightBatchId; }
    public Long getLeftItemId() { return leftItemId; }
    public void setLeftItemId(Long leftItemId) { this.leftItemId = leftItemId; }
    public Long getRightItemId() { return rightItemId; }
    public void setRightItemId(Long rightItemId) { this.rightItemId = rightItemId; }
    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }
    public BigDecimal getMatchScore() { return matchScore; }
    public void setMatchScore(BigDecimal matchScore) { this.matchScore = matchScore; }
    public BigDecimal getQuantityDiff() { return quantityDiff; }
    public void setQuantityDiff(BigDecimal quantityDiff) { this.quantityDiff = quantityDiff; }
    public BigDecimal getQuantityDiffRate() { return quantityDiffRate; }
    public void setQuantityDiffRate(BigDecimal quantityDiffRate) { this.quantityDiffRate = quantityDiffRate; }
    public BigDecimal getUnitPriceDiff() { return unitPriceDiff; }
    public void setUnitPriceDiff(BigDecimal unitPriceDiff) { this.unitPriceDiff = unitPriceDiff; }
    public BigDecimal getUnitPriceDiffRate() { return unitPriceDiffRate; }
    public void setUnitPriceDiffRate(BigDecimal unitPriceDiffRate) { this.unitPriceDiffRate = unitPriceDiffRate; }
    public BigDecimal getTotalPriceDiff() { return totalPriceDiff; }
    public void setTotalPriceDiff(BigDecimal totalPriceDiff) { this.totalPriceDiff = totalPriceDiff; }
    public BigDecimal getTotalPriceDiffRate() { return totalPriceDiffRate; }
    public void setTotalPriceDiffRate(BigDecimal totalPriceDiffRate) { this.totalPriceDiffRate = totalPriceDiffRate; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getLeftItemCode() { return leftItemCode; }
    public void setLeftItemCode(String leftItemCode) { this.leftItemCode = leftItemCode; }
    public String getLeftItemName() { return leftItemName; }
    public void setLeftItemName(String leftItemName) { this.leftItemName = leftItemName; }
    public String getLeftItemFeature() { return leftItemFeature; }
    public void setLeftItemFeature(String leftItemFeature) { this.leftItemFeature = leftItemFeature; }
    public String getLeftUnit() { return leftUnit; }
    public void setLeftUnit(String leftUnit) { this.leftUnit = leftUnit; }
    public BigDecimal getLeftQuantity() { return leftQuantity; }
    public void setLeftQuantity(BigDecimal leftQuantity) { this.leftQuantity = leftQuantity; }
    public BigDecimal getLeftUnitPrice() { return leftUnitPrice; }
    public void setLeftUnitPrice(BigDecimal leftUnitPrice) { this.leftUnitPrice = leftUnitPrice; }
    public BigDecimal getLeftTotalPrice() { return leftTotalPrice; }
    public void setLeftTotalPrice(BigDecimal leftTotalPrice) { this.leftTotalPrice = leftTotalPrice; }
    public String getRightItemCode() { return rightItemCode; }
    public void setRightItemCode(String rightItemCode) { this.rightItemCode = rightItemCode; }
    public String getRightItemName() { return rightItemName; }
    public void setRightItemName(String rightItemName) { this.rightItemName = rightItemName; }
    public String getRightItemFeature() { return rightItemFeature; }
    public void setRightItemFeature(String rightItemFeature) { this.rightItemFeature = rightItemFeature; }
    public String getRightUnit() { return rightUnit; }
    public void setRightUnit(String rightUnit) { this.rightUnit = rightUnit; }
    public BigDecimal getRightQuantity() { return rightQuantity; }
    public void setRightQuantity(BigDecimal rightQuantity) { this.rightQuantity = rightQuantity; }
    public BigDecimal getRightUnitPrice() { return rightUnitPrice; }
    public void setRightUnitPrice(BigDecimal rightUnitPrice) { this.rightUnitPrice = rightUnitPrice; }
    public BigDecimal getRightTotalPrice() { return rightTotalPrice; }
    public void setRightTotalPrice(BigDecimal rightTotalPrice) { this.rightTotalPrice = rightTotalPrice; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
