package com.ruoyi.cost.boq.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cost.boq.domain.CostBoqItem;

/** 清单明细数据访问层。 */
public interface CostBoqItemMapper
{
    List<CostBoqItem> selectItemList(CostBoqItem query);
    CostBoqItem selectItemById(@Param("id") Long id);
    int batchInsert(@Param("items") List<CostBoqItem> items);
    int deleteByBatchId(@Param("batchId") Long batchId, @Param("updateBy") String updateBy);
}
