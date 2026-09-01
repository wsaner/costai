package com.ruoyi.cost.file.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;

/** 项目文件扩展名、大小和常见文件签名校验。 */
@Component
public class CostProjectFileValidator
{
    public static final String[] ALLOWED_EXTENSIONS = {
            "xlsx", "xls", "csv", "pdf", "doc", "docx", "txt",
            "png", "jpg", "jpeg", "zip", "dwg", "dxf", "ifc"
    };

    public String validate(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("上传文件不能为空");
        }
        String originalName = FilenameUtils.getName(file.getOriginalFilename());
        if (StringUtils.isBlank(originalName))
        {
            throw new ServiceException("文件名不能为空");
        }
        try
        {
            FileUploadUtils.assertAllowed(file, ALLOWED_EXTENSIONS);
            String extension = FilenameUtils.getExtension(originalName).toLowerCase(Locale.ROOT);
            validateSignature(file, extension);
            return extension;
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("文件校验失败：" + e.getMessage());
        }
    }

    private void validateSignature(MultipartFile file, String extension) throws IOException
    {
        byte[] header = new byte[8192];
        int length;
        try (InputStream input = file.getInputStream())
        {
            length = input.read(header);
        }
        boolean valid = switch (extension)
        {
            case "pdf" -> startsWith(header, length, new byte[] { '%', 'P', 'D', 'F' });
            case "png" -> startsWith(header, length,
                    new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A });
            case "jpg", "jpeg" -> startsWith(header, length,
                    new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });
            case "xlsx" -> startsWith(header, length, new byte[] { 'P', 'K' })
                    || startsWith(header, length,
                            new byte[] { (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0 });
            case "docx", "zip" -> startsWith(header, length, new byte[] { 'P', 'K' });
            case "xls", "doc" -> startsWith(header, length,
                    new byte[] { (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0 });
            case "txt", "csv" -> !containsNullByte(header, length);
            default -> true;
        };
        if (!valid)
        {
            throw new ServiceException("文件内容与扩展名不匹配");
        }
    }

    private boolean containsNullByte(byte[] value, int length)
    {
        for (int i = 0; i < length; i++)
        {
            if (value[i] == 0)
            {
                return true;
            }
        }
        return false;
    }

    private boolean startsWith(byte[] value, int length, byte[] prefix)
    {
        if (length < prefix.length)
        {
            return false;
        }
        for (int i = 0; i < prefix.length; i++)
        {
            if (value[i] != prefix[i])
            {
                return false;
            }
        }
        return true;
    }
}
