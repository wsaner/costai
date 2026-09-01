package com.ruoyi.cost.file.vo;

import java.nio.file.Path;

/** 仅供下载控制器使用的已授权文件信息。 */
public class CostProjectFileDownloadVo
{
    private final Path path;
    private final String originalName;
    private final String mimeType;

    public CostProjectFileDownloadVo(Path path, String originalName, String mimeType)
    {
        this.path = path;
        this.originalName = originalName;
        this.mimeType = mimeType;
    }

    public Path getPath() { return path; }
    public String getOriginalName() { return originalName; }
    public String getMimeType() { return mimeType; }
}
