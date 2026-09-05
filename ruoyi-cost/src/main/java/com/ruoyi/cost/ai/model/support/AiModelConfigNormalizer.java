package com.ruoyi.cost.ai.model.support;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import com.ruoyi.common.exception.ServiceException;

public final class AiModelConfigNormalizer
{
    public static final String OPENAI_COMPATIBLE = "OPENAI_COMPATIBLE";

    private AiModelConfigNormalizer() {}

    public static String normalizeProvider(String providerType)
    {
        String value = trim(providerType).toUpperCase(Locale.ROOT);
        if (!OPENAI_COMPATIBLE.equals(value)) throw new ServiceException("当前仅支持 OpenAI Compatible");
        return value;
    }

    public static String normalizeBaseUrl(String baseUrl)
    {
        String value = trim(baseUrl);
        try
        {
            URI uri = new URI(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null)
                throw new ServiceException("基础地址必须是无账号、查询参数和片段的 HTTP(S) 地址");
            while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
            return value;
        }
        catch (URISyntaxException e)
        {
            throw new ServiceException("基础地址格式不正确");
        }
    }

    public static String keyHint(String apiKey)
    {
        if (apiKey == null || apiKey.isBlank()) return null;
        String value = apiKey.trim();
        return "****" + value.substring(Math.max(0, value.length() - 4));
    }

    public static String trim(String value) { return value == null ? "" : value.trim(); }
}
