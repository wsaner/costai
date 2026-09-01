package com.ruoyi.cost.project.support;

import java.util.Set;

/** 项目状态字典值约束。 */
public final class CostProjectStatus
{
    public static final String PREPARING = "PREPARING";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String UNDER_REVIEW = "UNDER_REVIEW";
    public static final String COMPLETED = "COMPLETED";
    public static final String ARCHIVED = "ARCHIVED";

    private static final Set<String> VALUES = Set.of(PREPARING, IN_PROGRESS, UNDER_REVIEW, COMPLETED, ARCHIVED);

    private CostProjectStatus()
    {
    }

    public static boolean isValid(String status)
    {
        return VALUES.contains(status);
    }
}
