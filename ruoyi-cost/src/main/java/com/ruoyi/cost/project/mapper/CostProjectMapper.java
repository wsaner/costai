package com.ruoyi.cost.project.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cost.project.domain.CostProject;
import com.ruoyi.cost.project.vo.CostProjectStatisticsVo;

/**
 * 造价项目数据访问层。
 */
public interface CostProjectMapper
{
    CostProject selectCostProjectById(CostProject project);

    List<CostProject> selectCostProjectList(CostProject project);

    CostProject selectByProjectCode(@Param("projectCode") String projectCode);

    int insertCostProject(CostProject project);

    int updateCostProject(CostProject project);

    int updateProjectStatus(CostProject project);

    int deleteCostProjectByIds(@Param("ids") Long[] ids, @Param("updateBy") String updateBy);

    CostProjectStatisticsVo selectProjectStatistics(CostProject project);
}
