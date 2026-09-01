package com.ruoyi.cost.boq.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.boq.domain.CostBoqImportError;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.dto.CostBoqImportRequest;
import com.ruoyi.cost.boq.importer.BoqRowStreamReaderFactory;
import com.ruoyi.cost.boq.importer.CsvBoqRowStreamReader;
import com.ruoyi.cost.boq.vo.CostBoqImportResultVo;
import com.ruoyi.cost.file.domain.CostProjectFile;
import com.ruoyi.cost.file.service.ICostProjectFileService;
import com.ruoyi.cost.file.vo.CostProjectFileDownloadVo;
import com.ruoyi.cost.project.domain.CostProject;
import com.ruoyi.cost.project.service.ICostProjectService;

@ExtendWith(MockitoExtension.class)
class CostBoqImportServiceImplTest
{
    @Mock private ICostProjectService projectService;
    @Mock private ICostProjectFileService fileService;
    @Mock private CostBoqImportPersistenceService persistence;
    @TempDir Path tempDir;
    private CostBoqImportServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new CostBoqImportServiceImpl(projectService, fileService,
                new BoqRowStreamReaderFactory(List.of(new CsvBoqRowStreamReader())),
                persistence, new ObjectMapper());
    }

    @Test
    @SuppressWarnings("unchecked")
    void importsValidRowsRecordsInvalidRowsAndKeepsOriginalTotal() throws Exception
    {
        Path path = tempDir.resolve("boq.csv");
        Files.writeString(path, "编码,名称,数量,单价,合价\n01,土方,3,2.335,7.00\n02,混凝土,非法,10,100\n");
        prepareFile(path);
        when(persistence.createBatch(any())).thenReturn(99L);

        CostBoqImportResultVo result = service.importBoq(7L, request(), "admin");

        assertEquals("PARTIAL_FAILED", result.getImportStatus());
        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertEquals(new BigDecimal("7.00"), result.getTotalAmount());
        ArgumentCaptor<List<CostBoqItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<CostBoqImportError>> errorCaptor = ArgumentCaptor.forClass(List.class);
        verify(persistence).persistChunk(itemCaptor.capture(), errorCaptor.capture());
        CostBoqItem item = itemCaptor.getValue().get(0);
        assertEquals(new BigDecimal("7.00"), item.getTotalPrice());
        assertEquals(new BigDecimal("7.005000"), item.getCalculatedTotalPrice());
        assertEquals("quantity", errorCaptor.getValue().get(0).getErrorField());
        verify(persistence).finish(eq(99L), eq(2), eq(1), eq(1), eq(new BigDecimal("7.00")),
                eq("PARTIAL_FAILED"), eq(null), eq("admin"));
    }

    @Test
    void rejectsDuplicateTargetFieldBeforeCreatingBatch() throws Exception
    {
        Path path = tempDir.resolve("boq.csv");
        Files.writeString(path, "名称,名称2\nA,B\n");
        when(projectService.selectCostProjectById(7L)).thenReturn(new CostProject());
        CostProjectFile file = new CostProjectFile();
        file.setId(8L); file.setProjectId(7L); file.setFileExt("csv");
        when(fileService.selectProjectFileById(8L)).thenReturn(file);
        CostBoqImportRequest request = request();
        request.getColumnMappings().put("F", "itemName");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.importBoq(7L, request, "admin"));

        assertTrue(exception.getMessage().contains("不能重复映射"));
    }

    @Test
    void persistsLargeImportInChunksOfFiveHundred() throws Exception
    {
        Path path = tempDir.resolve("large.csv");
        StringBuilder csv = new StringBuilder("编码,名称,数量,单价,合价\n");
        for (int i = 1; i <= 1201; i++)
        {
            csv.append(i).append(",项目").append(i).append(",1,2,2\n");
        }
        Files.writeString(path, csv);
        prepareFile(path);
        when(persistence.createBatch(any())).thenReturn(100L);

        CostBoqImportResultVo result = service.importBoq(7L, request(), "admin");

        assertEquals(1201, result.getTotalCount());
        assertEquals(1201, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertEquals(new BigDecimal("2402"), result.getTotalAmount());
        verify(persistence, times(3)).persistChunk(any(), any());
    }

    private void prepareFile(Path path)
    {
        when(projectService.selectCostProjectById(7L)).thenReturn(new CostProject());
        CostProjectFile file = new CostProjectFile();
        file.setId(8L); file.setProjectId(7L); file.setFileExt("csv");
        when(fileService.selectProjectFileById(8L)).thenReturn(file);
        when(fileService.prepareDownload(8L)).thenReturn(new CostProjectFileDownloadVo(path, "boq.csv", "text/csv"));
    }

    private CostBoqImportRequest request()
    {
        CostBoqImportRequest request = new CostBoqImportRequest();
        request.setProjectFileId(8L);
        request.setBatchName("送审预算");
        request.setBusinessType("SUBMITTED");
        request.setSheetName("CSV");
        request.setHeaderRow(1);
        LinkedHashMap<String, String> mappings = new LinkedHashMap<>();
        mappings.put("A", "itemCode"); mappings.put("B", "itemName"); mappings.put("C", "quantity");
        mappings.put("D", "unitPrice"); mappings.put("E", "totalPrice");
        request.setColumnMappings(mappings);
        return request;
    }
}
