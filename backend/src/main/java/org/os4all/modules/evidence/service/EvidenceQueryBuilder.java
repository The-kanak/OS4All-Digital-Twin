package org.os4all.modules.evidence.service;

import org.os4all.modules.ai.model.StructuredHealthContext;
import org.os4all.modules.evidence.model.EvidenceQuery;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds targeted scientific literature search queries based on detected OS4All physiological patterns.
 * Never searches for or invents arbitrary disease diagnoses.
 * Restricts queries strictly to physiological mechanisms, autonomic dynamics, and baseline recovery.
 */
@Component
public class EvidenceQueryBuilder {

    /**
     * Constructs a constrained biomedical EvidenceQuery from the structured health context and observed patterns.
     */
    public EvidenceQuery buildQuery(StructuredHealthContext context, List<String> observations) {
        String combined = (observations != null ? String.join(" ", observations) : "").toLowerCase(Locale.ROOT);
        StringBuilder queryText = new StringBuilder();
        List<String> keywords = new ArrayList<>();

        boolean hasHeartRateDeviation = hasMetricDeviation(context, "heart") || combined.contains("heart rate");
        boolean hasHrvDeviation = hasMetricDeviation(context, "hrv") || combined.contains("hrv");
        boolean hasSleepDeviation = hasMetricDeviation(context, "sleep") || combined.contains("sleep");
        boolean hasTempDeviation = hasMetricDeviation(context, "temp") || combined.contains("temp");
        boolean hasSpo2Deviation = hasMetricDeviation(context, "spo2") || hasMetricDeviation(context, "oxygen") || combined.contains("spo2");

        if (hasSleepDeviation && (hasHeartRateDeviation || hasHrvDeviation)) {
            queryText.append("heart rate variability and resting heart rate elevation during acute sleep restriction recovery");
            keywords.addAll(List.of("HRV", "resting heart rate", "sleep restriction", "autonomic recovery"));
        } else if (hasHeartRateDeviation && hasHrvDeviation) {
            queryText.append("resting heart rate elevation and nocturnal heart rate variability autonomic strain");
            keywords.addAll(List.of("resting heart rate", "HRV", "autonomic nervous system"));
        } else if (hasTempDeviation && hasSpo2Deviation) {
            queryText.append("body temperature elevation and oxygen saturation variance wearable tracking");
            keywords.addAll(List.of("oxygen saturation", "body temperature", "respiratory rate"));
        } else if (hasLabDeviation(context, "glucose")) {
            queryText.append("fasting blood glucose variation clinical reference range adult");
            keywords.addAll(List.of("fasting glucose", "glycemic variability"));
        } else if (hasLabDeviation(context, "cholesterol") || hasLabDeviation(context, "lipid")) {
            queryText.append("circulating lipid fractions cholesterol triglycerides metabolic tracking");
            keywords.addAll(List.of("lipid panel", "cholesterol", "triglycerides"));
        } else {
            queryText.append("wearable physiological baseline deviation multi-signal analysis");
            keywords.addAll(List.of("digital biomarkers", "personal baseline"));
        }

        String physiologicalContext = "State: " + (context.anomalyState() != null ? context.anomalyState() : "UNKNOWN") +
                ", Duration: " + context.durationDays() + " days";

        return new EvidenceQuery(
                queryText.toString(),
                keywords,
                physiologicalContext,
                true,
                5
        );
    }

    private boolean hasMetricDeviation(StructuredHealthContext context, String metricKeyword) {
        if (context.deviations() != null) {
            for (StructuredHealthContext.SignalDeviationItem item : context.deviations()) {
                if (item.metric() != null && item.metric().toLowerCase(Locale.ROOT).contains(metricKeyword)) {
                    if (item.zScore() != null && Math.abs(item.zScore()) >= 1.0) {
                        return true;
                    }
                }
            }
        }
        if (context.activeAnomalies() != null) {
            for (StructuredHealthContext.DetectedAnomalyItem item : context.activeAnomalies()) {
                if (item.explanation() != null && item.explanation().toLowerCase(Locale.ROOT).contains(metricKeyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasLabDeviation(StructuredHealthContext context, String biomarkerKeyword) {
        if (context.recentLabResults() != null) {
            for (StructuredHealthContext.LabBiomarkerItem lab : context.recentLabResults()) {
                if (lab.biomarker() != null && lab.biomarker().toLowerCase(Locale.ROOT).contains(biomarkerKeyword)) {
                    if (lab.referenceHigh() != null && lab.value().compareTo(lab.referenceHigh()) > 0) return true;
                    if (lab.referenceLow() != null && lab.value().compareTo(lab.referenceLow()) < 0) return true;
                }
            }
        }
        return false;
    }
}
