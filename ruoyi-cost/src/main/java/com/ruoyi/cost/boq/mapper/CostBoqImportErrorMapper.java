package com.ruoyi.cost.boq.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cost.boq.domain.CostBoqImportError;

/** 清单导入错误行数据访问层。 */
public interface CostBoqImportErrorMapper
{
    List<CostBoqImportError> selectByBatchId(@Param("batchId") Long batchId);
    int batchInsert(@Param("errors") List<CostBoqImportError> errors);
    int deleteByBatchId(@Param("batchId") Long batchId, @Param("updateBy") String updateBy);
}
