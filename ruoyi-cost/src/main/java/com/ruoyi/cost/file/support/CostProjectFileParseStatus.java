package com.ruoyi.cost.file.support;

import java.util.Set;

/** AI解析状态及首个状态判定。 */
public final class CostProjectFileParseStatus
{
    public static final String WAITING = "WAITING";
    public static final String PARSING = "PARSING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String UNSUPPORTED = "UNSUPPORTED";

    private static final Set<String> STORAGE_ONLY_EXTENSIONS = Set.of("dwg", "dxf", "ifc");

    private CostProjectFileParseStatus() { }

    public static String initialStatus(String extension)
    {
        return STORAGE_ONLY_EXTENSIONS.contains(extension) ? UNSUPPORTED : WAITING;
    }
}
