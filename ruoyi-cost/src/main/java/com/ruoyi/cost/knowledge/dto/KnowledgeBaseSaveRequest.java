package com.ruoyi.cost.knowledge.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class KnowledgeBaseSaveRequest
{
    private Long id;
    @NotBlank @Size(max = 200)
    private String name;
    @Size(max = 1000)
    private String description;
    private String status;
    @Min(1) @Max(20)
    private Integer topK = 5;
    @DecimalMin("0") @DecimalMax("1")
    private BigDecimal similarityThreshold = new BigDecimal("0.55");
    @Min(1000) @Max(50000)
    private Integer maxContextChars = 12000;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    public BigDecimal getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(BigDecimal similarityThreshold) { this.similarityThreshold = similarityThreshold; }
    public Integer getMaxContextChars() { return maxContextChars; }
    public void setMaxContextChars(Integer maxContextChars) { this.maxContextChars = maxContextChars; }
}
