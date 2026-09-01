package com.ruoyi.web.controller.cost;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.cost.project.domain.CostProject;
import com.ruoyi.cost.project.dto.CostProjectStatusRequest;
import com.ruoyi.cost.project.service.ICostProjectService;

/**
 * 造价项目管理。
 */
@RestController
@RequestMapping("/cost/project")
@Tag(name = "造价项目管理")
public class CostProjectController extends BaseController
{
    private final ICostProjectService projectService;

    public CostProjectController(ICostProjectService projectService)
    {
        this.projectService = projectService;
    }

    /** 项目分页查询。 */
    @PreAuthorize("@ss.hasPermi('cost:project:list')")
    @GetMapping("/list")
    @Operation(summary = "分页查询造价项目")
    public TableDataInfo list(CostProject project)
    {
        startPage();
        List<CostProject> projects = projectService.selectCostProjectList(project);
        return getDataTable(projects);
    }

    /** 项目详情。 */
    @PreAuthorize("@ss.hasPermi('cost:project:query')")
    @GetMapping("/{id}")
    @Operation(summary = "查询造价项目详情")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(projectService.selectCostProjectById(id));
    }

    /** 新增项目。 */
    @PreAuthorize("@ss.hasPermi('cost:project:add')")
    @Log(title = "造价项目", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "新增造价项目")
    public AjaxResult add(@Validated @RequestBody CostProject project)
    {
        project.setCreateBy(getUsername());
        return toAjax(projectService.insertCostProject(project));
    }

    /** 修改项目。 */
    @PreAuthorize("@ss.hasPermi('cost:project:edit')")
    @Log(title = "造价项目", businessType = BusinessType.UPDATE)
    @PutMapping
    @Operation(summary = "修改造价项目")
    public AjaxResult edit(@Validated @RequestBody CostProject project)
    {
        project.setUpdateBy(getUsername());
        return toAjax(projectService.updateCostProject(project));
    }

    /** 删除项目（逻辑删除）。 */
    @PreAuthorize("@ss.hasPermi('cost:project:remove')")
    @Log(title = "造价项目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    @Operation(summary = "批量删除造价项目")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(projectService.deleteCostProjectByIds(ids, getUsername()));
    }

    /** 单独修改项目状态。 */
    @PreAuthorize("@ss.hasPermi('cost:project:edit')")
    @Log(title = "造价项目状态", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    @Operation(summary = "修改造价项目状态")
    public AjaxResult changeStatus(@Validated @RequestBody CostProjectStatusRequest request)
    {
        return toAjax(projectService.changeProjectStatus(
                request.getId(), request.getProjectStatus(), getUsername()));
    }

    /** 当前数据权限范围内的项目概览。 */
    @PreAuthorize("@ss.hasPermi('cost:project:list')")
    @GetMapping("/statistics")
    @Operation(summary = "查询造价项目概览统计")
    public AjaxResult statistics(CostProject project)
    {
        return success(projectService.selectProjectStatistics(project));
    }

    /** 当前用户可选择的项目负责人。 */
    @PreAuthorize("@ss.hasPermi('cost:project:list')")
    @GetMapping("/managerOptions")
    @Operation(summary = "查询可选项目负责人")
    public AjaxResult managerOptions(@RequestParam(required = false) String keyword)
    {
        return success(projectService.selectProjectManagerOptions(keyword));
    }
}
