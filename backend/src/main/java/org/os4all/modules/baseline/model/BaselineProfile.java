package org.os4all.modules.baseline.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregated baseline profile for a user across all continuous biometric metrics.
 * Explicitly conveys:
 * "Normal for the population is not necessarily normal for the individual."
 * "Statistical deviation does NOT equal disease diagnosis."
 */
public class BaselineProfile {

    public static final String CLINICAL_DISCLAIMER =
            "OS4All Baseline Assessment: Normal for the population is not necessarily normal for the individual. " +
            "Statistical deviations represent variations from your own historical biometric pattern and DO NOT equal a clinical diagnosis or disease state. " +
            "Consult a licensed medical provider for diagnostic evaluation.";

    private UUID userId;
    private Instant generatedAt;
    private int totalMetricsTracked;
    private int establishedBaselinesCount;
    private String corePhilosophy;
    private String disclaimer;
    private List<BaselineMetric> metrics = new ArrayList<>();

    public BaselineProfile() {
        this.generatedAt = Instant.now();
        this.corePhilosophy = "Normal for the population is not necessarily normal for the individual.";
        this.disclaimer = CLINICAL_DISCLAIMER;
    }

    public BaselineProfile(UUID userId) {
        this();
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getTotalMetricsTracked() {
        return totalMetricsTracked;
    }

    public void setTotalMetricsTracked(int totalMetricsTracked) {
        this.totalMetricsTracked = totalMetricsTracked;
    }

    public int getEstablishedBaselinesCount() {
        return establishedBaselinesCount;
    }

    public void setEstablishedBaselinesCount(int establishedBaselinesCount) {
        this.establishedBaselinesCount = establishedBaselinesCount;
    }

    public String getCorePhilosophy() {
        return corePhilosophy;
    }

    public void setCorePhilosophy(String corePhilosophy) {
        this.corePhilosophy = corePhilosophy;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public List<BaselineMetric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<BaselineMetric> metrics) {
        this.metrics = metrics;
    }

    public void addMetric(BaselineMetric metric) {
        this.metrics.add(metric);
        this.totalMetricsTracked = this.metrics.size();
        if (metric.isBaselineEstablished()) {
            this.establishedBaselinesCount++;
        }
    }
}
