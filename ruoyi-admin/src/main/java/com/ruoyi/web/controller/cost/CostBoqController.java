package com.ruoyi.web.controller.cost;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.cost.boq.domain.CostBoqBatch;
import com.ruoyi.cost.boq.domain.CostBoqItem;
import com.ruoyi.cost.boq.dto.CostBoqImportRequest;
import com.ruoyi.cost.boq.service.CostBoqImportService;
import com.ruoyi.cost.boq.service.CostBoqService;

/** 工程量清单正式导入与管理。 */
@RestController
@RequestMapping("/cost/boq")
@Tag(name = "工程量清单管理")
public class CostBoqController extends BaseController
{
    private final CostBoqImportService importService;
    private final CostBoqService boqService;

    public CostBoqController(CostBoqImportService importService, CostBoqService boqService)
    {
        this.importService = importService;
        this.boqService = boqService;
    }

    @PreAuthorize("@ss.hasPermi('cost:boq:import') and @ss.hasPermi('cost:file:query')")
    @Log(title = "工程量清单导入", businessType = BusinessType.IMPORT)
    @PostMapping("/imports/{projectId}")
    @Operation(summary = "按已确认字段映射正式导入清单")
    public AjaxResult importBoq(@PathVariable Long projectId,
            @Validated @RequestBody CostBoqImportRequest request)
    {
        return success(importService.importBoq(projectId, request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('cost:boq:list')")
    @GetMapping("/batches")
    @Operation(summary = "分页查询项目清单批次")
    public TableDataInfo batches(CostBoqBatch query)
    {
        startPage();
        return getDataTable(boqService.selectBatchList(query));
    }

    @PreAuthorize("@ss.hasPermi('cost:boq:query')")
    @GetMapping("/batches/{batchId}")
    @Operation(summary = "查询清单批次详情")
    public AjaxResult batchInfo(@PathVariable Long batchId)
    {
        return success(boqService.selectBatchById(batchId));
    }

    @PreAuthorize("@ss.hasPermi('cost:boq:list')")
    @GetMapping("/items")
    @Operation(summary = "分页查询清单明细")
    public TableDataInfo items(CostBoqItem query)
    {
        startPage();
        return getDataTable(boqService.selectItemList(query));
    }

    @PreAuthorize("@ss.hasPermi('cost:boq:query')")
    @GetMapping("/batches/{batchId}/errors")
    @Operation(summary = "分页查询导入错误行")
    public TableDataInfo errors(@PathVariable Long batchId)
    {
        startPage();
        return getDataTable(boqService.selectErrorList(batchId));
    }

    @PreAuthorize("@ss.hasPermi('cost:boq:remove')")
    @Log(title = "工程量清单批次", businessType = BusinessType.DELETE)
    @DeleteMapping("/batches/{batchId}")
    @Operation(summary = "删除批次并同步删除清单与错误行")
    public AjaxResult removeBatch(@PathVariable Long batchId)
    {
        return toAjax(boqService.deleteBatch(batchId, getUsername()));
    }
}
