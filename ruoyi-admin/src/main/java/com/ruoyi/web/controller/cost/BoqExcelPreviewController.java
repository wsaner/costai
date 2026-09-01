package com.ruoyi.web.controller.cost;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.cost.boq.preview.dto.BoqPreviewRequest;
import com.ruoyi.cost.boq.preview.service.BoqExcelPreviewService;

/** 工程量清单Excel/CSV预览与字段识别。 */
@RestController
@RequestMapping("/cost/boq/preview")
@Tag(name = "工程量清单Excel预览")
public class BoqExcelPreviewController extends BaseController
{
    private final BoqExcelPreviewService previewService;

    public BoqExcelPreviewController(BoqExcelPreviewService previewService)
    {
        this.previewService = previewService;
    }

    @PreAuthorize("@ss.hasPermi('cost:boq:preview') and @ss.hasPermi('cost:file:upload')")
    @Log(title = "工程量清单预览", businessType = BusinessType.IMPORT)
    @PostMapping(value = "/upload/{projectId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传工程量清单并生成字段识别预览")
    public AjaxResult upload(@PathVariable Long projectId, @RequestParam MultipartFile file)
    {
        return success(previewService.uploadAndPreview(projectId, file, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('cost:boq:preview') and @ss.hasPermi('cost:file:query')")
    @PostMapping("/files/{projectFileId}")
    @Operation(summary = "重新预览项目文件或切换Sheet")
    public AjaxResult preview(@PathVariable Long projectFileId,
            @Validated @RequestBody(required = false) BoqPreviewRequest request)
    {
        String sheetName = request == null ? null : request.getSheetName();
        return success(previewService.previewProjectFile(projectFileId, sheetName));
    }
}
