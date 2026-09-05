package com.ruoyi.web.controller.ai;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.cost.ai.log.domain.AiRequestLog;
import com.ruoyi.cost.ai.log.service.AiRequestLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/request-logs")
@Tag(name = "AI调用日志")
public class AiRequestLogController extends BaseController
{
    private final AiRequestLogService requestLogService;

    public AiRequestLogController(AiRequestLogService requestLogService)
    {
        this.requestLogService = requestLogService;
    }

    @PreAuthorize("@ss.hasPermi('ai:log:list')")
    @GetMapping("/list")
    @Operation(summary = "分页查询AI调用日志")
    public TableDataInfo list(AiRequestLog query)
    {
        startPage();
        return getDataTable(requestLogService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('ai:log:query')")
    @GetMapping("/{id}")
    @Operation(summary = "查询AI调用日志详情")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(requestLogService.selectById(id));
    }
}
