package com.ruoyi.cost.boq.match.service;

import java.util.List;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.boq.match.dto.BoqCompareRequest;
import com.ruoyi.cost.boq.match.dto.BoqManualMatchRequest;
import com.ruoyi.cost.boq.match.vo.BoqCompareSummaryVo;
import com.ruoyi.cost.boq.domain.CostBoqBatch;
import com.ruoyi.cost.boq.domain.CostBoqItem;

/** 清单对比与匹配业务入口。 */
public interface BoqMatchingService
{
    BoqCompareSummaryVo startMatch(BoqCompareRequest request, String operator);
    BoqCompareSummaryVo rematch(BoqCompareRequest request, String operator);
    List<CostBoqCompare> selectCompareList(CostBoqCompare query);
    BoqCompareSummaryVo selectSummary(BoqCompareRequest request);
    BoqCompareSummaryVo manualMatch(BoqManualMatchRequest request, String operator);
    BoqCompareSummaryVo unmatch(Long compareId, String operator);
    List<CostBoqBatch> selectBatchOptions(Long projectId);
    List<CostBoqItem> selectItemOptions(Long projectId, Long batchId, String keyword);
}
