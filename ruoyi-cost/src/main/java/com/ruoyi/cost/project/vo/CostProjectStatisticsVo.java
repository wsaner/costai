package com.ruoyi.cost.project.vo;

import java.math.BigDecimal;

/** 项目概览统计。 */
public class CostProjectStatisticsVo
{
    private Long projectCount = 0L;
    private BigDecimal submittedAmount = BigDecimal.ZERO;
    private BigDecimal approvedAmount = BigDecimal.ZERO;
    private BigDecimal reductionAmount = BigDecimal.ZERO;
    private BigDecimal averageReductionRate = BigDecimal.ZERO;

    public Long getProjectCount()
    {
        return projectCount;
    }

    public void setProjectCount(Long projectCount)
    {
        this.projectCount = projectCount;
    }

    public BigDecimal getSubmittedAmount()
    {
        return submittedAmount;
    }

    public void setSubmittedAmount(BigDecimal submittedAmount)
    {
        this.submittedAmount = submittedAmount;
    }

    public BigDecimal getApprovedAmount()
    {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount)
    {
        this.approvedAmount = approvedAmount;
    }

    public BigDecimal getReductionAmount()
    {
        return reductionAmount;
    }

    public void setReductionAmount(BigDecimal reductionAmount)
    {
        this.reductionAmount = reductionAmount;
    }

    public BigDecimal getAverageReductionRate()
    {
        return averageReductionRate;
    }

    public void setAverageReductionRate(BigDecimal averageReductionRate)
    {
        this.averageReductionRate = averageReductionRate;
    }
}
