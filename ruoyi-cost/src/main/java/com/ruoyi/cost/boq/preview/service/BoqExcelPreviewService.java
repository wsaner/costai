package com.ruoyi.cost.boq.preview.service;

import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.cost.boq.preview.vo.BoqExcelPreviewVo;

/** 工程量清单文件预览服务。 */
public interface BoqExcelPreviewService
{
    BoqExcelPreviewVo uploadAndPreview(Long projectId, MultipartFile file, String operator);

    BoqExcelPreviewVo previewProjectFile(Long projectFileId, String sheetName);
}
