package com.ruoyi.cost.ai.model.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.cost.ai.log.domain.AiRequestLog;
import com.ruoyi.cost.ai.model.domain.AiModelConfig;
import org.junit.jupiter.api.Test;

class AiSecurityContractTest
{
    @Test
    void modelJsonNeverContainsCipherText() throws Exception
    {
        AiModelConfig config = new AiModelConfig();
        config.setId(1L);
        config.setName("safe");
        config.setApiKeyEncrypted("v1:ciphertext");
        config.setApiKeyHint("****1234");
        String json = new ObjectMapper().writeValueAsString(config);
        assertFalse(json.contains("ciphertext"));
        assertFalse(json.contains("apiKeyEncrypted"));
        assertTrue(json.contains("apiKeyHint"));
    }

    @Test
    void requestAuditSchemaHasNoPromptResponseOrSecretFields()
    {
        Set<String> fields = Arrays.stream(AiRequestLog.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());
        assertFalse(fields.contains("apiKey"));
        assertFalse(fields.contains("prompt"));
        assertFalse(fields.contains("response"));
        assertFalse(fields.contains("requestBody"));
    }
}
