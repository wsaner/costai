package com.ruoyi.cost.project.service;

import java.util.List;
import com.ruoyi.cost.project.domain.CostProject;
import com.ruoyi.cost.project.vo.CostProjectManagerVo;
import com.ruoyi.cost.project.vo.CostProjectStatisticsVo;

/**
 * 造价项目业务接口。
 */
public interface ICostProjectService
{
    List<CostProject> selectCostProjectList(CostProject project);

    CostProject selectCostProjectById(Long id);

    int insertCostProject(CostProject project);

    int updateCostProject(CostProject project);

    int deleteCostProjectByIds(Long[] ids, String operator);

    int changeProjectStatus(Long id, String projectStatus, String operator);

    CostProjectStatisticsVo selectProjectStatistics(CostProject project);

    List<CostProjectManagerVo> selectProjectManagerOptions(String keyword);
}
