package com.ruoyi.cost.boq.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cost.boq.domain.CostBoqBatch;

/** 清单批次数据访问层。 */
public interface CostBoqBatchMapper
{
    List<CostBoqBatch> selectBatchList(CostBoqBatch query);
    CostBoqBatch selectBatchById(@Param("id") Long id);
    Long lockBatchById(@Param("id") Long id);
    int insertBatch(CostBoqBatch batch);
    int updateImportResult(@Param("id") Long id, @Param("totalCount") int totalCount,
            @Param("successCount") int successCount, @Param("failCount") int failCount,
            @Param("totalAmount") BigDecimal totalAmount, @Param("importStatus") String importStatus,
            @Param("errorSummary") String errorSummary, @Param("updateBy") String updateBy);
    int deleteBatch(@Param("id") Long id, @Param("updateBy") String updateBy);
    int countBySourceFileId(@Param("sourceFileId") Long sourceFileId);
}
