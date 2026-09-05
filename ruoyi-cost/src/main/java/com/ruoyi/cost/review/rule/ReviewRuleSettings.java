package com.ruoyi.cost.review.rule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.review.domain.CostReviewRuleConfig;

/** 数据库规则配置的类型安全只读视图。 */
public final class ReviewRuleSettings
{
    private final Map<String, Map<String, String>> values;

    private ReviewRuleSettings(Map<String, Map<String, String>> values)
    {
        this.values = values;
    }

    public static ReviewRuleSettings from(List<CostReviewRuleConfig> configs)
    {
        Map<String, Map<String, String>> values = new LinkedHashMap<>();
        for (CostReviewRuleConfig config : configs)
        {
            values.computeIfAbsent(config.getRuleCode(), ignored -> new LinkedHashMap<>())
                    .put(config.getConfigKey(), config.getConfigValue());
        }
        Map<String, Map<String, String>> immutable = new LinkedHashMap<>();
        values.forEach((code, entries) -> immutable.put(code,
                Collections.unmodifiableMap(new LinkedHashMap<>(entries))));
        return new ReviewRuleSettings(Collections.unmodifiableMap(immutable));
    }

    public boolean enabled(String ruleCode)
    {
        String value = required(ruleCode, "enabled");
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value))
        {
            throw invalid(ruleCode, "enabled");
        }
        return Boolean.parseBoolean(value);
    }

    public BigDecimal decimal(String ruleCode, String key)
    {
        try
        {
            return new BigDecimal(required(ruleCode, key));
        }
        catch (NumberFormatException e)
        {
            throw invalid(ruleCode, key);
        }
    }

    public List<String> stringList(String ruleCode, String key)
    {
        String value = required(ruleCode, key);
        if (StringUtils.isBlank(value)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String item : value.split("[,，]"))
        {
            String trimmed = StringUtils.trim(item);
            if (StringUtils.isNotBlank(trimmed)) result.add(trimmed);
        }
        return Collections.unmodifiableList(result);
    }

    public Map<String, Map<String, String>> snapshot()
    {
        return values;
    }

    private String required(String ruleCode, String key)
    {
        String value = values.getOrDefault(ruleCode, Collections.emptyMap()).get(key);
        if (value == null)
        {
            throw new ServiceException("审核规则配置缺失：" + ruleCode + "." + key);
        }
        return value;
    }

    private ServiceException invalid(String ruleCode, String key)
    {
        return new ServiceException("审核规则配置无效：" + ruleCode + "." + key);
    }
}
