package org.os4all.modules.anomaly.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents an individual monitored biological signal, evaluating its deviation from personal baseline,
 * persistence, magnitude, data confidence, and recency.
 */
public class HealthSignal {

    private String metric;
    private String unit;
    private HealthState state; // STABLE, DRIFT, ANOMALY, FOLLOW_UP
    private BigDecimal baselineMean;
    private BigDecimal currentValue;
    private BigDecimal deviation;
    private BigDecimal deviationPercentage;
    private Double zScore;
    private String direction; // "ELEVATED", "DEPRESSED", "STABLE"
    private int persistenceDays;
    private int consecutiveDeviatingReadings;
    private BigDecimal dataConfidence; // Average confidence of readings (0.0 to 1.0)
    private Instant lastObservedAt;
    private String explanation;

    public HealthSignal() {
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

    public HealthState getState() {
        return state;
    }

    public void setState(HealthState state) {
        this.state = state;
    }

    public BigDecimal getBaselineMean() {
        return baselineMean;
    }

    public void setBaselineMean(BigDecimal baselineMean) {
        this.baselineMean = baselineMean;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getDeviation() {
        return deviation;
    }

    public void setDeviation(BigDecimal deviation) {
        this.deviation = deviation;
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

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public int getPersistenceDays() {
        return persistenceDays;
    }

    public void setPersistenceDays(int persistenceDays) {
        this.persistenceDays = persistenceDays;
    }

    public int getConsecutiveDeviatingReadings() {
        return consecutiveDeviatingReadings;
    }

    public void setConsecutiveDeviatingReadings(int consecutiveDeviatingReadings) {
        this.consecutiveDeviatingReadings = consecutiveDeviatingReadings;
    }

    public BigDecimal getDataConfidence() {
        return dataConfidence;
    }

    public void setDataConfidence(BigDecimal dataConfidence) {
        this.dataConfidence = dataConfidence;
    }

    public Instant getLastObservedAt() {
        return lastObservedAt;
    }

    public void setLastObservedAt(Instant lastObservedAt) {
        this.lastObservedAt = lastObservedAt;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
