package com.ruoyi.cost.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.cost.boq.match.mapper.CostBoqCompareMapper;
import com.ruoyi.cost.boq.service.CostBoqService;
import com.ruoyi.cost.project.domain.CostProject;
import com.ruoyi.cost.project.service.ICostProjectService;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.domain.CostReviewTask;
import com.ruoyi.cost.review.dto.ReviewIssueHandleRequest;
import com.ruoyi.cost.review.mapper.CostReviewIssueMapper;
import com.ruoyi.cost.review.mapper.CostReviewTaskMapper;
import com.ruoyi.cost.review.rule.ReviewRuleEngine;
import com.ruoyi.cost.review.service.ReviewRuleConfigService;
import com.ruoyi.cost.review.support.ReviewIssueWorkflow;

@ExtendWith(MockitoExtension.class)
class CostReviewServiceImplIssueTest
{
    @Mock CostBoqService boqService;
    @Mock ICostProjectService projectService;
    @Mock CostBoqCompareMapper compareMapper;
    @Mock CostReviewTaskMapper taskMapper;
    @Mock CostReviewIssueMapper issueMapper;
    @Mock ReviewRuleConfigService configService;
    @Mock ReviewRuleEngine ruleEngine;
    @Mock ReviewPersistenceService persistence;

    @Test
    void handlesIssueAfterTaskProjectPermissionCheckAndRefreshesStatistics()
    {
        CostReviewIssue pending = issue("PENDING", "MEDIUM");
        CostReviewIssue confirmed = issue("CONFIRMED", "HIGH");
        confirmed.setReviewer("admin");
        CostReviewTask task = new CostReviewTask();
        task.setId(9L);
        task.setProjectId(7L);
        when(issueMapper.selectIssueById(11L)).thenReturn(pending, confirmed);
        when(taskMapper.selectTaskById(9L)).thenReturn(task);
        when(projectService.selectCostProjectById(7L)).thenReturn(new CostProject());
        when(issueMapper.updateIssueHandle(org.mockito.ArgumentMatchers.eq(11L),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq("admin"))).thenReturn(1);
        when(taskMapper.refreshStatistics(9L, "admin")).thenReturn(1);
        CostReviewServiceImpl service = service();
        ReviewIssueHandleRequest request = new ReviewIssueHandleRequest();
        request.setStatus("CONFIRMED");
        request.setIssueLevel("HIGH");
        request.setReviewComment("核对原始工程量后确认");

        CostReviewIssue result = service.handleIssue(11L, request, 1L, "admin");

        assertEquals("CONFIRMED", result.getStatus());
        assertEquals("admin", result.getReviewer());
        verify(taskMapper).refreshStatistics(9L, "admin");
    }

    private CostReviewServiceImpl service()
    {
        return new CostReviewServiceImpl(boqService, projectService, compareMapper, taskMapper,
                issueMapper, configService, ruleEngine, persistence, new ObjectMapper(),
                new ReviewIssueWorkflow());
    }

    private CostReviewIssue issue(String status, String level)
    {
        CostReviewIssue issue = new CostReviewIssue();
        issue.setId(11L);
        issue.setReviewTaskId(9L);
        issue.setProjectId(7L);
        issue.setStatus(status);
        issue.setIssueLevel(level);
        return issue;
    }
}
