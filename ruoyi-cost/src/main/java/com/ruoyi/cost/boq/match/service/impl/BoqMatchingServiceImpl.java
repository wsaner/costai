package com.ruoyi.cost.boq.match.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import com.github.pagehelper.Page;
import com.github.pagehelper.page.PageMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.boq.domain.CostBoqBatch;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.mapper.CostBoqBatchMapper;
import com.ruoyi.cost.boq.mapper.CostBoqItemMapper;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.boq.match.dto.BoqCompareRequest;
import com.ruoyi.cost.boq.match.dto.BoqManualMatchRequest;
import com.ruoyi.cost.boq.match.mapper.CostBoqCompareMapper;
import com.ruoyi.cost.boq.match.service.BoqMatchEngine;
import com.ruoyi.cost.boq.match.service.BoqMatchingService;
import com.ruoyi.cost.boq.match.support.BoqCompareCalculator;
import com.ruoyi.cost.boq.match.support.BoqMatchType;
import com.ruoyi.cost.boq.match.vo.BoqCompareSummaryVo;
import com.ruoyi.cost.boq.service.CostBoqService;

/** 清单匹配编排、人工覆盖与权限校验。 */
@Service
public class BoqMatchingServiceImpl implements BoqMatchingService
{
    private static final int INSERT_CHUNK_SIZE = 500;
    private final CostBoqService boqService;
    private final CostBoqBatchMapper batchMapper;
    private final CostBoqItemMapper itemMapper;
    private final CostBoqCompareMapper compareMapper;
    private final BoqMatchEngine matchEngine;
    private final BoqCompareCalculator calculator;

    public BoqMatchingServiceImpl(CostBoqService boqService, CostBoqBatchMapper batchMapper,
            CostBoqItemMapper itemMapper, CostBoqCompareMapper compareMapper,
            BoqMatchEngine matchEngine, BoqCompareCalculator calculator)
    {
        this.boqService = boqService;
        this.batchMapper = batchMapper;
        this.itemMapper = itemMapper;
        this.compareMapper = compareMapper;
        this.matchEngine = matchEngine;
        this.calculator = calculator;
    }

    @Override
    @Transactional
    public BoqCompareSummaryVo startMatch(BoqCompareRequest request, String operator)
    {
        PairContext pair = requirePair(request);
        lockPair(pair);
        if (compareMapper.countPair(pair.projectId, pair.leftBatchId, pair.rightBatchId) > 0)
        {
            throw new ServiceException("该批次组合已有对比结果，请使用重新匹配");
        }
        List<CostBoqItem> leftItems = loadItems(pair.leftBatchId);
        List<CostBoqItem> rightItems = loadItems(pair.rightBatchId);
        requireAnyItems(leftItems, rightItems);
        insertRows(matchEngine.match(pair.projectId, pair.leftBatchId, pair.rightBatchId,
                leftItems, rightItems, Collections.emptySet(), Collections.emptySet(), operator));
        return summary(pair);
    }

    @Override
    @Transactional
    public BoqCompareSummaryVo rematch(BoqCompareRequest request, String operator)
    {
        PairContext pair = requirePair(request);
        lockPair(pair);
        List<CostBoqCompare> existing = compareMapper.selectPairRows(
                pair.projectId, pair.leftBatchId, pair.rightBatchId);
        Set<Long> manualLeft = new HashSet<>();
        Set<Long> manualRight = new HashSet<>();
        existing.stream().filter(row -> BoqMatchType.MANUAL.name().equals(row.getMatchType())).forEach(row -> {
            if (row.getLeftItemId() != null) manualLeft.add(row.getLeftItemId());
            if (row.getRightItemId() != null) manualRight.add(row.getRightItemId());
        });
        List<CostBoqItem> leftItems = loadItems(pair.leftBatchId);
        List<CostBoqItem> rightItems = loadItems(pair.rightBatchId);
        requireAnyItems(leftItems, rightItems);
        compareMapper.deleteNonManualByPair(pair.projectId, pair.leftBatchId,
                pair.rightBatchId, operator);
        insertRows(matchEngine.match(pair.projectId, pair.leftBatchId, pair.rightBatchId,
                leftItems, rightItems, manualLeft, manualRight, operator));
        return summary(pair);
    }

