package com.ruoyi.web.controller.ai;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.cost.ai.prompt.domain.AiPromptTemplate;
import com.ruoyi.cost.ai.prompt.dto.AiPromptTemplateSaveRequest;
import com.ruoyi.cost.ai.prompt.service.AiPromptTemplateService;
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
@RequestMapping("/ai/prompts")
@Tag(name = "AI Prompt模板")
public class AiPromptTemplateController extends BaseController
{
    private final AiPromptTemplateService promptService;

    public AiPromptTemplateController(AiPromptTemplateService promptService)
    {
        this.promptService = promptService;
    }

    @PreAuthorize("@ss.hasPermi('ai:prompt:list')")
    @GetMapping("/list")
    @Operation(summary = "分页查询Prompt模板")
    public TableDataInfo list(AiPromptTemplate query)
    {
        startPage();
        return getDataTable(promptService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('ai:prompt:query')")
    @GetMapping("/{id}")
    @Operation(summary = "查询Prompt模板详情")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(promptService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('ai:prompt:add')")
    @Log(title = "AI Prompt模板", businessType = BusinessType.INSERT,
            isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping
    @Operation(summary = "新增Prompt模板")
    public AjaxResult add(@Validated @RequestBody AiPromptTemplateSaveRequest request)
    {
        return success(promptService.create(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('ai:prompt:edit')")
    @Log(title = "AI Prompt模板", businessType = BusinessType.UPDATE,
            isSaveRequestData = false, isSaveResponseData = false)
    @PutMapping
    @Operation(summary = "修改Prompt模板")
    public AjaxResult edit(@Validated @RequestBody AiPromptTemplateSaveRequest request)
    {
        return toAjax(promptService.update(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('ai:prompt:remove')")
    @Log(title = "AI Prompt模板", businessType = BusinessType.DELETE,
            isSaveResponseData = false)
    @DeleteMapping("/{ids}")
    @Operation(summary = "删除Prompt模板")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        int rows = 0;
        for (Long id : ids) rows += promptService.delete(id, getUsername());
        return toAjax(rows);
    }
}
