package com.ruoyi.cost.review.support;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.dto.ReviewIssueHandleRequest;

/** 审核问题人工处理状态机与输入归一化。 */
@Component
public class ReviewIssueWorkflow
{
    private static final Set<String> LEVELS = Set.of("INFO", "LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "PENDING", Set.of("PENDING", "CONFIRMED", "IGNORED"),
            "CONFIRMED", Set.of("PENDING", "CONFIRMED", "IGNORED", "RECTIFIED"),
            "IGNORED", Set.of("PENDING", "CONFIRMED", "IGNORED"),
            "RECTIFIED", Set.of("PENDING", "CONFIRMED", "RECTIFIED"));

    public void normalizeAndValidate(CostReviewIssue current, ReviewIssueHandleRequest request)
    {
        String status = StringUtils.isBlank(request.getStatus()) ? current.getStatus()
                : request.getStatus().trim().toUpperCase();
        String level = StringUtils.isBlank(request.getIssueLevel()) ? current.getIssueLevel()
                : request.getIssueLevel().trim().toUpperCase();
        Set<String> allowed = TRANSITIONS.get(current.getStatus());
        if (allowed == null || !allowed.contains(status))
            throw new ServiceException("审核问题状态不能从" + current.getStatus() + "变更为" + status);
        if (!LEVELS.contains(level)) throw new ServiceException("风险等级无效");
        request.setStatus(status);
        request.setIssueLevel(level);
        request.setReviewComment(request.getReviewComment().trim());
    }
}
