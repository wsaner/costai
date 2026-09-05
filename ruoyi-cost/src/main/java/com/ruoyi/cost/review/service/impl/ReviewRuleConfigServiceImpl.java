package com.ruoyi.cost.review.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.review.domain.CostReviewRuleConfig;
import com.ruoyi.cost.review.mapper.CostReviewRuleConfigMapper;
import com.ruoyi.cost.review.rule.ReviewRuleSettings;
import com.ruoyi.cost.review.service.ReviewRuleConfigService;

@Service
public class ReviewRuleConfigServiceImpl implements ReviewRuleConfigService
{
    private final CostReviewRuleConfigMapper mapper;
    public ReviewRuleConfigServiceImpl(CostReviewRuleConfigMapper mapper) { this.mapper = mapper; }

    @Override public List<CostReviewRuleConfig> selectConfigList() { return mapper.selectConfigList(); }
    @Override public ReviewRuleSettings loadSettings() { return ReviewRuleSettings.from(selectConfigList()); }

    @Override
    @Transactional
    public int updateConfig(Long id, String configValue, String operator)
    {
        CostReviewRuleConfig config = mapper.selectConfigById(id);
        if (config == null) throw new ServiceException("审核规则配置不存在");
        String normalized = normalize(config, configValue);
        validateRelation(config, normalized);
        return mapper.updateConfigValue(id, normalized, operator);
    }

    private String normalize(CostReviewRuleConfig config, String value)
    {
        String normalized = StringUtils.trim(value);
        if (StringUtils.isBlank(normalized)) throw new ServiceException("配置值不能为空");
        switch (config.getValueType())
        {
            case "BOOLEAN":
                if (!"true".equalsIgnoreCase(normalized) && !"false".equalsIgnoreCase(normalized))
                    throw new ServiceException("布尔配置只能为true或false");
                return normalized.toLowerCase(Locale.ROOT);
            case "DECIMAL":
                try
                {
                    BigDecimal decimal = new BigDecimal(normalized);
                    if (decimal.compareTo(BigDecimal.ZERO) < 0) throw new ServiceException("阈值不能小于0");
                    if ((config.getConfigKey().endsWith("Rate") || "relativeTolerance".equals(config.getConfigKey()))
                            && decimal.compareTo(BigDecimal.ONE) > 0)
                        throw new ServiceException("比例配置不能大于1");
                    return decimal.stripTrailingZeros().toPlainString();
                }
                catch (NumberFormatException e)
                {
                    throw new ServiceException("配置值必须是有效数字");
                }
            case "STRING_LIST":
                return normalized.replace('，', ',');
            default:
                throw new ServiceException("不支持的规则配置类型");
        }
    }

    private void validateRelation(CostReviewRuleConfig changed, String proposed)
    {
        if (!"DECIMAL".equals(changed.getValueType())) return;
        List<CostReviewRuleConfig> configs = selectConfigList();
        BigDecimal warning = value(configs, changed, proposed, "warningRate");
        BigDecimal high = value(configs, changed, proposed, "highRate");
        if (warning != null && high != null && high.compareTo(warning) < 0)
            throw new ServiceException("高风险阈值不能小于预警阈值");
        if ("TOTAL_CALC_ERROR".equals(changed.getRuleCode()))
        {
            BigDecimal relative = value(configs, changed, proposed, "relativeTolerance");
            if (relative != null && high != null && high.compareTo(relative) < 0)
                throw new ServiceException("高风险阈值不能小于相对容差");
        }
    }

    private BigDecimal value(List<CostReviewRuleConfig> configs, CostReviewRuleConfig changed,
            String proposed, String key)
    {
        if (key.equals(changed.getConfigKey())) return new BigDecimal(proposed);
        return configs.stream().filter(item -> changed.getRuleCode().equals(item.getRuleCode())
                && key.equals(item.getConfigKey())).findFirst()
                .map(item -> new BigDecimal(item.getConfigValue())).orElse(null);
    }
}
