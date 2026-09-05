package com.ruoyi.cost.ai.log.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cost.ai.log.domain.AiRequestLog;

public interface AiRequestLogMapper
{
    int insertRequestLog(AiRequestLog requestLog);
    List<AiRequestLog> selectRequestLogList(AiRequestLog query);
    AiRequestLog selectRequestLogById(@Param("id") Long id);
}
