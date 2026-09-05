package com.ruoyi.cost.review.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.review.agent.CostReviewAgentResult;
import com.ruoyi.cost.review.mapper.CostReviewIssueMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 仅在模型调用结束后开启短事务，且只更新 AI 建议字段。 */
@Service
public class CostReviewAiPersistenceService
{
    private final CostReviewIssueMapper issueMapper;

    public CostReviewAiPersistenceService(CostReviewIssueMapper issueMapper)
    {
        this.issueMapper = issueMapper;
    }

    @Transactional
    public void save(Long issueId, CostReviewAgentResult result, Long userId, String username)
    {
        if (issueMapper.updateAiAnalysis(issueId, result, userId, username) != 1)
            throw new ServiceException("AI审核结果保存失败，请刷新后重试");
    }
}
