package com.ruoyi.cost.ai.model.protocol;

/** 已脱敏、可写入 ai_request_log 的 Provider 异常。 */
public class AiProviderException extends RuntimeException
{
    private static final long serialVersionUID = 1L;
    private final String errorCode;

    public AiProviderException(String errorCode, String message)
    {
        super(message);
        this.errorCode = errorCode;
    }

    public AiProviderException(String errorCode, String message, Throwable cause)
    {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
