package com.ruoyi.cost.boq.service;

import com.ruoyi.cost.boq.dto.CostBoqImportRequest;
import com.ruoyi.cost.boq.vo.CostBoqImportResultVo;

/** 正式清单导入。 */
public interface CostBoqImportService
{
    CostBoqImportResultVo importBoq(Long projectId, CostBoqImportRequest request, String operator);
}
