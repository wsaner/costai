package com.ruoyi.cost.file.support;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Component;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.file.FileUtils;

/** 将数据库相对路径安全解析到现有 profile/private/project 目录。 */
@Component
public class CostProjectFilePathResolver
{
    public String uploadBasePath()
    {
        return RuoYiConfig.getProfile() + "/private/project";
    }

    public Path resolve(String storagePath)
    {
        Path profile = Paths.get(RuoYiConfig.getProfile()).toAbsolutePath().normalize();
        Path privateRoot = profile.resolve("private/project").normalize();
        String relative = FileUtils.stripPrefix(storagePath).replaceFirst("^[\\\\/]+", "");
        Path resolved = profile.resolve(relative).normalize();
        if (!resolved.startsWith(privateRoot))
        {
            throw new ServiceException("项目文件存储路径无效");
        }
        return resolved;
    }
}
