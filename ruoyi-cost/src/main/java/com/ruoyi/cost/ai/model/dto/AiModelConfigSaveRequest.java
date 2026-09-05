package com.ruoyi.cost.ai.model.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 新增/修改模型配置。修改时 apiKey 为空表示保持原密钥。 */
public class AiModelConfigSaveRequest
{
    private Long id;
    @NotBlank(message = "配置名称不能为空") @Size(max = 100, message = "配置名称不能超过100个字符")
    private String name;
    @NotBlank(message = "提供商类型不能为空")
    private String providerType;
    @NotBlank(message = "基础地址不能为空") @Size(max = 500, message = "基础地址不能超过500个字符")
    private String baseUrl;
    @Size(max = 2000, message = "API Key 长度不能超过2000个字符")
    private String apiKey;
    private Boolean clearApiKey;
    @NotBlank(message = "对话模型不能为空") @Size(max = 100, message = "对话模型不能超过100个字符")
    private String chatModel;
    @Size(max = 100, message = "Embedding模型不能超过100个字符")
    private String embeddingModel;
    @DecimalMin(value = "0", message = "温度不能小于0") @DecimalMax(value = "2", message = "温度不能大于2")
    private BigDecimal temperature;
    @Min(value = 1, message = "最大Token必须大于0") @Max(value = 200000, message = "最大Token不能超过200000")
    private Integer maxTokens;
    @Min(value = 1, message = "超时时间必须大于0") @Max(value = 600, message = "超时时间不能超过600秒")
    private Integer timeoutSeconds;
    @Pattern(regexp = "[01]", message = "启用状态不正确")
    private String enabled;
    @Pattern(regexp = "[YN]", message = "默认状态不正确")
    private String isDefault;
    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Boolean getClearApiKey() { return clearApiKey; }
    public void setClearApiKey(Boolean clearApiKey) { this.clearApiKey = clearApiKey; }
    public String getChatModel() { return chatModel; }
    public void setChatModel(String chatModel) { this.chatModel = chatModel; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public String getIsDefault() { return isDefault; }
    public void setIsDefault(String isDefault) { this.isDefault = isDefault; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
