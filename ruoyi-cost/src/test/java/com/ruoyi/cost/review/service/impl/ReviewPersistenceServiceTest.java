package com.ruoyi.cost.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.review.domain.CostReviewTask;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.mapper.CostReviewTaskMapper;
import com.ruoyi.cost.review.mapper.CostReviewIssueMapper;

@ExtendWith(MockitoExtension.class)
class ReviewPersistenceServiceTest
{
    @Mock CostReviewTaskMapper taskMapper;
    @Mock CostReviewIssueMapper issueMapper;

    @Test
    void insertsLargeIssueSetInChunksAndCalculatesCountsAndRisk()
    {
        CostReviewTask task = new CostReviewTask();
        task.setId(9L);
        List<CostReviewIssue> issues = new ArrayList<>();
        for (int index = 0; index < 1201; index++)
        {
            CostReviewIssue issue = new CostReviewIssue();
            issue.setIssueLevel(index == 0 ? "CRITICAL" : index < 3 ? "HIGH" : "MEDIUM");
            issue.setRiskAmount(new BigDecimal("0.10"));
            issues.add(issue);
        }
        when(issueMapper.batchInsert(any())).thenAnswer(invocation ->
                ((List<?>) invocation.getArgument(0)).size());
        when(taskMapper.finishSuccess(eq(9L), anyInt(), anyInt(), anyInt(), anyInt(),
                any(BigDecimal.class), eq("admin"))).thenReturn(1);

        new ReviewPersistenceService(taskMapper, issueMapper).complete(task, issues, "admin");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CostReviewIssue>> chunks = ArgumentCaptor.forClass(List.class);
        verify(issueMapper, org.mockito.Mockito.times(3)).batchInsert(chunks.capture());
        assertEquals(List.of(500, 500, 201), chunks.getAllValues().stream().map(List::size).toList());
        verify(taskMapper).finishSuccess(9L, 1201, 1198, 2, 1,
                new BigDecimal("120.10"), "admin");
        assertEquals(9L, issues.get(0).getReviewTaskId());
        assertEquals("admin", issues.get(0).getCreateBy());
    }

    @Test
    void rejectsPartialBatchInsert()
    {
        CostReviewTask task = new CostReviewTask();
        task.setId(9L);
        CostReviewIssue issue = new CostReviewIssue();
        when(issueMapper.batchInsert(any())).thenReturn(0);
        assertThrows(ServiceException.class, () -> new ReviewPersistenceService(taskMapper,
                issueMapper).complete(task, List.of(issue), "admin"));
    }

    @Test
    void truncatesFailureMessageBeforePersistence()
    {
        String message = "x".repeat(1200);
        new ReviewPersistenceService(taskMapper, issueMapper).fail(9L, message, "admin");
        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(taskMapper).finishFailed(eq(9L), value.capture(), eq("admin"));
        assertEquals(1000, value.getValue().length());
    }
}