    @Override
    public List<CostBoqCompare> selectCompareList(CostBoqCompare query)
    {
        if (query == null)
        {
            throw new ServiceException("对比查询条件不能为空");
        }
        BoqCompareRequest request = request(query.getProjectId(), query.getLeftBatchId(), query.getRightBatchId());
        withoutPagination(() -> requirePair(request));
        validateMatchType(query.getMatchType());
        return compareMapper.selectCompareList(query);
    }

    @Override
    public BoqCompareSummaryVo selectSummary(BoqCompareRequest request)
    {
        return summary(requirePair(request));
    }

    @Override
    @Transactional
    public BoqCompareSummaryVo manualMatch(BoqManualMatchRequest request, String operator)
    {
        PairContext pair = requirePair(request);
        lockPair(pair);
        CostBoqItem left = requireItem(request.getLeftItemId(), pair.leftBatchId, pair.projectId, "左侧");
        CostBoqItem right = requireItem(request.getRightItemId(), pair.rightBatchId, pair.projectId, "右侧");
        compareMapper.deleteByItemReferences(pair.projectId, pair.leftBatchId, pair.rightBatchId,
                left.getId(), right.getId(), operator);
        insertRows(List.of(calculator.create(pair.projectId, pair.leftBatchId, pair.rightBatchId,
                left, right, BoqMatchType.MANUAL, 1D, operator)));
        rebuildUnmatched(pair, operator);
        return summary(pair);
    }

    @Override
    @Transactional
    public BoqCompareSummaryVo unmatch(Long compareId, String operator)
    {
        CostBoqCompare current = compareMapper.selectById(compareId);
        if (current == null)
        {
            throw new ServiceException("对比结果不存在");
        }
        PairContext pair = requirePair(request(current.getProjectId(),
                current.getLeftBatchId(), current.getRightBatchId()));
        lockPair(pair);
        current = compareMapper.selectById(compareId);
        if (current == null)
        {
            throw new ServiceException("对比结果已变更，请刷新后重试");
        }
        if (BoqMatchType.ONLY_LEFT.name().equals(current.getMatchType())
                || BoqMatchType.ONLY_RIGHT.name().equals(current.getMatchType()))
        {
            throw new ServiceException("该记录当前未匹配，无需取消");
        }
        if (compareMapper.deleteById(compareId, operator) != 1)
        {
            throw new ServiceException("取消匹配失败，请刷新后重试");
        }
        rebuildUnmatched(pair, operator);
        return summary(pair);
    }

    @Override
    public List<CostBoqBatch> selectBatchOptions(Long projectId)
    {
        if (projectId == null) throw new ServiceException("项目ID不能为空");
        CostBoqBatch query = new CostBoqBatch();
        query.setProjectId(projectId);
        return boqService.selectBatchList(query);
    }

    @Override
    public List<CostBoqItem> selectItemOptions(Long projectId, Long batchId, String keyword)
    {
        CostBoqBatch batch = withoutPagination(() -> boqService.selectBatchById(batchId));
        if (projectId == null || !projectId.equals(batch.getProjectId()))
        {
            throw new ServiceException("清单批次不属于当前项目");
        }
        CostBoqItem query = new CostBoqItem();
        query.setBatchId(batchId);
        query.setKeyword(StringUtils.trimToNull(keyword));
        return itemMapper.selectItemList(query);
    }

    private PairContext requirePair(BoqCompareRequest request)
    {
        if (request == null || request.getProjectId() == null || request.getLeftBatchId() == null
                || request.getRightBatchId() == null)
        {
            throw new ServiceException("项目和左右清单批次不能为空");
        }
        if (request.getLeftBatchId().equals(request.getRightBatchId()))
        {
            throw new ServiceException("左右清单批次不能相同");
        }
        CostBoqBatch left = boqService.selectBatchById(request.getLeftBatchId());
        CostBoqBatch right = boqService.selectBatchById(request.getRightBatchId());
        if (!request.getProjectId().equals(left.getProjectId())
                || !request.getProjectId().equals(right.getProjectId()))
        {
            throw new ServiceException("清单批次不属于当前项目");
        }
        return new PairContext(request.getProjectId(), left.getId(), right.getId());
    }

