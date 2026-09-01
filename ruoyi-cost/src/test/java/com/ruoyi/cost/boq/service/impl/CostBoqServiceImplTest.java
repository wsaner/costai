package com.ruoyi.cost.boq.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import java.util.Collections;
import com.github.pagehelper.Page;
import com.github.pagehelper.page.PageMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.cost.boq.domain.CostBoqBatch;
import com.ruoyi.cost.boq.mapper.CostBoqBatchMapper;
import com.ruoyi.cost.boq.mapper.CostBoqImportErrorMapper;
import com.ruoyi.cost.boq.mapper.CostBoqItemMapper;
import com.ruoyi.cost.project.domain.CostProject;
import com.ruoyi.cost.project.service.ICostProjectService;

@ExtendWith(MockitoExtension.class)
class CostBoqServiceImplTest
{
    @Mock CostBoqBatchMapper batchMapper;
    @Mock CostBoqItemMapper itemMapper;
    @Mock CostBoqImportErrorMapper errorMapper;
    @Mock ICostProjectService projectService;

    @Test
    void deleteBatchCascadesChildrenInOneServiceTransaction()
    {
        CostBoqBatch batch = new CostBoqBatch();
        batch.setId(9L); batch.setProjectId(7L);
        when(batchMapper.selectBatchById(9L)).thenReturn(batch);
        when(projectService.selectCostProjectById(7L)).thenReturn(new CostProject());
        when(batchMapper.deleteBatch(9L, "admin")).thenReturn(1);
        CostBoqServiceImpl service = new CostBoqServiceImpl(batchMapper, itemMapper, errorMapper, projectService);

        assertEquals(1, service.deleteBatch(9L, "admin"));

        InOrder order = inOrder(errorMapper, itemMapper, batchMapper);
        order.verify(errorMapper).deleteByBatchId(9L, "admin");
        order.verify(itemMapper).deleteByBatchId(9L, "admin");
        order.verify(batchMapper).deleteBatch(9L, "admin");
    }

    @Test
    void batchListIsolatesAccessCheckFromPageHelperAndRestoresPage()
    {
        CostBoqBatch query = new CostBoqBatch();
        query.setProjectId(7L);
        when(projectService.selectCostProjectById(7L)).thenAnswer(invocation -> {
            assertNull(PageMethod.getLocalPage());
            return new CostProject();
        });
        when(batchMapper.selectBatchList(query)).thenAnswer(invocation -> {
            assertEquals(10, PageMethod.getLocalPage().getPageSize());
            return Collections.emptyList();
        });
        CostBoqServiceImpl service = new CostBoqServiceImpl(batchMapper, itemMapper, errorMapper, projectService);
        Page<?> page = PageMethod.startPage(2, 10);
        try
        {
            service.selectBatchList(query);
            assertSame(page, PageMethod.getLocalPage());
        }
        finally
        {
            PageMethod.clearPage();
        }
    }

    @Test
    void itemListIsolatesBatchAndProjectChecksFromPageHelper()
    {
        CostBoqBatch batch = new CostBoqBatch();
        batch.setId(9L);
        batch.setProjectId(7L);
        when(batchMapper.selectBatchById(9L)).thenAnswer(invocation -> {
            assertNull(PageMethod.getLocalPage());
            return batch;
        });
        when(projectService.selectCostProjectById(7L)).thenAnswer(invocation -> {
            assertNull(PageMethod.getLocalPage());
            return new CostProject();
        });
        CostBoqServiceImpl service = new CostBoqServiceImpl(batchMapper, itemMapper, errorMapper, projectService);
        com.ruoyi.cost.boq.domain.CostBoqItem query = new com.ruoyi.cost.boq.domain.CostBoqItem();
        query.setBatchId(9L);
        Page<?> page = PageMethod.startPage(1, 20);
        try
        {
            service.selectItemList(query);
            assertSame(page, PageMethod.getLocalPage());
        }
        finally
        {
            PageMethod.clearPage();
        }
    }
}
