package org.os4all.modules.baseline.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Baseline statistics and personal deviation profile for an individual health metric.
 * "Normal for the population is not necessarily normal for the individual."
 */
public class BaselineMetric {

    private String metric;
    private String unit;
    private boolean baselineEstablished;
    private int observationCount;
    private int requiredObservations;
    private String statusMessage;

    // Core statistical estimators
    private BigDecimal mean;
    private BigDecimal median;
    private BigDecimal standardDeviation;
    private BigDecimal min;
    private BigDecimal max;

    // Baseline normal operational range (e.g. [mean - 1.96*std, mean + 1.96*std] or IQR)
    private BigDecimal baselineRangeLow;
    private BigDecimal baselineRangeHigh;

    // Recent average (e.g., last 3 observations or last 48 hours)
    private BigDecimal recentAverage;
    private int recentObservationCount;

    // Deviation from personal baseline
    private BigDecimal deviationFromBaseline;
    private BigDecimal deviationPercentage;
    private Double zScore; // Null when statistically inappropriate (e.g. std dev is 0 or count too small)

    // Directional trend and personal deviation commentary
    private String trendDirection; // "STABLE", "INCREASING", "DECREASING", "INSUFFICIENT_DATA"
    private String interpretation; // Explains deviation strictly relative to individual's history
    private Instant lastObservedAt;

    public BaselineMetric() {
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isBaselineEstablished() {
        return baselineEstablished;
    }

    public void setBaselineEstablished(boolean baselineEstablished) {
        this.baselineEstablished = baselineEstablished;
    }

    public int getObservationCount() {
        return observationCount;
    }

    public void setObservationCount(int observationCount) {
        this.observationCount = observationCount;
    }

    public int getRequiredObservations() {
        return requiredObservations;
    }

    public void setRequiredObservations(int requiredObservations) {
        this.requiredObservations = requiredObservations;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public BigDecimal getMean() {
        return mean;
    }

    public void setMean(BigDecimal mean) {
        this.mean = mean;
    }

    public BigDecimal getMedian() {
        return median;
    }

    public void setMedian(BigDecimal median) {
        this.median = median;
    }

    public BigDecimal getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(BigDecimal standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    public BigDecimal getMin() {
        return min;
    }

    public void setMin(BigDecimal min) {
        this.min = min;
    }

    public BigDecimal getMax() {
        return max;
    }

    public void setMax(BigDecimal max) {
        this.max = max;
    }

    public BigDecimal getBaselineRangeLow() {
        return baselineRangeLow;
    }

    public void setBaselineRangeLow(BigDecimal baselineRangeLow) {
        this.baselineRangeLow = baselineRangeLow;
    }

    public BigDecimal getBaselineRangeHigh() {
        return baselineRangeHigh;
    }

    public void setBaselineRangeHigh(BigDecimal baselineRangeHigh) {
        this.baselineRangeHigh = baselineRangeHigh;
    }

    public BigDecimal getRecentAverage() {
        return recentAverage;
    }

    public void setRecentAverage(BigDecimal recentAverage) {
        this.recentAverage = recentAverage;
    }

    public int getRecentObservationCount() {
        return recentObservationCount;
    }

    public void setRecentObservationCount(int recentObservationCount) {
        this.recentObservationCount = recentObservationCount;
    }

    public BigDecimal getDeviationFromBaseline() {
        return deviationFromBaseline;
    }

    public void setDeviationFromBaseline(BigDecimal deviationFromBaseline) {
        this.deviationFromBaseline = deviationFromBaseline;
    }

    public BigDecimal getDeviationPercentage() {
        return deviationPercentage;
    }

    public void setDeviationPercentage(BigDecimal deviationPercentage) {
        this.deviationPercentage = deviationPercentage;
    }

    public Double getZScore() {
        return zScore;
    }

    public void setZScore(Double zScore) {
        this.zScore = zScore;
    }

    public String getTrendDirection() {
        return trendDirection;
    }

    public void setTrendDirection(String trendDirection) {
        this.trendDirection = trendDirection;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }

    public Instant getLastObservedAt() {
        return lastObservedAt;
    }

    public void setLastObservedAt(Instant lastObservedAt) {
        this.lastObservedAt = lastObservedAt;
    }
}
