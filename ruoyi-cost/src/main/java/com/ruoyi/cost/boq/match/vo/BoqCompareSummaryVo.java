package com.ruoyi.cost.boq.match.vo;

import java.math.BigDecimal;

/** 清单对比状态汇总。 */
public class BoqCompareSummaryVo
{
    private long totalCount;
    private long matchedCount;
    private long exactCount;
    private long highSimilarityCount;
    private long lowSimilarityCount;
    private long onlyLeftCount;
    private long onlyRightCount;
    private long manualCount;
    private BigDecimal averageMatchScore = BigDecimal.ZERO;

    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
    public long getMatchedCount() { return matchedCount; }
    public void setMatchedCount(long matchedCount) { this.matchedCount = matchedCount; }
    public long getExactCount() { return exactCount; }
    public void setExactCount(long exactCount) { this.exactCount = exactCount; }
    public long getHighSimilarityCount() { return highSimilarityCount; }
    public void setHighSimilarityCount(long highSimilarityCount) { this.highSimilarityCount = highSimilarityCount; }
    public long getLowSimilarityCount() { return lowSimilarityCount; }
    public void setLowSimilarityCount(long lowSimilarityCount) { this.lowSimilarityCount = lowSimilarityCount; }
    public long getOnlyLeftCount() { return onlyLeftCount; }
    public void setOnlyLeftCount(long onlyLeftCount) { this.onlyLeftCount = onlyLeftCount; }
    public long getOnlyRightCount() { return onlyRightCount; }
    public void setOnlyRightCount(long onlyRightCount) { this.onlyRightCount = onlyRightCount; }
    public long getManualCount() { return manualCount; }
    public void setManualCount(long manualCount) { this.manualCount = manualCount; }
    public BigDecimal getAverageMatchScore() { return averageMatchScore; }
    public void setAverageMatchScore(BigDecimal averageMatchScore) { this.averageMatchScore = averageMatchScore; }
}