    private CostBoqItem requireItem(Long itemId, Long batchId, Long projectId, String side)
    {
        CostBoqItem item = itemMapper.selectItemById(itemId);
        if (item == null || !batchId.equals(item.getBatchId()) || !projectId.equals(item.getProjectId()))
        {
            throw new ServiceException(side + "清单不存在或不属于所选批次");
        }
        return item;
    }

    private void lockPair(PairContext pair)
    {
        long first = Math.min(pair.leftBatchId, pair.rightBatchId);
        long second = Math.max(pair.leftBatchId, pair.rightBatchId);
        if (batchMapper.lockBatchById(first) == null || batchMapper.lockBatchById(second) == null)
        {
            throw new ServiceException("清单批次已被删除");
        }
    }

    private List<CostBoqItem> loadItems(Long batchId)
    {
        CostBoqItem query = new CostBoqItem();
        query.setBatchId(batchId);
        return itemMapper.selectItemList(query);
    }

    private void requireAnyItems(List<CostBoqItem> leftItems, List<CostBoqItem> rightItems)
    {
        if (leftItems.isEmpty() && rightItems.isEmpty())
        {
            throw new ServiceException("左右批次均没有可对比的清单数据");
        }
    }

    private void rebuildUnmatched(PairContext pair, String operator)
    {
        compareMapper.deleteUnmatchedByPair(pair.projectId, pair.leftBatchId,
                pair.rightBatchId, operator);
        List<CostBoqCompare> matched = compareMapper.selectPairRows(
                pair.projectId, pair.leftBatchId, pair.rightBatchId);
        Set<Long> usedLeft = new HashSet<>();
        Set<Long> usedRight = new HashSet<>();
        for (CostBoqCompare row : matched)
        {
            if (row.getLeftItemId() != null) usedLeft.add(row.getLeftItemId());
            if (row.getRightItemId() != null) usedRight.add(row.getRightItemId());
        }
        List<CostBoqCompare> unmatched = new ArrayList<>();
        for (CostBoqItem item : loadItems(pair.leftBatchId))
        {
            if (!usedLeft.contains(item.getId())) unmatched.add(calculator.create(pair.projectId,
                    pair.leftBatchId, pair.rightBatchId, item, null,
                    BoqMatchType.ONLY_LEFT, 0D, operator));
        }
        for (CostBoqItem item : loadItems(pair.rightBatchId))
        {
            if (!usedRight.contains(item.getId())) unmatched.add(calculator.create(pair.projectId,
                    pair.leftBatchId, pair.rightBatchId, null, item,
                    BoqMatchType.ONLY_RIGHT, 0D, operator));
        }
        insertRows(unmatched);
    }

    private void insertRows(List<CostBoqCompare> rows)
    {
        for (int from = 0; from < rows.size(); from += INSERT_CHUNK_SIZE)
        {
            List<CostBoqCompare> chunk = rows.subList(from, Math.min(from + INSERT_CHUNK_SIZE, rows.size()));
            if (compareMapper.batchInsert(chunk) != chunk.size())
            {
                throw new ServiceException("清单对比结果批量保存数量不一致");
            }
        }
    }

    private BoqCompareSummaryVo summary(PairContext pair)
    {
        BoqCompareSummaryVo summary = compareMapper.selectSummary(
                pair.projectId, pair.leftBatchId, pair.rightBatchId);
        return summary == null ? new BoqCompareSummaryVo() : summary;
    }

    private void validateMatchType(String matchType)
    {
        if (StringUtils.isBlank(matchType)) return;
        try
        {
            BoqMatchType.valueOf(matchType);
        }
        catch (IllegalArgumentException e)
        {
            throw new ServiceException("匹配状态无效");
        }
    }

    private BoqCompareRequest request(Long projectId, Long leftBatchId, Long rightBatchId)
    {
        BoqCompareRequest request = new BoqCompareRequest();
        request.setProjectId(projectId);
        request.setLeftBatchId(leftBatchId);
        request.setRightBatchId(rightBatchId);
        return request;
    }

    private <T> T withoutPagination(Supplier<T> action)
    {
        Page<?> page = PageMethod.getLocalPage();
        if (page == null) return action.get();
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

    private record PairContext(Long projectId, Long leftBatchId, Long rightBatchId) {}
}
