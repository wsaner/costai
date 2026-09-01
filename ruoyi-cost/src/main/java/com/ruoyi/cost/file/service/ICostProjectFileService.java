package com.ruoyi.cost.file.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.cost.file.domain.CostProjectFile;
import com.ruoyi.cost.file.vo.CostProjectFileDownloadVo;
import com.ruoyi.cost.file.vo.CostProjectFileStatusVo;

/** 项目文件服务。 */
public interface ICostProjectFileService
{
    List<CostProjectFile> selectProjectFileList(Long projectId);

    CostProjectFile selectProjectFileById(Long id);

    CostProjectFile uploadProjectFile(Long projectId, String fileCategory, MultipartFile file, String operator);

    int updateFileCategory(Long id, String fileCategory, String operator);

    int deleteProjectFile(Long id, String operator);

    CostProjectFileDownloadVo prepareDownload(Long id);

    CostProjectFileStatusVo selectParseStatus(Long id);
}
