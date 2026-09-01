package com.ruoyi.web.controller.cost;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.cost.file.dto.CostProjectFileCategoryRequest;
import com.ruoyi.cost.file.service.ICostProjectFileService;
import com.ruoyi.cost.file.vo.CostProjectFileDownloadVo;

/** 项目文件中心。 */
@RestController
@RequestMapping("/cost/project")
@Tag(name = "项目文件中心")
public class CostProjectFileController extends BaseController
{
    private final ICostProjectFileService fileService;

    public CostProjectFileController(ICostProjectFileService fileService)
    {
        this.fileService = fileService;
    }

    @PreAuthorize("@ss.hasPermi('cost:file:list')")
    @GetMapping("/{projectId}/files/list")
    @Operation(summary = "查询项目文件列表")
    public AjaxResult list(@PathVariable Long projectId)
    {
        return success(fileService.selectProjectFileList(projectId));
    }

    @PreAuthorize("@ss.hasPermi('cost:file:query')")
    @GetMapping("/files/{id}")
    @Operation(summary = "查询项目文件信息")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(fileService.selectProjectFileById(id));
    }

    @PreAuthorize("@ss.hasPermi('cost:file:upload')")
    @Log(title = "项目文件", businessType = BusinessType.INSERT)
    @PostMapping(value = "/{projectId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传项目文件")
    public AjaxResult upload(@PathVariable Long projectId,
            @RequestParam String fileCategory, @RequestParam MultipartFile file)
    {
        return success(fileService.uploadProjectFile(projectId, fileCategory, file, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('cost:file:edit')")
    @Log(title = "项目文件分类", businessType = BusinessType.UPDATE)
    @PutMapping("/files/{id}/category")
    @Operation(summary = "修改项目文件分类")
    public AjaxResult updateCategory(@PathVariable Long id,
            @Validated @RequestBody CostProjectFileCategoryRequest request)
    {
        return toAjax(fileService.updateFileCategory(id, request.getFileCategory(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('cost:file:remove')")
    @Log(title = "项目文件", businessType = BusinessType.DELETE)
    @DeleteMapping("/files/{id}")
    @Operation(summary = "删除项目文件")
    public AjaxResult remove(@PathVariable Long id)
    {
        return toAjax(fileService.deleteProjectFile(id, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('cost:file:download')")
    @GetMapping("/files/{id}/download")
    @Operation(summary = "下载项目文件")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException
    {
        CostProjectFileDownloadVo download = fileService.prepareDownload(id);
        response.setContentType(StringUtils.isBlank(download.getMimeType())
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE : download.getMimeType());
        response.setContentLengthLong(download.getPath().toFile().length());
        FileUtils.setAttachmentResponseHeader(response, download.getOriginalName());
        FileUtils.writeBytes(download.getPath().toString(), response.getOutputStream());
    }

    @PreAuthorize("@ss.hasPermi('cost:file:query')")
    @GetMapping("/files/{id}/parse-status")
    @Operation(summary = "查询项目文件AI解析状态")
    public AjaxResult parseStatus(@PathVariable Long id)
    {
        return success(fileService.selectParseStatus(id));
    }
}
