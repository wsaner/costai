package com.ruoyi.cost.file.service.impl;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.cost.file.domain.CostProjectFile;
import com.ruoyi.cost.boq.mapper.CostBoqBatchMapper;
import com.ruoyi.cost.file.mapper.CostProjectFileMapper;
import com.ruoyi.cost.file.service.ICostProjectFileService;
import com.ruoyi.cost.file.support.CostProjectFileParseStatus;
import com.ruoyi.cost.file.support.CostProjectFilePathResolver;
import com.ruoyi.cost.file.support.CostProjectFileValidator;
import com.ruoyi.cost.file.vo.CostProjectFileDownloadVo;
import com.ruoyi.cost.file.vo.CostProjectFileStatusVo;
import com.ruoyi.cost.project.service.ICostProjectService;
import com.ruoyi.system.service.ISysDictTypeService;

/** 项目文件业务实现，复用现有本地 profile 文件存储。 */
@Service
public class CostProjectFileServiceImpl implements ICostProjectFileService
{
    private static final Logger log = LoggerFactory.getLogger(CostProjectFileServiceImpl.class);
    private static final String FILE_CATEGORY_DICT = "cost_file_category";

    private final CostProjectFileMapper fileMapper;
    private final ICostProjectService projectService;
    private final ISysDictTypeService dictTypeService;
    private final CostProjectFileValidator fileValidator;
    private final CostProjectFilePathResolver pathResolver;
    private final CostBoqBatchMapper boqBatchMapper;

    public CostProjectFileServiceImpl(CostProjectFileMapper fileMapper, ICostProjectService projectService,
            ISysDictTypeService dictTypeService, CostProjectFileValidator fileValidator,
            CostProjectFilePathResolver pathResolver, CostBoqBatchMapper boqBatchMapper)
    {
        this.fileMapper = fileMapper;
        this.projectService = projectService;
        this.dictTypeService = dictTypeService;
        this.fileValidator = fileValidator;
        this.pathResolver = pathResolver;
        this.boqBatchMapper = boqBatchMapper;
    }

    @Override
    public List<CostProjectFile> selectProjectFileList(Long projectId)
    {
        requireProjectAccess(projectId);
        return fileMapper.selectByProjectId(projectId);
    }

    @Override
    public CostProjectFile selectProjectFileById(Long id)
    {
        return requireAccessibleFile(id);
    }

