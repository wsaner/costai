package com.ruoyi.cost.review.rule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import com.ruoyi.cost.review.domain.CostReviewIssue;

/** 依次执行所有启用且支持当前上下文的确定性规则。 */
@Component
public class ReviewRuleEngine
{
    private final List<ReviewRule> rules;

    public ReviewRuleEngine(List<ReviewRule> rules)
    {
        this.rules = rules.stream().sorted(Comparator.comparing(ReviewRule::getCode)).toList();
    }

    public List<CostReviewIssue> execute(ReviewContext context)
    {
        List<CostReviewIssue> issues = new ArrayList<>();
        for (ReviewRule rule : rules)
        {
            if (rule.supports(context)) issues.addAll(rule.execute(context));
        }
        return issues;
    }
}
