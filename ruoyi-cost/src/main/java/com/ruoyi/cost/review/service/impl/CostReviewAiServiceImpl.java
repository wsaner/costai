package com.ruoyi.cost.review.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.mapper.CostBoqItemMapper;
import com.ruoyi.cost.review.agent.CostReviewAgent;
import com.ruoyi.cost.review.agent.CostReviewAgentContext;
import com.ruoyi.cost.review.agent.CostReviewAgentResult;
import com.ruoyi.cost.review.agent.CostReviewAiEligibility;
import com.ruoyi.cost.review.agent.CostReviewContextBuilder;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.domain.CostReviewTask;
import com.ruoyi.cost.review.dto.CostReviewAiAnalysisResponse;
import com.ruoyi.cost.review.dto.CostReviewAiRequest;
import com.ruoyi.cost.review.service.CostReviewAiService;
import com.ruoyi.cost.review.service.CostReviewService;
import org.springframework.stereotype.Service;

/** 单问题语义复核编排；模型网络调用期间不持有数据库事务。 */
@Service
public class CostReviewAiServiceImpl implements CostReviewAiService
{
    private final CostReviewService reviewService;
    private final CostBoqItemMapper itemMapper;
    private final CostReviewContextBuilder contextBuilder;
    private final CostReviewAgent agent;
    private final CostReviewAiPersistenceService persistenceService;

    public CostReviewAiServiceImpl(CostReviewService reviewService, CostBoqItemMapper itemMapper,
            CostReviewContextBuilder contextBuilder, CostReviewAgent agent,
            CostReviewAiPersistenceService persistenceService)
    {
        this.reviewService = reviewService;
        this.itemMapper = itemMapper;
        this.contextBuilder = contextBuilder;
        this.agent = agent;
        this.persistenceService = persistenceService;
    }

    @Override
    public CostReviewAiAnalysisResponse analyzeIssue(Long issueId, CostReviewAiRequest request,
            Long userId, String username)
    {
        CostReviewAiRequest safeRequest = request == null ? new CostReviewAiRequest() : request;
        CostReviewIssue issue = reviewService.selectIssueById(issueId);
        CostReviewAiEligibility.Decision decision = CostReviewAiEligibility.evaluate(issue);
        if (!decision.eligible()) throw new ServiceException(decision.reason());
        CostReviewTask task = reviewService.selectTaskById(issue.getReviewTaskId());
        CostBoqItem left = loadItem(issue.getLeftItemId(), issue.getProjectId(), task.getLeftBatchId(), "左侧");
        CostBoqItem right = loadItem(issue.getRightItemId(), issue.getProjectId(), task.getRightBatchId(), "右侧");
        if (left == null && right == null) throw new ServiceException("审核问题未关联可分析的清单明细");

        String boundedContext = contextBuilder.build(task, issue, left, right,
                safeRequest.getAdditionalContext());
        CostReviewAgentResult result = agent.execute(new CostReviewAgentContext(issueId,
                safeRequest.getModelConfigId(), userId, username, boundedContext));
        persistenceService.save(issueId, result, userId, username);
        return new CostReviewAiAnalysisResponse(result, reviewService.selectIssueById(issueId));
    }

    private CostBoqItem loadItem(Long itemId, Long projectId, Long expectedBatchId, String side)
    {
        if (itemId == null) return null;
        CostBoqItem item = itemMapper.selectItemById(itemId);
        if (item == null) throw new ServiceException(side + "清单明细不存在");
        if (!projectId.equals(item.getProjectId()) || !expectedBatchId.equals(item.getBatchId()))
            throw new ServiceException(side + "清单明细归属异常");
        return item;
    }
}
