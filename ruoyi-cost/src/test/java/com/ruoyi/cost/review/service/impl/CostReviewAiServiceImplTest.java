package com.ruoyi.cost.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.mapper.CostBoqItemMapper;
import com.ruoyi.cost.review.agent.CostReviewAgent;
import com.ruoyi.cost.review.agent.CostReviewAgentResult;
import com.ruoyi.cost.review.agent.CostReviewContextBuilder;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.domain.CostReviewTask;
import com.ruoyi.cost.review.dto.CostReviewAiRequest;
import com.ruoyi.cost.review.service.CostReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CostReviewAiServiceImplTest
{
    @Mock CostReviewService reviewService;
    @Mock CostBoqItemMapper itemMapper;
    @Mock CostReviewAgent agent;
    @Mock CostReviewAiPersistenceService persistence;
    private final CostReviewContextBuilder contextBuilder = new CostReviewContextBuilder(new ObjectMapper());

    @Test
    void analyzesOnlyTwoOwnedItemsAndPersistsSuggestion()
    {
        CostReviewIssue issue = issue("QUANTITY_DIFF");
        CostReviewTask task = task();
        CostBoqItem left = item(11L, 1L);
        CostBoqItem right = item(12L, 2L);
        CostReviewAgentResult result = new CostReviewAgentResult();
        result.setHasIssue(true); result.setIssueType("FEATURE"); result.setRiskLevel("HIGH");
        when(reviewService.selectIssueById(9L)).thenReturn(issue, issue);
        when(reviewService.selectTaskById(7L)).thenReturn(task);
        when(itemMapper.selectItemById(11L)).thenReturn(left);
        when(itemMapper.selectItemById(12L)).thenReturn(right);
        when(agent.execute(any())).thenReturn(result);

        CostReviewAiServiceImpl service = new CostReviewAiServiceImpl(reviewService, itemMapper,
                contextBuilder, agent, persistence);
        assertEquals(result, service.analyzeIssue(9L, new CostReviewAiRequest(), 3L, "reviewer").analysis());

        verify(itemMapper).selectItemById(11L);
        verify(itemMapper).selectItemById(12L);
        verify(persistence).save(9L, result, 3L, "reviewer");
    }

    @Test
    void rejectsDeterministicIssueBeforeLoadingBoqOrCallingModel()
    {
        when(reviewService.selectIssueById(9L)).thenReturn(issue("NEGATIVE_QUANTITY"));
        CostReviewAiServiceImpl service = new CostReviewAiServiceImpl(reviewService, itemMapper,
                contextBuilder, agent, persistence);

        assertThrows(ServiceException.class,
                () -> service.analyzeIssue(9L, new CostReviewAiRequest(), 3L, "reviewer"));
        verify(itemMapper, never()).selectItemById(any());
        verify(agent, never()).execute(any());
    }

    @Test
    void rejectsItemFromAnotherProject()
    {
        CostReviewIssue issue = issue("ONLY_LEFT");
        when(reviewService.selectIssueById(9L)).thenReturn(issue);
        when(reviewService.selectTaskById(7L)).thenReturn(task());
        CostBoqItem foreign = item(11L, 1L); foreign.setProjectId(999L);
        when(itemMapper.selectItemById(11L)).thenReturn(foreign);
        CostReviewAiServiceImpl service = new CostReviewAiServiceImpl(reviewService, itemMapper,
                contextBuilder, agent, persistence);

        assertThrows(ServiceException.class,
                () -> service.analyzeIssue(9L, new CostReviewAiRequest(), 3L, "reviewer"));
        verify(agent, never()).execute(any());
    }

    private CostReviewIssue issue(String ruleCode)
    {
        CostReviewIssue issue = new CostReviewIssue();
        issue.setId(9L); issue.setReviewTaskId(7L); issue.setProjectId(5L);
        issue.setLeftItemId(11L); issue.setRightItemId(12L);
        issue.setRuleCode(ruleCode); issue.setIssueType("QUANTITY"); issue.setIssueLevel("MEDIUM");
        issue.setIssueTitle("差异");
        return issue;
    }

    private CostReviewTask task()
    {
        CostReviewTask task = new CostReviewTask();
        task.setId(7L); task.setProjectId(5L); task.setLeftBatchId(1L); task.setRightBatchId(2L);
        task.setTaskName("送审 VS 审核");
        return task;
    }

    private CostBoqItem item(Long id, Long batchId)
    {
        CostBoqItem item = new CostBoqItem();
        item.setId(id); item.setProjectId(5L); item.setBatchId(batchId); item.setItemName("混凝土");
        return item;
    }
}
