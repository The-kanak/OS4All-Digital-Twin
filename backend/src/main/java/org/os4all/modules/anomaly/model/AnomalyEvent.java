package org.os4all.modules.anomaly.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * AnomalyEvent records an active or historical departure state (DRIFT, ANOMALY, FOLLOW_UP).
 * Strict non-diagnostic requirement: explains which signals changed, baseline comparison, duration,
 * confidence, and state reason without automated medical disease diagnoses.
 */
public class AnomalyEvent {

    private UUID id;
    private UUID userId;
    private HealthState state; // DRIFT, ANOMALY, FOLLOW_UP
    private String primaryMetric;
    private List<String> affectedSignals = new ArrayList<>();
    private String baselineComparison;
    private int durationDays;
    private BigDecimal confidence;
    private String reason;
    private String explanation;
    private boolean multiSignalCorrelationDetected;
    private String patternName; // e.g. "Autonomic Recovery Strain Pattern", "Sustained Elevation"
    private Instant detectedAt;

    public AnomalyEvent() {
        this.id = UUID.randomUUID();
        this.detectedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public HealthState getState() {
        return state;
    }

    public void setState(HealthState state) {
        this.state = state;
    }

    public String getPrimaryMetric() {
        return primaryMetric;
    }

    public void setPrimaryMetric(String primaryMetric) {
        this.primaryMetric = primaryMetric;
    }

    public List<String> getAffectedSignals() {
        return affectedSignals;
    }

    public void setAffectedSignals(List<String> affectedSignals) {
        this.affectedSignals = affectedSignals;
    }

    public String getBaselineComparison() {
        return baselineComparison;
    }

    public void setBaselineComparison(String baselineComparison) {
        this.baselineComparison = baselineComparison;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public boolean isMultiSignalCorrelationDetected() {
        return multiSignalCorrelationDetected;
    }

    public void setMultiSignalCorrelationDetected(boolean multiSignalCorrelationDetected) {
        this.multiSignalCorrelationDetected = multiSignalCorrelationDetected;
    }

    public String getPatternName() {
        return patternName;
    }

    public void setPatternName(String patternName) {
        this.patternName = patternName;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }
}
