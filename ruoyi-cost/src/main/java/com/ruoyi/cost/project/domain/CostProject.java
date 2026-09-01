package com.ruoyi.cost.project.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 造价项目对象 cost_project
 *
 * @author CostAI
 */
public class CostProject extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 项目主键 */
    private Long id;

    /** 项目编号 */
    @Excel(name = "项目编号")
    private String projectCode;

    /** 项目名称 */
    @Excel(name = "项目名称")
    private String projectName;

    /** 项目类型 */
    @Excel(name = "项目类型")
    private String projectType;

    /** 项目专业 */
    @Excel(name = "项目专业")
    private String professionalType;

    /** 项目阶段 */
    @Excel(name = "项目阶段")
    private String projectStage;

    /** 建设单位 */
    private String constructionUnit;

    /** 施工单位 */
    private String contractorUnit;

    /** 咨询单位 */
    private String consultingUnit;

    /** 省 */
    private String province;

    /** 市 */
    private String city;

    /** 区县 */
    private String district;

    /** 建筑面积 */
    private BigDecimal buildingArea;

    /** 项目负责人用户ID */
    private Long projectManagerId;

    /** 项目负责人名称快照 */
    @Excel(name = "项目负责人")
    private String projectManagerName;

    /** 归属部门ID，用于现有数据权限 */
    private Long ownerDeptId;

    /** 送审金额 */
    @Excel(name = "送审金额", scale = 2)
    private BigDecimal submittedAmount;

    /** 审定金额 */
    @Excel(name = "审定金额", scale = 2)
    private BigDecimal approvedAmount;

    /** 核增金额 */
    @Excel(name = "核增金额", scale = 2)
    private BigDecimal increaseAmount;

    /** 核减金额 */
    @Excel(name = "核减金额", scale = 2)
    private BigDecimal reductionAmount;

    /** 核减率，按小数存储，例如0.125表示12.5% */
    @Excel(name = "核减率")
    private BigDecimal reductionRate;

    /** 开工日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    /** 竣工日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date completionDate;

    /** 项目状态 */
    @Excel(name = "项目状态")
    private String projectStatus;

    /** 项目描述 */
    private String description;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    @NotBlank(message = "项目编号不能为空")
    @Size(max = 64, message = "项目编号长度不能超过64个字符")
    public String getProjectCode()
    {
        return projectCode;
    }

    public void setProjectCode(String projectCode)
    {
        this.projectCode = projectCode;
    }

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 200, message = "项目名称长度不能超过200个字符")
    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    @NotBlank(message = "项目类型不能为空")
    public String getProjectType()
    {
        return projectType;
    }

    public void setProjectType(String projectType)
    {
        this.projectType = projectType;
    }

    @NotBlank(message = "项目专业不能为空")
    public String getProfessionalType()
    {
        return professionalType;
    }

    public void setProfessionalType(String professionalType)
    {
        this.professionalType = professionalType;
    }

    @NotBlank(message = "项目阶段不能为空")
    public String getProjectStage()
    {
        return projectStage;
    }

    public void setProjectStage(String projectStage)
    {
        this.projectStage = projectStage;
    }

    @Size(max = 200, message = "建设单位长度不能超过200个字符")
    public String getConstructionUnit()
    {
        return constructionUnit;
    }

    public void setConstructionUnit(String constructionUnit)
    {
        this.constructionUnit = constructionUnit;
    }

    @Size(max = 200, message = "施工单位长度不能超过200个字符")
    public String getContractorUnit()
    {
        return contractorUnit;
    }

    public void setContractorUnit(String contractorUnit)
    {
        this.contractorUnit = contractorUnit;
    }

    @Size(max = 200, message = "咨询单位长度不能超过200个字符")
    public String getConsultingUnit()
    {
        return consultingUnit;
    }

    public void setConsultingUnit(String consultingUnit)
    {
        this.consultingUnit = consultingUnit;
    }

    public String getProvince()
    {
        return province;
    }

    public void setProvince(String province)
    {
        this.province = province;
    }

    public String getCity()
    {
        return city;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    public String getDistrict()
    {
        return district;
    }

    public void setDistrict(String district)
    {
        this.district = district;
    }

    @DecimalMin(value = "0", message = "建筑面积不能小于0")
    public BigDecimal getBuildingArea()
    {
        return buildingArea;
    }

    public void setBuildingArea(BigDecimal buildingArea)
    {
        this.buildingArea = buildingArea;
    }

    @NotNull(message = "项目负责人不能为空")
    public Long getProjectManagerId()
    {
        return projectManagerId;
    }

    public void setProjectManagerId(Long projectManagerId)
    {
        this.projectManagerId = projectManagerId;
    }

    public String getProjectManagerName()
    {
        return projectManagerName;
    }

    public void setProjectManagerName(String projectManagerName)
    {
        this.projectManagerName = projectManagerName;
    }

    public Long getOwnerDeptId()
    {
        return ownerDeptId;
    }

    public void setOwnerDeptId(Long ownerDeptId)
    {
        this.ownerDeptId = ownerDeptId;
    }

    @DecimalMin(value = "0", message = "送审金额不能小于0")
    public BigDecimal getSubmittedAmount()
    {
        return submittedAmount;
    }

    public void setSubmittedAmount(BigDecimal submittedAmount)
    {
        this.submittedAmount = submittedAmount;
    }

    @DecimalMin(value = "0", message = "审定金额不能小于0")
    public BigDecimal getApprovedAmount()
    {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount)
    {
        this.approvedAmount = approvedAmount;
    }

    @DecimalMin(value = "0", message = "核增金额不能小于0")
    public BigDecimal getIncreaseAmount()
    {
        return increaseAmount;
    }

    public void setIncreaseAmount(BigDecimal increaseAmount)
    {
        this.increaseAmount = increaseAmount;
    }

    @DecimalMin(value = "0", message = "核减金额不能小于0")
    public BigDecimal getReductionAmount()
    {
        return reductionAmount;
    }

    public void setReductionAmount(BigDecimal reductionAmount)
    {
        this.reductionAmount = reductionAmount;
    }

    public BigDecimal getReductionRate()
    {
        return reductionRate;
    }

    public void setReductionRate(BigDecimal reductionRate)
    {
        this.reductionRate = reductionRate;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    public Date getCompletionDate()
    {
        return completionDate;
    }

    public void setCompletionDate(Date completionDate)
    {
        this.completionDate = completionDate;
    }

    @NotBlank(message = "项目状态不能为空")
    public String getProjectStatus()
    {
        return projectStatus;
    }

    public void setProjectStatus(String projectStatus)
    {
        this.projectStatus = projectStatus;
    }

    @Size(max = 2000, message = "项目描述长度不能超过2000个字符")
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("projectCode", getProjectCode())
                .append("projectName", getProjectName())
                .append("projectType", getProjectType())
                .append("professionalType", getProfessionalType())
                .append("projectStage", getProjectStage())
                .append("projectManagerId", getProjectManagerId())
                .append("submittedAmount", getSubmittedAmount())
                .append("approvedAmount", getApprovedAmount())
                .append("reductionAmount", getReductionAmount())
                .append("reductionRate", getReductionRate())
                .append("projectStatus", getProjectStatus())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .toString();
    }
}
