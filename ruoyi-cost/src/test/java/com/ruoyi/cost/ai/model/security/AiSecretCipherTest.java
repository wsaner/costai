package com.ruoyi.cost.ai.model.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Base64;
import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

class AiSecretCipherTest
{
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptsWithRandomIvAndDecrypts()
    {
        AiSecretCipher cipher = new AiSecretCipher(KEY);
        String first = cipher.encrypt("sk-sensitive-value");
        String second = cipher.encrypt("sk-sensitive-value");
        assertNotEquals(first, second);
        assertEquals("sk-sensitive-value", cipher.decrypt(first));
        assertEquals("sk-sensitive-value", cipher.decrypt(second));
    }

    @Test
    void rejectsTamperedCipherTextAndMissingServerKey()
    {
        AiSecretCipher cipher = new AiSecretCipher(KEY);
        String encrypted = cipher.encrypt("secret");
        assertThrows(ServiceException.class,
                () -> cipher.decrypt(encrypted.substring(0, encrypted.length() - 2) + "AA"));
        assertThrows(ServiceException.class, () -> new AiSecretCipher("").encrypt("secret"));
    }
}
