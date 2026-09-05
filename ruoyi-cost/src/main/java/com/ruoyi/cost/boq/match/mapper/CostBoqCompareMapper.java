package com.ruoyi.cost.boq.match.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.boq.match.vo.BoqCompareSummaryVo;

/** 清单对比结果数据访问层。 */
public interface CostBoqCompareMapper
{
    List<CostBoqCompare> selectCompareList(CostBoqCompare query);
    List<CostBoqCompare> selectPairRows(@Param("projectId") Long projectId,
            @Param("leftBatchId") Long leftBatchId, @Param("rightBatchId") Long rightBatchId);
    CostBoqCompare selectById(@Param("id") Long id);
    BoqCompareSummaryVo selectSummary(@Param("projectId") Long projectId,
            @Param("leftBatchId") Long leftBatchId, @Param("rightBatchId") Long rightBatchId);
    int countPair(@Param("projectId") Long projectId, @Param("leftBatchId") Long leftBatchId,
            @Param("rightBatchId") Long rightBatchId);
    int batchInsert(@Param("rows") List<CostBoqCompare> rows);
    int deleteNonManualByPair(@Param("projectId") Long projectId,
            @Param("leftBatchId") Long leftBatchId, @Param("rightBatchId") Long rightBatchId,
            @Param("updateBy") String updateBy);
    int deleteUnmatchedByPair(@Param("projectId") Long projectId,
            @Param("leftBatchId") Long leftBatchId, @Param("rightBatchId") Long rightBatchId,
            @Param("updateBy") String updateBy);
    int deleteByItemReferences(@Param("projectId") Long projectId,
            @Param("leftBatchId") Long leftBatchId, @Param("rightBatchId") Long rightBatchId,
            @Param("leftItemId") Long leftItemId, @Param("rightItemId") Long rightItemId,
            @Param("updateBy") String updateBy);
    int deleteById(@Param("id") Long id, @Param("updateBy") String updateBy);
    int deleteByBatchId(@Param("batchId") Long batchId, @Param("updateBy") String updateBy);
}
