package com.ruoyi.cost.ai.model.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;

/** 使用部署环境独立密钥进行 AES-256-GCM 加解密。 */
@Component
public class AiSecretCipher
{
    private static final String PREFIX = "v1:";
    private static final int IV_LENGTH = 12;
    private final byte[] key;
    private final SecureRandom secureRandom = new SecureRandom();

    public AiSecretCipher(@Value("${ai.security.encryption-key:}") String encodedKey)
    {
        this.key = decodeKey(encodedKey);
    }

    public String encrypt(String plainText)
    {
        if (plainText == null || plainText.isBlank()) return null;
        ensureConfigured();
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        try
        {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        }
        catch (GeneralSecurityException e)
        {
            throw new ServiceException("AI 密钥加密失败，请检查服务端安全配置");
        }
    }

    public String decrypt(String cipherText)
    {
        if (cipherText == null || cipherText.isBlank()) return null;
        ensureConfigured();
        if (!cipherText.startsWith(PREFIX)) throw new ServiceException("AI 密钥密文版本不受支持");
        try
        {
            byte[] combined = Base64.getDecoder().decode(cipherText.substring(PREFIX.length()));
            if (combined.length <= IV_LENGTH) throw new GeneralSecurityException("invalid cipher");
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        }
        catch (GeneralSecurityException | IllegalArgumentException e)
        {
            throw new ServiceException("AI 密钥解密失败，请检查服务端安全配置");
        }
    }

    private void ensureConfigured()
    {
        if (key == null) throw new ServiceException(
                "未配置 AI_CONFIG_ENCRYPTION_KEY，无法保存或使用 API Key");
    }

    private static byte[] decodeKey(String encodedKey)
    {
        if (encodedKey == null || encodedKey.isBlank()) return null;
        try
        {
            byte[] decoded = Base64.getDecoder().decode(encodedKey.trim());
            if (decoded.length != 32) throw new IllegalArgumentException("length");
            return decoded;
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalStateException("AI_CONFIG_ENCRYPTION_KEY 必须是 Base64 编码的 32 字节密钥");
        }
    }
}
