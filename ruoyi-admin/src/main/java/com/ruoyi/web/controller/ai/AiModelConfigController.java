package com.ruoyi.web.controller.ai;

import java.util.List;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.cost.ai.model.domain.AiModelConfig;
import com.ruoyi.cost.ai.model.dto.AiModelConfigSaveRequest;
import com.ruoyi.cost.ai.model.protocol.AiInvocationContext;
import com.ruoyi.cost.ai.model.service.AiModelConfigService;
import com.ruoyi.cost.ai.model.service.AiModelService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/model-configs")
@Tag(name = "AI模型配置")
public class AiModelConfigController extends BaseController
{
    private final AiModelConfigService configService;
    private final AiModelService modelService;

    public AiModelConfigController(AiModelConfigService configService, AiModelService modelService)
    {
        this.configService = configService;
        this.modelService = modelService;
    }

    @PreAuthorize("@ss.hasPermi('ai:model:list')")
    @GetMapping("/list")
    @Operation(summary = "分页查询AI模型配置")
    public TableDataInfo list(AiModelConfig query)
    {
        startPage();
        return getDataTable(configService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('ai:model:query')")
    @GetMapping("/{id}")
    @Operation(summary = "查询AI模型配置详情")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(configService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('ai:model:add')")
    @Log(title = "AI模型配置", businessType = BusinessType.INSERT,
            isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping
    @Operation(summary = "新增AI模型配置")
    public AjaxResult add(@Validated @RequestBody AiModelConfigSaveRequest request)
    {
        return success(configService.create(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('ai:model:edit')")
    @Log(title = "AI模型配置", businessType = BusinessType.UPDATE,
            isSaveRequestData = false, isSaveResponseData = false)
    @PutMapping
    @Operation(summary = "修改AI模型配置")
    public AjaxResult edit(@Validated @RequestBody AiModelConfigSaveRequest request)
    {
        return toAjax(configService.update(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('ai:model:remove')")
    @Log(title = "AI模型配置", businessType = BusinessType.DELETE, isSaveResponseData = false)
    @DeleteMapping("/{ids}")
    @Operation(summary = "删除AI模型配置")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        int rows = 0;
        for (Long id : ids) rows += configService.delete(id, getUsername());
        return toAjax(rows);
    }

    @PreAuthorize("@ss.hasPermi('ai:model:test')")
    @Log(title = "AI模型连接测试", businessType = BusinessType.OTHER,
            isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/{id}/test")
    @Operation(summary = "测试AI模型连接")
    public AjaxResult test(@PathVariable Long id)
    {
        AiInvocationContext context = new AiInvocationContext(getUserId(), getUsername(),
                "MODEL_CONFIG", String.valueOf(id));
        return success(modelService.testConnection(id, context));
    }
}
