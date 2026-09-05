package com.ruoyi.cost.boq.match.dto;

import jakarta.validation.constraints.NotNull;

/** 人工指定左右清单匹配。 */
public class BoqManualMatchRequest extends BoqCompareRequest
{
    @NotNull(message = "左侧清单不能为空")
    private Long leftItemId;
    @NotNull(message = "右侧清单不能为空")
    private Long rightItemId;

    public Long getLeftItemId() { return leftItemId; }
    public void setLeftItemId(Long leftItemId) { this.leftItemId = leftItemId; }
    public Long getRightItemId() { return rightItemId; }
    public void setRightItemId(Long rightItemId) { this.rightItemId = rightItemId; }
}
