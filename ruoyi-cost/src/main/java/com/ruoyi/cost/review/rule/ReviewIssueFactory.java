package com.ruoyi.cost.review.rule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.support.ReviewIssueLevel;
import com.ruoyi.cost.review.support.ReviewIssueType;

/** 统一创建规则问题并生成可追溯JSON证据。 */
@Component
public class ReviewIssueFactory
{
    private final ObjectMapper objectMapper;

    public ReviewIssueFactory(ObjectMapper objectMapper)
    {
        this.objectMapper = objectMapper;
    }

    public CostReviewIssue item(ReviewContext context, CostBoqItem item, String side,
            String ruleCode, ReviewIssueLevel level, String title, String description,
            String originalValue, String referenceValue, BigDecimal difference,
            BigDecimal differenceRate, BigDecimal riskAmount, Map<String, Object> evidence)
    {
        CostReviewIssue issue = base(context, ruleCode, level, title, description,
                originalValue, referenceValue, difference, differenceRate, riskAmount, evidence);
        issue.setBoqItemId(item.getId());
        if ("LEFT".equals(side)) issue.setLeftItemId(item.getId());
        if ("RIGHT".equals(side)) issue.setRightItemId(item.getId());
        issue.setItemSide(side);
        issue.setItemCodeSnapshot(item.getItemCode());
        issue.setItemNameSnapshot(item.getItemName());
        return issue;
    }

    public CostReviewIssue compare(ReviewContext context, CostBoqCompare row,
            String ruleCode, ReviewIssueLevel level, String title, String description,
            String originalValue, String referenceValue, BigDecimal difference,
            BigDecimal differenceRate, BigDecimal riskAmount, Map<String, Object> evidence)
    {
        CostReviewIssue issue = base(context, ruleCode, level, title, description,
                originalValue, referenceValue, difference, differenceRate, riskAmount, evidence);
        issue.setCompareResultId(row.getId());
        issue.setLeftItemId(row.getLeftItemId());
        issue.setRightItemId(row.getRightItemId());
        issue.setBoqItemId(row.getLeftItemId() != null ? row.getLeftItemId() : row.getRightItemId());
        issue.setItemSide(row.getLeftItemId() != null && row.getRightItemId() != null
                ? "BOTH" : row.getLeftItemId() != null ? "LEFT" : "RIGHT");
        issue.setItemCodeSnapshot(StringUtils.isNotBlank(row.getLeftItemCode())
                ? row.getLeftItemCode() : row.getRightItemCode());
        issue.setItemNameSnapshot(StringUtils.isNotBlank(row.getLeftItemName())
                ? row.getLeftItemName() : row.getRightItemName());
        return issue;
    }

    public Map<String, Object> evidence(Object... entries)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2)
        {
            result.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return result;
    }

    private CostReviewIssue base(ReviewContext context, String ruleCode,
            ReviewIssueLevel level, String title, String description, String originalValue,
            String referenceValue, BigDecimal difference, BigDecimal differenceRate,
            BigDecimal riskAmount, Map<String, Object> evidence)
    {
        CostReviewIssue issue = new CostReviewIssue();
        issue.setProjectId(context.projectId());
        issue.setIssueType(ReviewIssueType.fromRule(ruleCode));
        issue.setIssueLevel(level.name());
        issue.setIssueTitle(title);
        issue.setIssueDescription(description);
        issue.setOriginalValue(originalValue);
        issue.setReferenceValue(referenceValue);
        issue.setDifferenceValue(difference);
        issue.setDifferenceRate(differenceRate);
        issue.setRiskAmount(risk(riskAmount));
        issue.setRuleCode(ruleCode);
        issue.setStatus("PENDING");
        try
        {
            issue.setEvidenceJson(objectMapper.writeValueAsString(evidence));
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("审核规则证据序列化失败");
        }
        return issue;
    }

    private BigDecimal risk(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO
                : value.abs().setScale(6, RoundingMode.HALF_UP);
    }
}
