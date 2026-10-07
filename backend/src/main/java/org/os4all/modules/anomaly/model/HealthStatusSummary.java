package org.os4all.modules.anomaly.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * HealthStatusSummary reflects overall system status for the user,
 * active state (STABLE, DRIFT, ANOMALY, FOLLOW_UP), active anomaly count,
 * detected multi-signal patterns, and non-diagnostic disclaimers.
 */
public class HealthStatusSummary {

    public static final String CLINICAL_DISCLAIMER =
            "OS4All Physiological State Assessment: State determinations (STABLE, DRIFT, ANOMALY, FOLLOW_UP) " +
            "evaluate statistical departures from your individual historical pattern and DO NOT equal a clinical disease diagnosis. " +
            "Seek guidance from a qualified physician for medical concerns.";

    private UUID userId;
    private HealthState aggregateState; // STABLE, DRIFT, ANOMALY, FOLLOW_UP
    private String statusHeadline;
    private int activeSignalsCount;
    private int deviatingSignalsCount;
    private int activeAnomaliesCount;
    private boolean multiSignalPatternActive;
    private String patternSummary;
    private List<HealthSignal> signals = new ArrayList<>();
    private List<AnomalyEvent> activeAnomalies = new ArrayList<>();
    private String disclaimer = CLINICAL_DISCLAIMER;
    private Instant evaluatedAt;

    public HealthStatusSummary() {
        this.evaluatedAt = Instant.now();
    }

    public HealthStatusSummary(UUID userId) {
        this();
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public HealthState getAggregateState() {
        return aggregateState;
    }

    public void setAggregateState(HealthState aggregateState) {
        this.aggregateState = aggregateState;
    }

    public String getStatusHeadline() {
        return statusHeadline;
    }

    public void setStatusHeadline(String statusHeadline) {
        this.statusHeadline = statusHeadline;
    }

    public int getActiveSignalsCount() {
        return activeSignalsCount;
    }

    public void setActiveSignalsCount(int activeSignalsCount) {
        this.activeSignalsCount = activeSignalsCount;
    }

    public int getDeviatingSignalsCount() {
        return deviatingSignalsCount;
    }

    public void setDeviatingSignalsCount(int deviatingSignalsCount) {
        this.deviatingSignalsCount = deviatingSignalsCount;
    }

    public int getActiveAnomaliesCount() {
        return activeAnomaliesCount;
    }

    public void setActiveAnomaliesCount(int activeAnomaliesCount) {
        this.activeAnomaliesCount = activeAnomaliesCount;
    }

    public boolean isMultiSignalPatternActive() {
        return multiSignalPatternActive;
    }

    public void setMultiSignalPatternActive(boolean multiSignalPatternActive) {
        this.multiSignalPatternActive = multiSignalPatternActive;
    }

    public String getPatternSummary() {
        return patternSummary;
    }

    public void setPatternSummary(String patternSummary) {
        this.patternSummary = patternSummary;
    }

    public List<HealthSignal> getSignals() {
        return signals;
    }

    public void setSignals(List<HealthSignal> signals) {
        this.signals = signals;
    }

    public List<AnomalyEvent> getActiveAnomalies() {
        return activeAnomalies;
    }

    public void setActiveAnomalies(List<AnomalyEvent> activeAnomalies) {
        this.activeAnomalies = activeAnomalies;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
