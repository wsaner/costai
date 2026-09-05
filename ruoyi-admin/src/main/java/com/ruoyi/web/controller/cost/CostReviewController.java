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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.cost.review.domain.CostReviewTask;
import com.ruoyi.cost.review.domain.CostReviewIssue;
import com.ruoyi.cost.review.dto.ReviewIssueHandleRequest;
import com.ruoyi.cost.review.dto.ReviewStartRequest;
import com.ruoyi.cost.review.dto.RuleConfigUpdateRequest;
import com.ruoyi.cost.review.dto.CostReviewAiRequest;
import com.ruoyi.cost.review.service.CostReviewAiService;
import com.ruoyi.cost.review.service.CostReviewService;
import com.ruoyi.cost.review.service.ReviewRuleConfigService;

/** 纯Java造价审核规则执行与结果查询。 */
@RestController
@RequestMapping("/cost/review")
@Tag(name = "造价审核规则")
public class CostReviewController extends BaseController
{
    private final CostReviewService reviewService;
    private final ReviewRuleConfigService configService;
    private final CostReviewAiService reviewAiService;

    public CostReviewController(CostReviewService reviewService, ReviewRuleConfigService configService,
            CostReviewAiService reviewAiService)
    {
        this.reviewService = reviewService;
        this.configService = configService;
        this.reviewAiService = reviewAiService;
    }

    @PreAuthorize("@ss.hasPermi('cost:review:start')")
    @Log(title = "造价规则审核", businessType = BusinessType.INSERT)
    @PostMapping("/tasks")
    @Operation(summary = "对两个已匹配清单批次执行纯Java规则审核")
    public AjaxResult start(@Validated @RequestBody ReviewStartRequest request)
    {
        return success(reviewService.startReview(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('cost:review:list')")
    @GetMapping("/tasks")
    @Operation(summary = "分页查询项目审核任务")
    public TableDataInfo list(CostReviewTask query)
    {
        startPage();
        return getDataTable(reviewService.selectTaskList(query));
    }

    @PreAuthorize("@ss.hasPermi('cost:review:query')")
    @GetMapping("/tasks/{reviewTaskId}")
    @Operation(summary = "查询审核任务详情及问题统计")
    public AjaxResult info(@PathVariable Long reviewTaskId)
    {
        return success(reviewService.selectTaskById(reviewTaskId));
    }

    @PreAuthorize("@ss.hasPermi('cost:review:list')")
    @GetMapping("/tasks/{reviewTaskId}/issues")
    @Operation(summary = "分页查询审核问题")
    public TableDataInfo issues(@PathVariable Long reviewTaskId, CostReviewIssue query)
    {
        query.setReviewTaskId(reviewTaskId);
        startPage();
        return getDataTable(reviewService.selectIssueList(query));
    }

    @PreAuthorize("@ss.hasPermi('cost:review:query')")
    @GetMapping("/issues/{issueId}")
    @Operation(summary = "查询审核问题详情和规则证据")
    public AjaxResult issueInfo(@PathVariable Long issueId)
    {
        return success(reviewService.selectIssueById(issueId));
    }

    @PreAuthorize("@ss.hasPermi('cost:review:handle')")
    @Log(title = "审核问题处理", businessType = BusinessType.UPDATE)
    @PutMapping("/issues/{issueId}")
    @Operation(summary = "确认、忽略、整改问题或调整风险等级")
    public AjaxResult handleIssue(@PathVariable Long issueId,
            @Validated @RequestBody ReviewIssueHandleRequest request)
    {
        return success(reviewService.handleIssue(issueId, request, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('cost:review:ai')")
    @Log(title = "审核问题AI分析", businessType = BusinessType.UPDATE,
            isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/issues/{issueId}/ai-analysis")
    @Operation(summary = "对单个规则候选执行有限上下文AI语义分析")
    public AjaxResult analyzeIssue(@PathVariable Long issueId,
            @Validated @RequestBody(required = false) CostReviewAiRequest request)
    {
        return success(reviewAiService.analyzeIssue(issueId, request, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('cost:review:config')")
    @GetMapping("/rule-configs")
    @Operation(summary = "查询数据库化审核规则配置")
    public AjaxResult ruleConfigs()
    {
        return success(configService.selectConfigList());
    }

    @PreAuthorize("@ss.hasPermi('cost:review:config')")
    @Log(title = "审核规则配置", businessType = BusinessType.UPDATE)
    @PutMapping("/rule-configs/{configId}")
    @Operation(summary = "修改单个审核规则配置值")
    public AjaxResult updateRuleConfig(@PathVariable Long configId,
            @Validated @RequestBody RuleConfigUpdateRequest request)
    {
        return toAjax(configService.updateConfig(configId, request.getConfigValue(), getUsername()));
    }
}
