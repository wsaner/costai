package com.ruoyi.cost.file.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.boq.mapper.CostBoqBatchMapper;
import com.ruoyi.cost.file.domain.CostProjectFile;
import com.ruoyi.cost.file.mapper.CostProjectFileMapper;
import com.ruoyi.cost.file.support.CostProjectFileParseStatus;
import com.ruoyi.cost.file.support.CostProjectFilePathResolver;
import com.ruoyi.cost.file.support.CostProjectFileValidator;
import com.ruoyi.cost.knowledge.mapper.KnowledgeMapper;
import com.ruoyi.cost.project.domain.CostProject;
import com.ruoyi.cost.project.service.ICostProjectService;
import com.ruoyi.system.service.ISysDictTypeService;

@ExtendWith(MockitoExtension.class)
class CostProjectFileServiceImplTest
{
    @Mock
    private CostProjectFileMapper fileMapper;
    @Mock
    private ICostProjectService projectService;
    @Mock
    private ISysDictTypeService dictTypeService;
    @Mock
    private CostBoqBatchMapper boqBatchMapper;
    @Mock
    private KnowledgeMapper knowledgeMapper;

    @TempDir
    Path tempDir;

    private String previousProfile;
    private CostProjectFileServiceImpl fileService;

    @BeforeEach
    void setUp()
    {
        previousProfile = RuoYiConfig.getProfile();
        new RuoYiConfig().setProfile(tempDir.toString());
        fileService = new CostProjectFileServiceImpl(fileMapper, projectService, dictTypeService,
                new CostProjectFileValidator(), new CostProjectFilePathResolver(), boqBatchMapper, knowledgeMapper);
        when(projectService.selectCostProjectById(7L)).thenReturn(new CostProject());
    }

    @AfterEach
    void tearDown()
    {
        new RuoYiConfig().setProfile(previousProfile);
    }

    @Test
    void listChecksProjectDataScopeBeforeQuery()
    {
        when(fileMapper.selectByProjectId(7L)).thenReturn(List.of());
        assertEquals(0, fileService.selectProjectFileList(7L).size());
        verify(projectService).selectCostProjectById(7L);
        verify(fileMapper).selectByProjectId(7L);
    }

    @Test
    void deleteRejectsFileReferencedByActiveBoqBatch()
    {
        CostProjectFile file = new CostProjectFile();
        file.setId(3L); file.setProjectId(7L); file.setStoragePath("private/project/7/a.txt");
        when(fileMapper.selectById(3L)).thenReturn(file);
        when(boqBatchMapper.countBySourceFileId(3L)).thenReturn(1);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> fileService.deleteProjectFile(3L, "admin"));

        assertTrue(exception.getMessage().contains("清单批次"));
        verify(fileMapper, never()).deleteCostProjectFile(any(), any());
    }

    @Test
    void deleteRejectsFileReferencedByKnowledgeDocument()
    {
        CostProjectFile file = new CostProjectFile();
        file.setId(4L); file.setProjectId(7L); file.setStoragePath("private/project/7/rule.pdf");
        when(fileMapper.selectById(4L)).thenReturn(file);
        when(knowledgeMapper.countByProjectFileId(4L)).thenReturn(1);
        ServiceException exception = assertThrows(ServiceException.class,
                () -> fileService.deleteProjectFile(4L, "admin"));
        assertTrue(exception.getMessage().contains("知识库"));
        verify(fileMapper, never()).deleteCostProjectFile(any(), any());
    }

    @Test
    void uploadPersistsMetadataAndServerControlledInitialStatus()
    {
        when(dictTypeService.selectDictDataByType("cost_file_category"))
                .thenReturn(List.of(category("BOQ")));
        when(fileMapper.insertCostProjectFile(any(CostProjectFile.class))).thenReturn(1);
        MockMultipartFile upload = new MockMultipartFile("file", "清单.txt", "text/plain", "工程量".getBytes());

        CostProjectFile result = fileService.uploadProjectFile(7L, "BOQ", upload, "admin");

        assertEquals(CostProjectFileParseStatus.WAITING, result.getAiParseStatus());
        assertEquals("txt", result.getFileExt());
        assertEquals(64, result.getFileHash().length());
        assertEquals("工程量".getBytes().length, result.getFileSize());
        assertEquals(true, Files.isRegularFile(new CostProjectFilePathResolver().resolve(result.getStoragePath())));
    }

    @Test
    void fileLookupChecksTheStoredProjectIdAndCannotTrustRequestProjectId()
    {
        CostProjectFile file = storedFile();
        when(fileMapper.selectById(3L)).thenReturn(file);
        when(projectService.selectCostProjectById(7L)).thenThrow(new ServiceException("项目不存在或无权访问"));

        assertThrows(ServiceException.class, () -> fileService.selectProjectFileById(3L));
        verify(projectService).selectCostProjectById(7L);
    }

    @Test
    void invalidCategoryIsRejectedBeforeStorage()
    {
        when(dictTypeService.selectDictDataByType("cost_file_category"))
                .thenReturn(List.of(category("BOQ")));
        MockMultipartFile upload = new MockMultipartFile("file", "清单.txt", "text/plain", "工程量".getBytes());

        assertThrows(ServiceException.class,
                () -> fileService.uploadProjectFile(7L, "INVALID", upload, "admin"));
        verify(fileMapper, never()).insertCostProjectFile(any());
    }

    @Test
    void deleteUsesLogicalDeleteAndRemovesPhysicalFile()
            throws Exception
    {
        Path physical = tempDir.resolve("private/project/2026/09/01/delete.txt");
        Files.createDirectories(physical.getParent());
        Files.writeString(physical, "delete");
        CostProjectFile file = storedFile();
        file.setStoragePath("/profile/private/project/2026/09/01/delete.txt");
        when(fileMapper.selectById(3L)).thenReturn(file);
        when(fileMapper.deleteCostProjectFile(3L, "admin")).thenReturn(1);

        assertEquals(1, fileService.deleteProjectFile(3L, "admin"));
        verify(fileMapper).deleteCostProjectFile(3L, "admin");
        assertFalse(Files.exists(physical));
    }

    private SysDictData category(String value)
    {
        SysDictData data = new SysDictData();
        data.setDictValue(value);
        data.setStatus("0");
        return data;
    }

    private CostProjectFile storedFile()
    {
        CostProjectFile file = new CostProjectFile();
        file.setId(3L);
        file.setProjectId(7L);
        file.setFileId("file-id");
        file.setOriginalName("清单.txt");
        file.setMimeType("text/plain");
        file.setAiParseStatus(CostProjectFileParseStatus.WAITING);
        file.setStoragePath("/profile/private/project/2026/09/01/file.txt");
        return file;
    }
}
