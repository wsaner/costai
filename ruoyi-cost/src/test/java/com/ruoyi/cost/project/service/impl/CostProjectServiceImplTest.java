package com.ruoyi.cost.project.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.project.domain.CostProject;
import com.ruoyi.cost.file.mapper.CostProjectFileMapper;
import com.ruoyi.cost.project.mapper.CostProjectMapper;
import com.ruoyi.cost.project.support.CostProjectStatus;
import com.ruoyi.cost.project.vo.CostProjectStatisticsVo;
import com.ruoyi.system.service.ISysUserService;

@ExtendWith(MockitoExtension.class)
class CostProjectServiceImplTest
{
    @Mock
    private CostProjectMapper projectMapper;

    @Mock
    private ISysUserService userService;

    @Mock
    private CostProjectAccessService accessService;

    @Mock
    private CostProjectFileMapper projectFileMapper;

    @InjectMocks
    private CostProjectServiceImpl projectService;

    @Test
    void rejectsDuplicateProjectCode()
    {
        CostProject existing = new CostProject();
        existing.setId(9L);
        when(projectMapper.selectByProjectCode("COST-001")).thenReturn(existing);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> projectService.insertCostProject(validProject()));

        assertEquals("项目编号'COST-001'已存在", exception.getMessage());
    }

    @Test
    void insertsWithServerCalculatedRateAndManagerSnapshot()
    {
        CostProject project = validProject();
        SysUser manager = new SysUser();
        manager.setUserId(10L);
        manager.setNickName("张造价");
        manager.setDeptId(103L);
        manager.setStatus("0");
        when(userService.selectUserById(10L)).thenReturn(manager);
        when(projectMapper.insertCostProject(project)).thenReturn(1);

        assertEquals(1, projectService.insertCostProject(project));
        assertEquals(new BigDecimal("0.125000"), project.getReductionRate());
        assertEquals(new BigDecimal("1000.00"), project.getSubmittedAmount());
        assertEquals("张造价", project.getProjectManagerName());
        assertEquals(103L, project.getOwnerDeptId());
        verify(userService).checkUserDataScope(10L);
    }

    @Test
    void deletionChecksEachProjectAgainstCurrentDataScope()
    {
        when(accessService.selectAccessibleProject(any(CostProject.class))).thenReturn(new CostProject());
        when(projectMapper.deleteCostProjectByIds(any(Long[].class), anyString())).thenReturn(2);

        assertEquals(2, projectService.deleteCostProjectByIds(new Long[] { 1L, 2L }, "admin"));
        verify(projectMapper).deleteCostProjectByIds(new Long[] { 1L, 2L }, "admin");
    }

    @Test
    void deletionRejectsProjectWithActiveFiles()
    {
        when(accessService.selectAccessibleProject(any(CostProject.class))).thenReturn(new CostProject());
        when(projectFileMapper.countByProjectId(1L)).thenReturn(1);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> projectService.deleteCostProjectByIds(new Long[] { 1L }, "admin"));

        assertEquals("项目存在文件，请先删除项目文件", exception.getMessage());
    }

    @Test
    void statisticsUsesWeightedReductionRate()
    {
        CostProjectStatisticsVo raw = new CostProjectStatisticsVo();
        raw.setProjectCount(2L);
        raw.setSubmittedAmount(new BigDecimal("300.00"));
        raw.setApprovedAmount(new BigDecimal("250.00"));
        raw.setReductionAmount(new BigDecimal("50.00"));
        when(projectMapper.selectProjectStatistics(any(CostProject.class))).thenReturn(raw);

        CostProjectStatisticsVo result = projectService.selectProjectStatistics(new CostProject());

        assertEquals(new BigDecimal("0.166667"), result.getAverageReductionRate());
    }

    private CostProject validProject()
    {
        CostProject project = new CostProject();
        project.setProjectCode(" COST-001 ");
        project.setProjectName("测试项目");
        project.setProjectType("RESIDENTIAL");
        project.setProfessionalType("COMPREHENSIVE");
        project.setProjectStage("ESTIMATE");
        project.setProjectStatus(CostProjectStatus.PREPARING);
        project.setProjectManagerId(10L);
        project.setSubmittedAmount(new BigDecimal("1000"));
        project.setApprovedAmount(new BigDecimal("875"));
        project.setReductionAmount(new BigDecimal("125"));
        return project;
    }
}
