package com.ruoyi.cost.project.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.cost.project.domain.CostProject;
import com.ruoyi.cost.project.mapper.CostProjectMapper;
import com.ruoyi.cost.project.service.ICostProjectService;
import com.ruoyi.cost.project.support.CostProjectAmountCalculator;
import com.ruoyi.cost.project.support.CostProjectStatus;
import com.ruoyi.cost.project.vo.CostProjectManagerVo;
import com.ruoyi.cost.project.vo.CostProjectStatisticsVo;
import com.ruoyi.system.service.ISysUserService;

/**
 * 造价项目业务实现。
 */
@Service
public class CostProjectServiceImpl implements ICostProjectService
{
    private final CostProjectMapper projectMapper;
    private final ISysUserService userService;
    private final CostProjectAccessService accessService;

    public CostProjectServiceImpl(CostProjectMapper projectMapper, ISysUserService userService,
            CostProjectAccessService accessService)
    {
        this.projectMapper = projectMapper;
        this.userService = userService;
        this.accessService = accessService;
    }

    @Override
    @DataScope(deptAlias = "p", userAlias = "p", deptField = "owner_dept_id", userField = "project_manager_id")
    public List<CostProject> selectCostProjectList(CostProject project)
    {
        return projectMapper.selectCostProjectList(project);
    }

    @Override
    public CostProject selectCostProjectById(Long id)
    {
        CostProject query = new CostProject();
        query.setId(id);
        CostProject project = accessService.selectAccessibleProject(query);
        if (project == null)
        {
            throw new ServiceException("项目不存在或无权访问");
        }
        return project;
    }

    @Override
    @Transactional
    public int insertCostProject(CostProject project)
    {
        normalizeAndValidate(project);
        ensureProjectCodeUnique(project.getProjectCode(), null);
        populateManager(project);
        project.setCreateTime(DateUtils.getNowDate());
        return projectMapper.insertCostProject(project);
    }

    @Override
    @Transactional
    public int updateCostProject(CostProject project)
    {
        if (project.getId() == null)
        {
            throw new ServiceException("项目ID不能为空");
        }
        selectCostProjectById(project.getId());
        normalizeAndValidate(project);
        ensureProjectCodeUnique(project.getProjectCode(), project.getId());
        populateManager(project);
        project.setUpdateTime(DateUtils.getNowDate());
        return projectMapper.updateCostProject(project);
    }

    @Override
    @Transactional
    public int deleteCostProjectByIds(Long[] ids, String operator)
    {
        if (ids == null || ids.length == 0)
        {
            throw new ServiceException("请选择需要删除的项目");
        }
        for (Long id : ids)
        {
            selectCostProjectById(id);
        }
        return projectMapper.deleteCostProjectByIds(ids, operator);
    }

    @Override
    @Transactional
    public int changeProjectStatus(Long id, String projectStatus, String operator)
    {
        selectCostProjectById(id);
        validateStatus(projectStatus);
        CostProject project = new CostProject();
        project.setId(id);
        project.setProjectStatus(projectStatus);
        project.setUpdateBy(operator);
        project.setUpdateTime(DateUtils.getNowDate());
        return projectMapper.updateProjectStatus(project);
    }

    @Override
    @DataScope(deptAlias = "p", userAlias = "p", deptField = "owner_dept_id", userField = "project_manager_id")
    public CostProjectStatisticsVo selectProjectStatistics(CostProject project)
    {
        CostProjectStatisticsVo statistics = projectMapper.selectProjectStatistics(project);
        if (statistics == null)
        {
            return new CostProjectStatisticsVo();
        }
        statistics.setAverageReductionRate(CostProjectAmountCalculator.calculateReductionRate(
                statistics.getSubmittedAmount(), statistics.getReductionAmount()));
        return statistics;
    }

    @Override
    public List<CostProjectManagerVo> selectProjectManagerOptions(String keyword)
    {
        SysUser query = new SysUser();
        query.setStatus(UserConstants.NORMAL);
        if (StringUtils.isNotBlank(keyword))
        {
            query.setUserName(keyword.trim());
        }
        PageHelper.startPage(1, 100, false);
        return userService.selectUserList(query).stream()
                .filter(user -> user.getUserId() != null)
                .map(this::toManagerOption)
                .collect(Collectors.toList());
    }

    private CostProjectManagerVo toManagerOption(SysUser user)
    {
        CostProjectManagerVo option = new CostProjectManagerVo();
        option.setUserId(user.getUserId());
        option.setUserName(user.getUserName());
        option.setNickName(user.getNickName());
        option.setDeptId(user.getDeptId());
        option.setDeptName(user.getDept() == null ? null : user.getDept().getDeptName());
        return option;
    }

    private void normalizeAndValidate(CostProject project)
    {
        project.setProjectCode(StringUtils.trim(project.getProjectCode()));
        project.setProjectName(StringUtils.trim(project.getProjectName()));
        project.setSubmittedAmount(CostProjectAmountCalculator.amountOrZero(project.getSubmittedAmount()));
        project.setApprovedAmount(CostProjectAmountCalculator.amountOrZero(project.getApprovedAmount()));
        project.setIncreaseAmount(CostProjectAmountCalculator.amountOrZero(project.getIncreaseAmount()));
        project.setReductionAmount(CostProjectAmountCalculator.amountOrZero(project.getReductionAmount()));
        project.setReductionRate(CostProjectAmountCalculator.calculateReductionRate(
                project.getSubmittedAmount(), project.getReductionAmount()));
        validateStatus(project.getProjectStatus());
    }

    private void populateManager(CostProject project)
    {
        if (project.getProjectManagerId() == null)
        {
            throw new ServiceException("项目负责人不能为空");
        }
        userService.checkUserDataScope(project.getProjectManagerId());
        SysUser manager = userService.selectUserById(project.getProjectManagerId());
        if (manager == null || !UserConstants.NORMAL.equals(manager.getStatus()))
        {
            throw new ServiceException("项目负责人不存在或已停用");
        }
        if (manager.getDeptId() == null)
        {
            throw new ServiceException("项目负责人未归属部门");
        }
        project.setProjectManagerName(manager.getNickName());
        project.setOwnerDeptId(manager.getDeptId());
    }

    private void ensureProjectCodeUnique(String projectCode, Long currentId)
    {
        CostProject existing = projectMapper.selectByProjectCode(projectCode);
        if (existing != null && !Objects.equals(existing.getId(), currentId))
        {
            throw new ServiceException("项目编号'" + projectCode + "'已存在");
        }
    }

    private void validateStatus(String projectStatus)
    {
        if (!CostProjectStatus.isValid(projectStatus))
        {
            throw new ServiceException("项目状态无效");
        }
    }
}
