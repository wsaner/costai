package com.ruoyi.web.controller.cost;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.cost.boq.match.domain.CostBoqCompare;
import com.ruoyi.cost.boq.match.dto.BoqCompareRequest;
import com.ruoyi.cost.boq.match.dto.BoqManualMatchRequest;
import com.ruoyi.cost.boq.match.service.BoqMatchingService;

/** 工程量清单对比与人工匹配。 */
@RestController
@RequestMapping("/cost/boq/compares")
@Tag(name = "工程量清单对比")
public class CostBoqCompareController extends BaseController
{
    private final BoqMatchingService matchingService;

    public CostBoqCompareController(BoqMatchingService matchingService)
    {
        this.matchingService = matchingService;
    }

    @PreAuthorize("@ss.hasPermi('cost:compare:start')")
    @Log(title = "清单自动匹配", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "首次执行两个清单批次的非AI匹配")
    public AjaxResult start(@Validated @RequestBody BoqCompareRequest request)
    {
        return success(matchingService.startMatch(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('cost:compare:start')")
    @Log(title = "清单重新匹配", businessType = BusinessType.UPDATE)
    @PostMapping("/rematch")
    @Operation(summary = "保留人工结果并重新生成自动匹配")
    public AjaxResult rematch(@Validated @RequestBody BoqCompareRequest request)
    {
        return success(matchingService.rematch(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('cost:compare:list')")
    @GetMapping
    @Operation(summary = "分页查询清单对比结果")
    public TableDataInfo list(CostBoqCompare query)
    {
        startPage();
        return getDataTable(matchingService.selectCompareList(query));
    }

    @PreAuthorize("@ss.hasPermi('cost:compare:list')")
    @GetMapping("/summary")
    @Operation(summary = "查询清单对比汇总")
    public AjaxResult summary(@Validated BoqCompareRequest request)
    {
        return success(matchingService.selectSummary(request));
    }

    @PreAuthorize("@ss.hasPermi('cost:compare:manual')")
    @Log(title = "清单人工匹配", businessType = BusinessType.UPDATE)
    @PutMapping("/manual")
    @Operation(summary = "人工指定左侧清单与右侧清单匹配")
    public AjaxResult manual(@Validated @RequestBody BoqManualMatchRequest request)
    {
        return success(matchingService.manualMatch(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('cost:compare:manual')")
    @Log(title = "取消清单匹配", businessType = BusinessType.UPDATE)
    @PutMapping("/{compareId}/unmatch")
    @Operation(summary = "取消自动或人工匹配并恢复为左右未匹配")
    public AjaxResult unmatch(@PathVariable Long compareId)
    {
        return success(matchingService.unmatch(compareId, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('cost:compare:list')")
    @GetMapping("/batch-options/{projectId}")
    @Operation(summary = "查询当前项目可对比的清单批次")
    public AjaxResult batchOptions(@PathVariable Long projectId)
    {
        return success(matchingService.selectBatchOptions(projectId));
    }

    @PreAuthorize("@ss.hasPermi('cost:compare:manual')")
    @GetMapping("/item-options")
    @Operation(summary = "分页搜索人工匹配候选清单")
    public TableDataInfo itemOptions(@RequestParam Long projectId, @RequestParam Long batchId,
            @RequestParam(required = false) String keyword)
    {
        startPage();
        return getDataTable(matchingService.selectItemOptions(projectId, batchId, keyword));
    }
}
