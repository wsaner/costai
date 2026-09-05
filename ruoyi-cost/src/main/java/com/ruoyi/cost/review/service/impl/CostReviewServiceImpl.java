package com.ruoyi.cost.review.service.impl;

import java.util.List;
import java.util.function.Supplier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.page.PageMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.boq.domain.CostBoqBatch;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.boq.match.mapper.CostBoqCompareMapper;
import com.ruoyi.cost.boq.service.CostBoqService;
import com.ruoyi.cost.project.service.ICostProjectService;
import com.ruoyi.cost.review.domain.CostReviewTask;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.dto.ReviewIssueHandleRequest;
import com.ruoyi.cost.review.dto.ReviewStartRequest;
import com.ruoyi.cost.review.mapper.CostReviewTaskMapper;
import com.ruoyi.cost.review.mapper.CostReviewIssueMapper;
import com.ruoyi.cost.review.rule.ReviewContext;
import com.ruoyi.cost.review.rule.ReviewRuleEngine;
import com.ruoyi.cost.review.rule.ReviewRuleSettings;
import com.ruoyi.cost.review.service.CostReviewService;
import com.ruoyi.cost.review.service.ReviewRuleConfigService;
import com.ruoyi.cost.review.support.ReviewIssueWorkflow;
import com.ruoyi.cost.review.agent.CostReviewAiEligibility;

@Service
public class CostReviewServiceImpl implements CostReviewService
{
    private static final String RULE_VERSION = "JAVA_RULES_V1";
    private final CostBoqService boqService;
    private final ICostProjectService projectService;
    private final CostBoqCompareMapper compareMapper;
    private final CostReviewTaskMapper taskMapper;
    private final CostReviewIssueMapper issueMapper;
    private final ReviewRuleConfigService configService;
    private final ReviewRuleEngine ruleEngine;
    private final ReviewPersistenceService persistence;
    private final ObjectMapper objectMapper;
    private final ReviewIssueWorkflow issueWorkflow;

    public CostReviewServiceImpl(CostBoqService boqService, ICostProjectService projectService,
            CostBoqCompareMapper compareMapper, CostReviewTaskMapper taskMapper,
            CostReviewIssueMapper issueMapper, ReviewRuleConfigService configService,
            ReviewRuleEngine ruleEngine, ReviewPersistenceService persistence, ObjectMapper objectMapper,
            ReviewIssueWorkflow issueWorkflow)
    {
        this.boqService = boqService;
        this.projectService = projectService;
        this.compareMapper = compareMapper;
        this.taskMapper = taskMapper;
        this.issueMapper = issueMapper;
        this.configService = configService;
        this.ruleEngine = ruleEngine;
        this.persistence = persistence;
        this.objectMapper = objectMapper;
        this.issueWorkflow = issueWorkflow;
    }

    @Override
    public CostReviewTask startReview(ReviewStartRequest request, String operator)
    {
        Pair pair = requirePair(request);
        List<CostBoqCompare> compares = compareMapper.selectPairRows(
                pair.projectId, pair.left.getId(), pair.right.getId());
        if (compares.isEmpty()) throw new ServiceException("请先完成所选批次的清单匹配");
        List<CostBoqItem> leftItems = loadItems(pair.left.getId());
        List<CostBoqItem> rightItems = loadItems(pair.right.getId());
        ReviewRuleSettings settings = configService.loadSettings();
        CostReviewTask task = createTask(request, pair, leftItems.size(), rightItems.size(),
                compares.size(), settings, operator);
        persistence.createRunning(task);
        try
        {
            ReviewContext context = new ReviewContext(pair.projectId, pair.left.getId(),
                    pair.right.getId(), leftItems, rightItems, compares, settings);
            persistence.complete(task, ruleEngine.execute(context), operator);
            return selectTaskById(task.getId());
        }
        catch (RuntimeException e)
        {
            String message = e instanceof ServiceException ? e.getMessage() : "确定性审核规则执行失败";
            persistence.fail(task.getId(), message, operator);
            if (e instanceof ServiceException serviceException) throw serviceException;
            throw new ServiceException("确定性审核规则执行失败");
        }
    }

    @Override
    public List<CostReviewTask> selectTaskList(CostReviewTask query)
    {
        if (query == null || query.getProjectId() == null) throw new ServiceException("项目ID不能为空");
        withoutPagination(() -> projectService.selectCostProjectById(query.getProjectId()));
        return taskMapper.selectTaskList(query);
    }

    @Override
    public CostReviewTask selectTaskById(Long reviewTaskId)
    {
        if (reviewTaskId == null) throw new ServiceException("审核任务ID不能为空");
        CostReviewTask task = withoutPagination(() -> taskMapper.selectTaskById(reviewTaskId));
        if (task == null) throw new ServiceException("审核任务不存在");
        withoutPagination(() -> projectService.selectCostProjectById(task.getProjectId()));
        return task;
    }

