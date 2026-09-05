package com.ruoyi.cost.review.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.cost.review.domain.CostReviewTask;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.mapper.CostReviewTaskMapper;
import com.ruoyi.cost.review.mapper.CostReviewIssueMapper;
import com.ruoyi.cost.review.support.ReviewIssueLevel;

/** 审核运行状态和结果的短事务持久化边界。 */
@Service
public class ReviewPersistenceService
{
    private static final int INSERT_CHUNK_SIZE = 500;
    private final CostReviewTaskMapper taskMapper;
    private final CostReviewIssueMapper issueMapper;
    public ReviewPersistenceService(CostReviewTaskMapper taskMapper, CostReviewIssueMapper issueMapper)
    {
        this.taskMapper = taskMapper;
        this.issueMapper = issueMapper;
    }

    @Transactional
    public CostReviewTask createRunning(CostReviewTask task)
    {
        if (taskMapper.insertTask(task) != 1 || task.getId() == null)
            throw new ServiceException("创建审核任务失败");
        return task;
    }

    @Transactional
    public void complete(CostReviewTask task, List<CostReviewIssue> issues, String operator)
    {
        Date now = DateUtils.getNowDate();
        for (CostReviewIssue issue : issues)
        {
            issue.setReviewTaskId(task.getId());
            issue.setCreateBy(operator);
            issue.setCreateTime(now);
        }
        for (int from = 0; from < issues.size(); from += INSERT_CHUNK_SIZE)
        {
            List<CostReviewIssue> chunk = issues.subList(from, Math.min(from + INSERT_CHUNK_SIZE, issues.size()));
            if (issueMapper.batchInsert(chunk) != chunk.size())
                throw new ServiceException("审核问题批量保存数量不一致");
        }
        int medium = count(issues, ReviewIssueLevel.MEDIUM);
        int high = count(issues, ReviewIssueLevel.HIGH);
        int critical = count(issues, ReviewIssueLevel.CRITICAL);
        BigDecimal risk = issues.stream().map(CostReviewIssue::getRiskAmount)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (taskMapper.finishSuccess(task.getId(), issues.size(), medium, high, critical,
                risk, operator) != 1)
            throw new ServiceException("更新审核任务结果失败");
    }

    @Transactional
    public void fail(Long taskId, String message, String operator)
    {
        String safeMessage = message == null ? "规则执行失败"
                : message.substring(0, Math.min(message.length(), 1000));
        taskMapper.finishFailed(taskId, safeMessage, operator);
    }

    private int count(List<CostReviewIssue> issues, ReviewIssueLevel level)
    {
        return (int) issues.stream().filter(issue -> level.name().equals(issue.getIssueLevel())).count();
    }
}
