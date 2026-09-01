package com.ruoyi.cost.boq.preview.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.boq.preview.reader.CsvStreamingWorkbookReader;
import com.ruoyi.cost.boq.preview.reader.SpreadsheetReaderFactory;
import com.ruoyi.cost.boq.preview.service.impl.RuleBasedBoqColumnMappingService;
import com.ruoyi.cost.boq.preview.support.BoqHeaderDetector;
import com.ruoyi.cost.boq.preview.vo.BoqExcelPreviewVo;
import com.ruoyi.cost.file.domain.CostProjectFile;
import com.ruoyi.cost.file.service.ICostProjectFileService;
import com.ruoyi.cost.file.vo.CostProjectFileDownloadVo;

@ExtendWith(MockitoExtension.class)
class BoqExcelPreviewServiceImplTest
{
    @Mock
    private ICostProjectFileService projectFileService;

    @TempDir
    Path tempDir;

    private BoqExcelPreviewServiceImpl previewService;

    @BeforeEach
    void setUp()
    {
        SpreadsheetReaderFactory factory = new SpreadsheetReaderFactory(List.of(new CsvStreamingWorkbookReader()));
        BoqHeaderDetector detector = new BoqHeaderDetector(new RuleBasedBoqColumnMappingService());
        previewService = new BoqExcelPreviewServiceImpl(projectFileService, factory, detector);
    }

    @Test
    void detectsHeaderMapsAliasesLimitsPreviewAndReportsInvalidNumber() throws Exception
    {
        Path path = tempDir.resolve("boq.csv");
        StringBuilder csv = new StringBuilder("分部分项工程量清单,,,,,\n")
                .append("清单编号,工程名称,计量单位,数量,单价,总金额\n");
        for (int i = 0; i < 60; i++)
        {
            csv.append("0101,").append("项目").append(i).append(",m3,")
                    .append(i == 0 ? "非法数量" : i).append(",100,1000\n");
        }
        Files.writeString(path, csv.toString(), StandardCharsets.UTF_8);
        mockProjectFile(path, "csv");

        BoqExcelPreviewVo result = previewService.previewProjectFile(9L, null);

        assertEquals(2, result.getDetectedHeaderRow());
        assertEquals(50, result.getPreviewRows().size());
        assertEquals("A", result.getMappingSuggestions().get("itemCode"));
        assertEquals("B", result.getMappingSuggestions().get("itemName"));
        assertEquals("D", result.getMappingSuggestions().get("quantity"));
        assertTrue(result.getWarnings().stream().anyMatch(message -> message.contains("不是有效数字")));
    }

    @Test
    void rejectsMissingHeaderWithFriendlyMessage() throws Exception
    {
        Path path = tempDir.resolve("other.csv");
        Files.writeString(path, "姓名,电话,地址\n张三,123,合肥\n", StandardCharsets.UTF_8);
        mockProjectFile(path, "csv");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> previewService.previewProjectFile(9L, null));

        assertTrue(exception.getMessage().contains("未找到可识别"));
    }

    @Test
    void rejectsDetectedHeaderWithoutData() throws Exception
    {
        Path path = tempDir.resolve("empty.csv");
        Files.writeString(path, "项目编码,项目名称,工程量\n", StandardCharsets.UTF_8);
        mockProjectFile(path, "csv");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> previewService.previewProjectFile(9L, null));

        assertTrue(exception.getMessage().contains("没有有效数据"));
    }

    @Test
    void rejectsOleEncryptedXlsxWithFriendlyMessage() throws Exception
    {
        Path path = tempDir.resolve("encrypted.xlsx");
        Files.write(path, new byte[] { (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, 0, 0, 0, 0 });
        mockProjectFile(path, "xlsx");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> previewService.previewProjectFile(9L, null));

        assertTrue(exception.getMessage().contains("密码保护"));
    }

    @Test
    void uploadUsesProjectFileCenterAndBoqCategory() throws Exception
    {
        Path path = tempDir.resolve("upload.csv");
        Files.writeString(path, "项目编码,项目名称,工程量\n01,混凝土,10\n", StandardCharsets.UTF_8);
        CostProjectFile stored = projectFile("csv");
        when(projectFileService.uploadProjectFile(org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq("BOQ"), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("admin"))).thenReturn(stored);
        when(projectFileService.selectProjectFileById(9L)).thenReturn(stored);
        when(projectFileService.prepareDownload(9L))
                .thenReturn(new CostProjectFileDownloadVo(path, "upload.csv", "text/csv"));

        previewService.uploadAndPreview(3L,
                new MockMultipartFile("file", "upload.csv", "text/csv", "data".getBytes()), "admin");

        verify(projectFileService).uploadProjectFile(org.mockito.ArgumentMatchers.eq(3L),
                org.mockito.ArgumentMatchers.eq("BOQ"), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("admin"));
    }

    @Test
    void rejectsNonSpreadsheetBeforeCreatingProjectFile()
    {
        MockMultipartFile pdf = new MockMultipartFile("file", "清单.pdf", "application/pdf", "%PDF".getBytes());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> previewService.uploadAndPreview(3L, pdf, "admin"));

        assertTrue(exception.getMessage().contains("xlsx、xls、csv"));
        verifyNoInteractions(projectFileService);
    }

    private void mockProjectFile(Path path, String extension)
    {
        when(projectFileService.selectProjectFileById(9L)).thenReturn(projectFile(extension));
        when(projectFileService.prepareDownload(9L))
                .thenReturn(new CostProjectFileDownloadVo(path, "boq." + extension, "text/csv"));
    }

    private CostProjectFile projectFile(String extension)
    {
        CostProjectFile file = new CostProjectFile();
        file.setId(9L);
        file.setProjectId(3L);
        file.setOriginalName("boq." + extension);
        file.setFileExt(extension);
        return file;
    }
}