    @Override
    public List<CostReviewIssue> selectIssueList(CostReviewIssue query)
    {
        if (query == null || query.getReviewTaskId() == null)
            throw new ServiceException("审核任务ID不能为空");
        CostReviewTask task = withoutPagination(() -> selectTaskById(query.getReviewTaskId()));
        query.setProjectId(task.getProjectId());
        List<CostReviewIssue> issues = issueMapper.selectIssueList(query);
        issues.forEach(this::decorateAiEligibility);
        return issues;
    }

    @Override
    public CostReviewIssue selectIssueById(Long issueId)
    {
        if (issueId == null) throw new ServiceException("审核问题ID不能为空");
        CostReviewIssue issue = withoutPagination(() -> issueMapper.selectIssueById(issueId));
        if (issue == null) throw new ServiceException("审核问题不存在");
        CostReviewTask task = withoutPagination(() -> selectTaskById(issue.getReviewTaskId()));
        if (!task.getProjectId().equals(issue.getProjectId())) throw new ServiceException("审核问题项目归属异常");
        decorateAiEligibility(issue);
        return issue;
    }

    @Override
    @Transactional
    public CostReviewIssue handleIssue(Long issueId, ReviewIssueHandleRequest request,
            Long reviewerUserId, String reviewer)
    {
        CostReviewIssue current = selectIssueById(issueId);
        issueWorkflow.normalizeAndValidate(current, request);
        if (issueMapper.updateIssueHandle(issueId, request, reviewerUserId, reviewer) != 1)
            throw new ServiceException("审核问题处理失败，请刷新后重试");
        if (taskMapper.refreshStatistics(current.getReviewTaskId(), reviewer) != 1)
            throw new ServiceException("审核任务统计刷新失败，请刷新后重试");
        return selectIssueById(issueId);
    }

    private Pair requirePair(ReviewStartRequest request)
    {
        if (request == null || request.getProjectId() == null || request.getLeftBatchId() == null
                || request.getRightBatchId() == null)
            throw new ServiceException("项目和左右清单批次不能为空");
        if (request.getLeftBatchId().equals(request.getRightBatchId()))
            throw new ServiceException("左右清单批次不能相同");
        CostBoqBatch left = boqService.selectBatchById(request.getLeftBatchId());
        CostBoqBatch right = boqService.selectBatchById(request.getRightBatchId());
        if (!request.getProjectId().equals(left.getProjectId())
                || !request.getProjectId().equals(right.getProjectId()))
            throw new ServiceException("清单批次不属于当前项目");
        return new Pair(request.getProjectId(), left, right);
    }

    private List<CostBoqItem> loadItems(Long batchId)
    {
        CostBoqItem query = new CostBoqItem();
        query.setBatchId(batchId);
        return boqService.selectItemList(query);
    }

    private CostReviewTask createTask(ReviewStartRequest request, Pair pair,
            int leftCount, int rightCount,
            int compareCount, ReviewRuleSettings settings, String operator)
    {
        CostReviewTask task = new CostReviewTask();
        task.setProjectId(pair.projectId);
        task.setLeftBatchId(pair.left.getId());
        task.setRightBatchId(pair.right.getId());
        task.setTaskName(StringUtils.isBlank(request.getTaskName())
                ? pair.left.getBatchName() + " VS " + pair.right.getBatchName()
                : request.getTaskName().trim());
        task.setStatus("RUNNING");
        task.setRuleVersion(RULE_VERSION);
        try
        {
            task.setConfigSnapshotJson(objectMapper.writeValueAsString(settings.snapshot()));
        }
        catch (JsonProcessingException e)
        {
            throw new ServiceException("审核规则配置快照生成失败");
        }
        task.setLeftItemCount(leftCount);
        task.setRightItemCount(rightCount);
        task.setCompareCount(compareCount);
        task.setStartedBy(operator);
        task.setStartTime(DateUtils.getNowDate());
        task.setCreateBy(operator);
        task.setCreateTime(DateUtils.getNowDate());
        return task;
    }

    private <T> T withoutPagination(Supplier<T> action)
    {
        Page<?> page = PageMethod.getLocalPage();
        if (page == null) return action.get();
        PageMethod.clearPage();
        try { return action.get(); }
        finally { PageMethod.setLocalPage(page); }
    }

    private void decorateAiEligibility(CostReviewIssue issue)
    {
        CostReviewAiEligibility.Decision decision = CostReviewAiEligibility.evaluate(issue);
        issue.setAiEligible(decision.eligible());
        issue.setAiEligibilityReason(decision.reason());
    }

    private record Pair(Long projectId, CostBoqBatch left, CostBoqBatch right) {}
}