    @Override
    @Transactional
    public CostProjectFile uploadProjectFile(Long projectId, String fileCategory, MultipartFile file, String operator)
    {
        requireProjectAccess(projectId);
        fileCategory = StringUtils.trim(fileCategory);
        validateCategory(fileCategory);
        String extension = fileValidator.validate(file);
        String storagePath = null;
        try
        {
            String originalName = FilenameUtils.getName(file.getOriginalFilename());
            String fileHash = sha256(file);
            storagePath = FileUploadUtils.upload(pathResolver.uploadBasePath(), file,
                    CostProjectFileValidator.ALLOWED_EXTENSIONS, true);
            deleteOnRollback(pathResolver.resolve(storagePath));

            CostProjectFile projectFile = new CostProjectFile();
            projectFile.setProjectId(projectId);
            projectFile.setFileId(IdUtils.fastSimpleUUID());
            projectFile.setOriginalName(originalName);
            projectFile.setFileName(FileUtils.getName(storagePath));
            projectFile.setFileExt(extension);
            projectFile.setMimeType(normalizeMimeType(file.getContentType()));
            projectFile.setFileSize(file.getSize());
            projectFile.setStoragePath(storagePath);
            projectFile.setFileCategory(fileCategory);
            projectFile.setFileHash(fileHash);
            projectFile.setAiParseStatus(CostProjectFileParseStatus.initialStatus(extension));
            projectFile.setCreateBy(operator);
            projectFile.setCreateTime(DateUtils.getNowDate());
            if (fileMapper.insertCostProjectFile(projectFile) != 1)
            {
                throw new ServiceException("项目文件记录保存失败");
            }
            return projectFile;
        }
        catch (ServiceException e)
        {
            deleteStoredFile(storagePath);
            throw e;
        }
        catch (Exception e)
        {
            deleteStoredFile(storagePath);
            throw new ServiceException("项目文件上传失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public int updateFileCategory(Long id, String fileCategory, String operator)
    {
        requireAccessibleFile(id);
        fileCategory = StringUtils.trim(fileCategory);
        validateCategory(fileCategory);
        return fileMapper.updateFileCategory(id, fileCategory, operator);
    }

    @Override
    @Transactional
    public int deleteProjectFile(Long id, String operator)
    {
        CostProjectFile projectFile = requireAccessibleFile(id);
        if (boqBatchMapper.countBySourceFileId(id) > 0)
        {
            throw new ServiceException("文件已用于清单导入，请先删除关联清单批次");
        }
        int rows = fileMapper.deleteCostProjectFile(id, operator);
        if (rows == 1)
        {
            Path storedFile = pathResolver.resolve(projectFile.getStoragePath());
            deleteAfterCommit(storedFile);
        }
        return rows;
    }

    @Override
    public CostProjectFileDownloadVo prepareDownload(Long id)
    {
        CostProjectFile projectFile = requireAccessibleFile(id);
        Path path = pathResolver.resolve(projectFile.getStoragePath());
        if (!Files.isRegularFile(path))
        {
            throw new ServiceException("文件不存在或已被移除");
        }
        return new CostProjectFileDownloadVo(path, projectFile.getOriginalName(), projectFile.getMimeType());
    }

    @Override
    public CostProjectFileStatusVo selectParseStatus(Long id)
    {
        CostProjectFile projectFile = requireAccessibleFile(id);
        CostProjectFileStatusVo status = new CostProjectFileStatusVo();
        status.setId(projectFile.getId());
        status.setFileId(projectFile.getFileId());
        status.setAiParseStatus(projectFile.getAiParseStatus());
        status.setAiParseError(projectFile.getAiParseError());
        status.setUpdateTime(projectFile.getUpdateTime());
        return status;
    }

    private CostProjectFile requireAccessibleFile(Long id)
    {
        if (id == null)
        {
            throw new ServiceException("文件ID不能为空");
        }
        CostProjectFile projectFile = fileMapper.selectById(id);
        if (projectFile == null)
        {
            throw new ServiceException("项目文件不存在");
        }
        requireProjectAccess(projectFile.getProjectId());
        return projectFile;
    }

    private void requireProjectAccess(Long projectId)
    {
        if (projectId == null)
        {
            throw new ServiceException("项目ID不能为空");
        }
        projectService.selectCostProjectById(projectId);
    }

    private void validateCategory(String fileCategory)
    {
        String normalized = StringUtils.trim(fileCategory);
        List<SysDictData> categories = dictTypeService.selectDictDataByType(FILE_CATEGORY_DICT);
        boolean valid = categories != null && categories.stream().anyMatch(item ->
                UserConstants.NORMAL.equals(item.getStatus()) && normalized.equals(item.getDictValue()));
        if (!valid)
        {
            throw new ServiceException("文件分类无效");
        }
    }

    private String normalizeMimeType(String mimeType)
    {
        String value = StringUtils.isBlank(mimeType) ? "application/octet-stream" : mimeType.trim();
        try
        {
            value = MediaType.parseMediaType(value).toString();
        }
        catch (IllegalArgumentException e)
        {
            value = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return value.length() > 128 ? value.substring(0, 128) : value;
    }

    private String sha256(MultipartFile file) throws Exception
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = file.getInputStream())
        {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1)
            {
                digest.update(buffer, 0, length);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private void deleteAfterCommit(Path path)
    {
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
            {
                @Override
                public void afterCommit()
                {
                    deletePath(path);
                }
            });
        }
        else
        {
            deletePath(path);
        }
    }

    private void deleteOnRollback(Path path)
    {
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
            {
                @Override
                public void afterCompletion(int status)
                {
                    if (status != TransactionSynchronization.STATUS_COMMITTED)
                    {
                        deletePath(path);
                    }
                }
            });
        }
    }

    private void deleteStoredFile(String storagePath)
    {
        if (StringUtils.isNotBlank(storagePath))
        {
            deletePath(pathResolver.resolve(storagePath));
        }
    }

    private void deletePath(Path path)
    {
        try
        {
            Files.deleteIfExists(path);
        }
        catch (Exception e)
        {
            log.warn("删除项目文件失败: {}", path, e);
        }
    }
}
