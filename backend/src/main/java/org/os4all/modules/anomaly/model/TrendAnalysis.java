package org.os4all.modules.anomaly.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Trend analysis capturing longitudinal slopes, direction, persistence, and velocity across observation windows.
 */
public class TrendAnalysis {

    private String metric;
    private String unit;
    private String trendDirection; // "STABLE", "INCREASING", "DECREASING", "FLUCTUATING"
    private BigDecimal slope; // Velocity (units per day)
    private Double rSquared; // Correlation coefficient squared (linearity)
    private int windowDays;
    private int dataPointCount;
    private Instant windowStart;
    private Instant windowEnd;
    private List<String> correlatedSignals = new ArrayList<>();
    private String narrative;

    public TrendAnalysis() {
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

    public String getTrendDirection() {
        return trendDirection;
    }

    public void setTrendDirection(String trendDirection) {
        this.trendDirection = trendDirection;
    }

    public BigDecimal getSlope() {
        return slope;
    }

    public void setSlope(BigDecimal slope) {
        this.slope = slope;
    }

    public Double getRSquared() {
        return rSquared;
    }

    public void setRSquared(Double rSquared) {
        this.rSquared = rSquared;
    }

    public int getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(int windowDays) {
        this.windowDays = windowDays;
    }

    public int getDataPointCount() {
        return dataPointCount;
    }

    public void setDataPointCount(int dataPointCount) {
        this.dataPointCount = dataPointCount;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(Instant windowStart) {
        this.windowStart = windowStart;
    }

    public Instant getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(Instant windowEnd) {
        this.windowEnd = windowEnd;
    }

    public List<String> getCorrelatedSignals() {
        return correlatedSignals;
    }

    public void setCorrelatedSignals(List<String> correlatedSignals) {
        this.correlatedSignals = correlatedSignals;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }
}
