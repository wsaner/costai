package com.ruoyi.cost.boq.service;

import java.util.List;
import com.ruoyi.cost.boq.domain.CostBoqBatch;
import com.ruoyi.cost.boq.domain.CostBoqImportError;
import com.ruoyi.cost.boq.domain.CostBoqItem;

/** 清单批次与明细管理。 */
public interface CostBoqService
{
    List<CostBoqBatch> selectBatchList(CostBoqBatch query);
    CostBoqBatch selectBatchById(Long batchId);
    List<CostBoqItem> selectItemList(CostBoqItem query);
    List<CostBoqImportError> selectErrorList(Long batchId);
    int deleteBatch(Long batchId, String operator);
}
