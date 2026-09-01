package com.ruoyi.cost.file.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cost.file.domain.CostProjectFile;

/** 项目文件数据访问层。 */
public interface CostProjectFileMapper
{
    List<CostProjectFile> selectByProjectId(@Param("projectId") Long projectId);

    CostProjectFile selectById(@Param("id") Long id);

    int countByProjectId(@Param("projectId") Long projectId);

    int insertCostProjectFile(CostProjectFile projectFile);

    int updateFileCategory(@Param("id") Long id, @Param("fileCategory") String fileCategory,
            @Param("updateBy") String updateBy);

    int deleteCostProjectFile(@Param("id") Long id, @Param("updateBy") String updateBy);
}
