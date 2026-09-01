package com.ruoyi.cost.boq.service.impl;

import java.util.List;
import java.util.function.Supplier;
import com.github.pagehelper.Page;
import com.github.pagehelper.page.PageMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.boq.domain.CostBoqBatch;
import com.ruoyi.cost.boq.domain.CostBoqImportError;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.mapper.CostBoqBatchMapper;
import com.ruoyi.cost.boq.mapper.CostBoqImportErrorMapper;
import com.ruoyi.cost.boq.mapper.CostBoqItemMapper;
import com.ruoyi.cost.boq.service.CostBoqService;
import com.ruoyi.cost.project.service.ICostProjectService;

/** 清单批次、明细、错误行管理。 */
@Service
public class CostBoqServiceImpl implements CostBoqService
{
    private final CostBoqBatchMapper batchMapper;
    private final CostBoqItemMapper itemMapper;
    private final CostBoqImportErrorMapper errorMapper;
    private final ICostProjectService projectService;

    public CostBoqServiceImpl(CostBoqBatchMapper batchMapper, CostBoqItemMapper itemMapper,
            CostBoqImportErrorMapper errorMapper, ICostProjectService projectService)
    {
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
        this.errorMapper = errorMapper;
        this.projectService = projectService;
    }

    @Override
    public List<CostBoqBatch> selectBatchList(CostBoqBatch query)
    {
        if (query == null || query.getProjectId() == null)
        {
            throw new ServiceException("项目ID不能为空");
        }
        withoutPagination(() -> projectService.selectCostProjectById(query.getProjectId()));
        return batchMapper.selectBatchList(query);
    }

    @Override
    public CostBoqBatch selectBatchById(Long batchId)
    {
        CostBoqBatch batch = requireBatch(batchId);
        projectService.selectCostProjectById(batch.getProjectId());
        return batch;
    }

    @Override
    public List<CostBoqItem> selectItemList(CostBoqItem query)
    {
        if (query == null || query.getBatchId() == null)
        {
            throw new ServiceException("批次ID不能为空");
        }
        CostBoqBatch batch = withoutPagination(() -> selectBatchById(query.getBatchId()));
        query.setProjectId(batch.getProjectId());
        return itemMapper.selectItemList(query);
    }

    @Override
    public List<CostBoqImportError> selectErrorList(Long batchId)
    {
        withoutPagination(() -> selectBatchById(batchId));
        return errorMapper.selectByBatchId(batchId);
    }

    @Override
    @Transactional
    public int deleteBatch(Long batchId, String operator)
    {
        CostBoqBatch batch = selectBatchById(batchId);
        errorMapper.deleteByBatchId(batch.getId(), operator);
        itemMapper.deleteByBatchId(batch.getId(), operator);
        return batchMapper.deleteBatch(batch.getId(), operator);
    }

    private CostBoqBatch requireBatch(Long batchId)
    {
        if (batchId == null)
        {
            throw new ServiceException("批次ID不能为空");
        }
        CostBoqBatch batch = batchMapper.selectBatchById(batchId);
        if (batch == null)
        {
            throw new ServiceException("清单批次不存在");
        }
        return batch;
    }

    /**
     * Controller按照框架规范先调用startPage。关联资源查询必须先校验项目权限，
     * 因此校验期间临时移除PageHelper上下文，避免分页LIMIT误加到校验SQL上。
     */
    private <T> T withoutPagination(Supplier<T> action)
    {
        Page<?> page = PageMethod.getLocalPage();
        if (page == null)
        {
            return action.get();
        }
        PageMethod.clearPage();
        try
        {
            return action.get();
        }
        finally
        {
            PageMethod.setLocalPage(page);
        }
    }
}
