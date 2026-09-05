package com.ruoyi.cost.review.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cost.review.domain.CostReviewTask;

public interface CostReviewTaskMapper
{
    int insertTask(CostReviewTask task);
    int finishSuccess(@Param("id") Long id, @Param("issueCount") int issueCount,
            @Param("mediumCount") int mediumCount, @Param("highCount") int highCount,
            @Param("criticalCount") int criticalCount, @Param("riskAmount") BigDecimal riskAmount,
            @Param("updateBy") String updateBy);
    int finishFailed(@Param("id") Long id, @Param("errorMessage") String errorMessage,
            @Param("updateBy") String updateBy);
    int refreshStatistics(@Param("id") Long id, @Param("updateBy") String updateBy);
    List<CostReviewTask> selectTaskList(CostReviewTask query);
    CostReviewTask selectTaskById(@Param("id") Long id);
    int deleteByBoqBatchId(@Param("boqBatchId") Long boqBatchId, @Param("updateBy") String updateBy);
}
