package com.ruoyi.cost.review.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.dto.ReviewIssueHandleRequest;
import com.ruoyi.cost.review.agent.CostReviewAgentResult;

public interface CostReviewIssueMapper
{
    int batchInsert(@Param("issues") List<CostReviewIssue> issues);
    List<CostReviewIssue> selectIssueList(CostReviewIssue query);
    CostReviewIssue selectIssueById(@Param("id") Long id);
    int updateIssueHandle(@Param("id") Long id, @Param("request") ReviewIssueHandleRequest request,
            @Param("reviewerUserId") Long reviewerUserId, @Param("reviewer") String reviewer);
    int updateAiAnalysis(@Param("id") Long id, @Param("result") CostReviewAgentResult result,
            @Param("userId") Long userId, @Param("username") String username);
    int deleteByBoqBatchId(@Param("boqBatchId") Long boqBatchId, @Param("updateBy") String updateBy);
}
