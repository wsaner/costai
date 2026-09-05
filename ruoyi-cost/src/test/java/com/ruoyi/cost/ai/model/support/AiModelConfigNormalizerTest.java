package com.ruoyi.cost.ai.model.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

class AiModelConfigNormalizerTest
{
    @Test
    void normalizesCompatibleConfiguration()
    {
        assertEquals("OPENAI_COMPATIBLE",
                AiModelConfigNormalizer.normalizeProvider(" openai_compatible "));
        assertEquals("https://gateway.example.com/v1",
                AiModelConfigNormalizer.normalizeBaseUrl("https://gateway.example.com/v1/"));
        assertEquals("****cdef", AiModelConfigNormalizer.keyHint("abcdef"));
    }

    @Test
    void rejectsUnsafeOrUnsupportedBaseUrl()
    {
        assertThrows(ServiceException.class,
                () -> AiModelConfigNormalizer.normalizeBaseUrl("https://user:pass@example.com/v1"));
        assertThrows(ServiceException.class,
                () -> AiModelConfigNormalizer.normalizeBaseUrl("https://example.com/v1?key=secret"));
        assertThrows(ServiceException.class,
                () -> AiModelConfigNormalizer.normalizeBaseUrl("file:///tmp/model"));
    }
}
