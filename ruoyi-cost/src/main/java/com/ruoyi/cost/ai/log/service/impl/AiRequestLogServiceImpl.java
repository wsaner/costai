package com.ruoyi.cost.ai.log.service.impl;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.cost.ai.log.domain.AiRequestLog;
import com.ruoyi.cost.ai.log.mapper.AiRequestLogMapper;
import com.ruoyi.cost.ai.log.service.AiRequestLogService;

@Service
public class AiRequestLogServiceImpl implements AiRequestLogService
{
    private static final Logger log = LoggerFactory.getLogger(AiRequestLogServiceImpl.class);
    private final AiRequestLogMapper mapper;

    public AiRequestLogServiceImpl(AiRequestLogMapper mapper) { this.mapper = mapper; }

    @Override
    public void record(AiRequestLog requestLog)
    {
        try { mapper.insertRequestLog(requestLog); }
        catch (RuntimeException e) { log.error("AI 请求审计日志写入失败"); }
    }

    @Override
    public List<AiRequestLog> selectList(AiRequestLog query)
    {
        return mapper.selectRequestLogList(query == null ? new AiRequestLog() : query);
    }

    @Override
    public AiRequestLog selectById(Long id)
    {
        AiRequestLog requestLog = mapper.selectRequestLogById(id);
        if (requestLog == null) throw new ServiceException("AI 调用日志不存在");
        return requestLog;
    }
}
