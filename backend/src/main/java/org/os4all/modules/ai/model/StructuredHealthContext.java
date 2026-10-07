package org.os4all.modules.ai.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Clean, structured health context object provided strictly to the AI layer.
 * AI has NO raw database access; it receives exclusively this curated context.
 * Contains only physiological and behavioral measurements necessary for deterministic trend interpretation.
 */
public record StructuredHealthContext(
        String workflowId,
        String contextVersion,
        String userIdentifier, // Anonymized pseudonym, e.g. "USER_PSEUDONYM_123"
        String biologicalSex,
        Integer ageYears,
        String anomalyState,
        List<CurrentMeasurementItem> currentMeasurements,
        Map<String, BaselineContextItem> personalBaselines,
        List<SignalDeviationItem> deviations,
        String trendDirection,
        Integer durationDays,
        List<RecentObservationItem> recentObservations,
        List<DetectedAnomalyItem> activeAnomalies,
        List<String> userProvidedSymptoms,
        List<LabBiomarkerItem> recentLabResults,
        List<EvidenceResultItem> evidenceResults,
        List<PreviousInsightItem> previousInsights,
        List<UploadedReportItem> uploadedReports
) {
    public static final String CURRENT_VERSION = "v1.0-deterministic";

    /**
     * Backward-compatible 10-parameter constructor for legacy calls.
     */
    public StructuredHealthContext(
            String userIdentifier,
            String biologicalSex,
            Integer ageYears,
            Map<String, BaselineContextItem> personalBaselines,
            List<RecentObservationItem> recentObservations,
            List<DetectedAnomalyItem> activeAnomalies,
            List<LabBiomarkerItem> recentLabResults,
            String currentAggregateState,
            List<PreviousInsightItem> previousInsights,
            List<UploadedReportItem> uploadedReports
    ) {
        this(
                "WF-" + UUID.randomUUID().toString().substring(0, 8),
                CURRENT_VERSION,
                userIdentifier,
                biologicalSex,
                ageYears,
                currentAggregateState != null ? currentAggregateState : "BASELINE",
                extractCurrentMeasurements(recentObservations),
                personalBaselines != null ? personalBaselines : Map.of(),
                extractDeviations(personalBaselines, recentObservations, activeAnomalies),
                determineDominantTrend(activeAnomalies),
                determineMaxDuration(activeAnomalies),
                recentObservations != null ? recentObservations : List.of(),
                activeAnomalies != null ? activeAnomalies : List.of(),
                extractSymptoms(recentObservations),
                recentLabResults != null ? recentLabResults : List.of(),
                List.of(),
                previousInsights != null ? previousInsights : List.of(),
                uploadedReports != null ? uploadedReports : List.of()
        );
    }

    /**
     * Backward-compatible alias for currentAggregateState.
     */
    public String currentAggregateState() {
        return anomalyState != null ? anomalyState : "BASELINE";
    }

    /**
     * Creates a copy of this context with updated evidence results.
     */
    public StructuredHealthContext withEvidenceResults(List<EvidenceResultItem> evidence) {
        return new StructuredHealthContext(
                this.workflowId,
                this.contextVersion,
                this.userIdentifier,
                this.biologicalSex,
                this.ageYears,
                this.anomalyState,
                this.currentMeasurements,
                this.personalBaselines,
                this.deviations,
                this.trendDirection,
                this.durationDays,
                this.recentObservations,
                this.activeAnomalies,
                this.userProvidedSymptoms,
                this.recentLabResults,
                evidence != null ? evidence : List.of(),
                this.previousInsights,
                this.uploadedReports
        );
    }

    public record CurrentMeasurementItem(
            String metric,
            BigDecimal value,
            String unit,
            String timestampIso
    ) {}

    public record SignalDeviationItem(
            String metric,
            BigDecimal currentValue,
            BigDecimal baselineMean,
            BigDecimal deviation,
            Double zScore,
            String trendDirection,
            int durationDays
    ) {}

    public record EvidenceResultItem(
            String title,
            String source,
            String urlOrDoi,
            String relevance,
            String domain,
            String retrievedTime,
            String relevantExcerpt,
            Double relevanceScore
    ) {
        public EvidenceResultItem(String title, String source, String urlOrDoi, String relevance) {
            this(
                    title,
                    source,
                    urlOrDoi,
                    relevance,
                    extractDomainFromUrl(urlOrDoi, source),
                    Instant.now().toString(),
                    relevance,
                    0.90
            );
        }

        private static String extractDomainFromUrl(String url, String source) {
            if (url != null && url.startsWith("http")) {
                try {
                    String host = java.net.URI.create(url).getHost();
                    if (host != null && !host.isBlank()) return host;
                } catch (Exception ignored) {}
            }
            return source != null ? source : "ncbi.nlm.nih.gov";
        }
    }

    public record PreviousInsightItem(
            String id,
            String summary,
            String urgency,
            String createdAtIso
    ) {}

    public record UploadedReportItem(
            String id,
            String fileName,
            String reportType,
            String status,
            int extractedBiomarkersCount,
            boolean reviewRequired,
            String uploadedAtIso
    ) {}

    public record BaselineContextItem(
            String metric,
            BigDecimal mean,
            BigDecimal standardDeviation,
            BigDecimal baselineRangeLow,
            BigDecimal baselineRangeHigh,
            int observationCount,
            boolean isReliable
    ) {}

    public record RecentObservationItem(
            String observationType,
            BigDecimal value,
            String unit,
            String timestampIso,
            BigDecimal deviationFromBaseline
    ) {}

    public record DetectedAnomalyItem(
            String metric,
            String state,
            String explanation,
            BigDecimal persistenceDays,
            BigDecimal confidence,
            boolean isMultiSignalCompound
    ) {}

    public record LabBiomarkerItem(
            String biomarker,
            BigDecimal value,
            String unit,
            BigDecimal referenceLow,
            BigDecimal referenceHigh,
            String collectionDateIso
    ) {}

    private static List<CurrentMeasurementItem> extractCurrentMeasurements(List<RecentObservationItem> recentObservations) {
        if (recentObservations == null || recentObservations.isEmpty()) {
            return List.of();
        }
        Map<String, CurrentMeasurementItem> latestByMetric = new LinkedHashMap<>();
        for (RecentObservationItem item : recentObservations) {
            latestByMetric.put(item.observationType(), new CurrentMeasurementItem(
                    item.observationType(),
                    item.value(),
                    item.unit(),
                    item.timestampIso()
            ));
        }
        return new ArrayList<>(latestByMetric.values());
    }

    private static List<SignalDeviationItem> extractDeviations(
            Map<String, BaselineContextItem> baselines,
            List<RecentObservationItem> observations,
            List<DetectedAnomalyItem> anomalies
    ) {
        List<SignalDeviationItem> list = new ArrayList<>();
        if (observations == null || baselines == null) {
            return list;
        }

        Map<String, RecentObservationItem> latestObs = new HashMap<>();
        for (RecentObservationItem o : observations) {
            String norm = normalizeMetricKey(o.observationType());
            latestObs.put(norm, o);
            latestObs.put(o.observationType().toLowerCase(Locale.ROOT), o);
        }

        for (Map.Entry<String, BaselineContextItem> entry : baselines.entrySet()) {
            String rawKey = entry.getKey().toLowerCase(Locale.ROOT);
            String normKey = normalizeMetricKey(entry.getKey());
            RecentObservationItem obs = latestObs.get(normKey);
            if (obs == null) {
                obs = latestObs.get(rawKey);
            }
            BaselineContextItem base = entry.getValue();

            if (obs != null && obs.value() != null && base.mean() != null) {
                BigDecimal dev = obs.value().subtract(base.mean());
                Double z = null;
                if (base.standardDeviation() != null && base.standardDeviation().compareTo(BigDecimal.ZERO) > 0) {
                    z = dev.doubleValue() / base.standardDeviation().doubleValue();
                }

                String direction = dev.compareTo(BigDecimal.ZERO) > 0 ? "INCREASE" :
                        (dev.compareTo(BigDecimal.ZERO) < 0 ? "DECREASE" : "STABLE");

                int duration = 1;
                if (anomalies != null) {
                    for (DetectedAnomalyItem a : anomalies) {
                        if (a.metric() != null && a.metric().equalsIgnoreCase(base.metric())) {
                            duration = a.persistenceDays() != null ? a.persistenceDays().intValue() : 1;
                            break;
                        }
                    }
                }

                list.add(new SignalDeviationItem(
                        base.metric(),
                        obs.value(),
                        base.mean(),
                        dev,
                        z,
                        direction,
                        duration
                ));
            }
        }
        return list;
    }

    private static String determineDominantTrend(List<DetectedAnomalyItem> anomalies) {
        if (anomalies == null || anomalies.isEmpty()) {
            return "STABLE";
        }
        for (DetectedAnomalyItem item : anomalies) {
            if ("ANOMALY".equalsIgnoreCase(item.state())) return "ANOMALY";
            if ("DRIFT".equalsIgnoreCase(item.state())) return "DRIFT";
        }
        return "STABLE";
    }

    private static Integer determineMaxDuration(List<DetectedAnomalyItem> anomalies) {
        if (anomalies == null || anomalies.isEmpty()) {
            return 0;
        }
        int max = 0;
        for (DetectedAnomalyItem a : anomalies) {
            if (a.persistenceDays() != null && a.persistenceDays().intValue() > max) {
                max = a.persistenceDays().intValue();
            }
        }
        return max;
    }

    private static List<String> extractSymptoms(List<RecentObservationItem> observations) {
        if (observations == null) return List.of();
        List<String> symptoms = new ArrayList<>();
        for (RecentObservationItem o : observations) {
            if ("SYMPTOM".equalsIgnoreCase(o.observationType())) {
                symptoms.add("Symptom reported at " + o.timestampIso());
            }
        }
        return symptoms;
    }

    private static String normalizeMetricKey(String key) {
        if (key == null) return "";
        String lower = key.toLowerCase(Locale.ROOT).replace("_", " ").trim();
        if (lower.contains("heart")) return "heart_rate";
        if (lower.contains("hrv")) return "hrv";
        if (lower.contains("sleep")) return "sleep";
        if (lower.contains("temp")) return "temperature";
        return lower;
    }
}
