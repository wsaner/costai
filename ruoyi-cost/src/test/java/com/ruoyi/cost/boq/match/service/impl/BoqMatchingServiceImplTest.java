package com.ruoyi.cost.boq.match.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.boq.domain.CostBoqBatch;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.mapper.CostBoqBatchMapper;
import com.ruoyi.cost.boq.mapper.CostBoqItemMapper;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.boq.match.dto.BoqCompareRequest;
import com.ruoyi.cost.boq.match.mapper.CostBoqCompareMapper;
import com.ruoyi.cost.boq.match.service.BoqMatchEngine;
import com.ruoyi.cost.boq.match.support.BoqCompareCalculator;
import com.ruoyi.cost.boq.match.support.BoqMatchType;
import com.ruoyi.cost.boq.match.support.BoqTextNormalizer;
import com.ruoyi.cost.boq.match.vo.BoqCompareSummaryVo;
import com.ruoyi.cost.boq.service.CostBoqService;

@ExtendWith(MockitoExtension.class)
class BoqMatchingServiceImplTest
{
    @Mock CostBoqService boqService;
    @Mock CostBoqBatchMapper batchMapper;
    @Mock CostBoqItemMapper itemMapper;
    @Mock CostBoqCompareMapper compareMapper;
    private BoqMatchingServiceImpl service;

    @BeforeEach
    void setUp()
    {
        BoqCompareCalculator calculator = new BoqCompareCalculator();
        service = new BoqMatchingServiceImpl(boqService, batchMapper, itemMapper, compareMapper,
                new BoqMatchEngine(new BoqTextNormalizer(), calculator), calculator);
    }

    @Test
    void rematchPreservesManualRowsAndExcludesTheirItemsFromAutomaticResults()
    {
        stubPair();
        CostBoqCompare manual = new CostBoqCompare();
        manual.setMatchType(BoqMatchType.MANUAL.name());
        manual.setLeftItemId(1L);
        manual.setRightItemId(11L);
        when(compareMapper.selectPairRows(7L, 8L, 9L)).thenReturn(List.of(manual));
        when(itemMapper.selectItemList(org.mockito.ArgumentMatchers.any(CostBoqItem.class)))
                .thenAnswer(invocation -> invocation.<CostBoqItem>getArgument(0).getBatchId().equals(8L)
                        ? List.of(item(1L, 7L, 8L, "A"), item(2L, 7L, 8L, "B"))
                        : List.of(item(11L, 7L, 9L, "A"), item(12L, 7L, 9L, "B")));
        when(compareMapper.batchInsert(anyList())).thenAnswer(invocation ->
                invocation.<List<?>>getArgument(0).size());
        when(compareMapper.selectSummary(7L, 8L, 9L)).thenReturn(new BoqCompareSummaryVo());

        service.rematch(request(), "admin");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CostBoqCompare>> rows = ArgumentCaptor.forClass(List.class);
        verify(compareMapper).batchInsert(rows.capture());
        assertEquals(1, rows.getValue().size());
        assertEquals(2L, rows.getValue().get(0).getLeftItemId());
        assertEquals(12L, rows.getValue().get(0).getRightItemId());
        assertFalse(rows.getValue().stream().anyMatch(row -> Long.valueOf(1L).equals(row.getLeftItemId())
                || Long.valueOf(11L).equals(row.getRightItemId())));
        verify(compareMapper).deleteNonManualByPair(7L, 8L, 9L, "admin");
    }

    @Test
    void startRejectsExistingPairWithoutOverwritingResults()
    {
        stubPair();
        when(compareMapper.countPair(7L, 8L, 9L)).thenReturn(1);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.startMatch(request(), "admin"));

        assertEquals("该批次组合已有对比结果，请使用重新匹配", error.getMessage());
        verify(compareMapper, never()).batchInsert(anyList());
    }

    @Test
    void rejectsCrossProjectBatchBeforeReadingItems()
    {
        CostBoqBatch left = batch(8L, 7L);
        CostBoqBatch right = batch(9L, 99L);
        when(boqService.selectBatchById(8L)).thenReturn(left);
        when(boqService.selectBatchById(9L)).thenReturn(right);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.startMatch(request(), "admin"));

        assertEquals("清单批次不属于当前项目", error.getMessage());
        verify(itemMapper, never()).selectItemList(org.mockito.ArgumentMatchers.any());
    }

    private void stubPair()
    {
        CostBoqBatch left = batch(8L, 7L);
        CostBoqBatch right = batch(9L, 7L);
        when(boqService.selectBatchById(8L)).thenReturn(left);
        when(boqService.selectBatchById(9L)).thenReturn(right);
        when(batchMapper.lockBatchById(8L)).thenReturn(8L);
        when(batchMapper.lockBatchById(9L)).thenReturn(9L);
    }

    private CostBoqBatch batch(Long id, Long projectId)
    {
        CostBoqBatch batch = new CostBoqBatch();
        batch.setId(id);
        batch.setProjectId(projectId);
        return batch;
    }

    private CostBoqItem item(Long id, Long projectId, Long batchId, String code)
    {
        CostBoqItem item = new CostBoqItem();
        item.setId(id);
        item.setProjectId(projectId);
        item.setBatchId(batchId);
        item.setItemCode(code);
        item.setItemName(code);
        item.setUnit("m3");
        return item;
    }

    private BoqCompareRequest request()
    {
        BoqCompareRequest request = new BoqCompareRequest();
        request.setProjectId(7L);
        request.setLeftBatchId(8L);
        request.setRightBatchId(9L);
        return request;
    }
}
