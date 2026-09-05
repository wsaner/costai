package com.ruoyi.cost.ai.log.service;

import java.util.List;
import com.ruoyi.cost.ai.log.domain.AiRequestLog;

public interface AiRequestLogService
{
    void record(AiRequestLog requestLog);
    List<AiRequestLog> selectList(AiRequestLog query);
    AiRequestLog selectById(Long id);
}
