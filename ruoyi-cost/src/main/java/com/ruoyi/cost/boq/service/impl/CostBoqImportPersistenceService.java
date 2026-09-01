package com.ruoyi.cost.boq.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.boq.domain.CostBoqBatch;
import com.ruoyi.cost.boq.domain.CostBoqImportError;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.mapper.CostBoqBatchMapper;
import com.ruoyi.cost.boq.mapper.CostBoqImportErrorMapper;
import com.ruoyi.cost.boq.mapper.CostBoqItemMapper;

/** 将批次创建、分块写入和最终汇总隔离为短事务。 */
@Service
public class CostBoqImportPersistenceService
{
    private final CostBoqBatchMapper batchMapper;
    private final CostBoqItemMapper itemMapper;
    private final CostBoqImportErrorMapper errorMapper;

    public CostBoqImportPersistenceService(CostBoqBatchMapper batchMapper,
            CostBoqItemMapper itemMapper, CostBoqImportErrorMapper errorMapper)
    {
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
        this.errorMapper = errorMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createBatch(CostBoqBatch batch)
    {
        if (batchMapper.insertBatch(batch) != 1 || batch.getId() == null)
        {
            throw new ServiceException("清单导入批次创建失败");
        }
        return batch.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistChunk(List<CostBoqItem> items, List<CostBoqImportError> errors)
    {
        if (!items.isEmpty() && itemMapper.batchInsert(items) != items.size())
        {
            throw new ServiceException("清单分块保存数量不一致");
        }
        if (!errors.isEmpty() && errorMapper.batchInsert(errors) != errors.size())
        {
            throw new ServiceException("错误行分块保存数量不一致");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(Long batchId, int totalCount, int successCount, int failCount,
            BigDecimal totalAmount, String status, String errorSummary, String operator)
    {
        if (batchMapper.updateImportResult(batchId, totalCount, successCount, failCount,
                totalAmount, status, errorSummary, operator) != 1)
        {
            throw new ServiceException("清单导入结果更新失败");
        }
    }
}
