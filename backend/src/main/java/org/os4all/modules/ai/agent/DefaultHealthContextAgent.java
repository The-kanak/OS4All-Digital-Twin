package org.os4all.modules.ai.agent;

import org.os4all.modules.ai.entity.AiInferenceLog;
import org.os4all.modules.ai.model.StructuredHealthContext;
import org.os4all.modules.ai.repository.AiInferenceLogRepository;
import org.os4all.modules.anomaly.model.AnomalyEvent;
import org.os4all.modules.anomaly.model.HealthSignal;
import org.os4all.modules.anomaly.model.HealthStatusSummary;
import org.os4all.modules.anomaly.service.RuleBasedAnomalyEngine;
import org.os4all.modules.baseline.model.BaselineProfile;
import org.os4all.modules.baseline.service.PersonalBaselineService;
import org.os4all.modules.ingestion.entity.HealthObservation;
import org.os4all.modules.ingestion.entity.ObservationType;
import org.os4all.modules.ingestion.repository.HealthObservationRepository;
import org.os4all.modules.lab.entity.LabReport;
import org.os4all.modules.lab.entity.LabResult;
import org.os4all.modules.lab.repository.LabReportRepository;
import org.os4all.modules.lab.repository.LabResultRepository;
import org.os4all.modules.user.entity.UserProfile;
import org.os4all.modules.user.repository.UserProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class DefaultHealthContextAgent implements HealthContextAgent {

    private final PersonalBaselineService baselineService;
    private final RuleBasedAnomalyEngine anomalyEngine;
    private final HealthObservationRepository observationRepository;
    private final LabResultRepository labResultRepository;
    private final UserProfileRepository userProfileRepository;
    private final LabReportRepository labReportRepository;
    private final AiInferenceLogRepository inferenceLogRepository;

    public DefaultHealthContextAgent(
            PersonalBaselineService baselineService,
            RuleBasedAnomalyEngine anomalyEngine,
            HealthObservationRepository observationRepository,
            LabResultRepository labResultRepository,
            UserProfileRepository userProfileRepository,
            LabReportRepository labReportRepository,
            AiInferenceLogRepository inferenceLogRepository
    ) {
        this.baselineService = baselineService;
        this.anomalyEngine = anomalyEngine;
        this.observationRepository = observationRepository;
        this.labResultRepository = labResultRepository;
        this.userProfileRepository = userProfileRepository;
        this.labReportRepository = labReportRepository;
        this.inferenceLogRepository = inferenceLogRepository;
    }

    @Override
    public StructuredHealthContext buildHealthContext(UUID userId) {
        // Anonymize user identifier (never send real user identity or DB PII to LLM)
        String userPseudonym = "USER_" + userId.toString().substring(0, 8);

        // Demographic context (anonymized age and biological sex only)
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        String sex = profileOpt.map(UserProfile::getBiologicalSex).orElse("UNSPECIFIED");
        Integer age = null;
        if (profileOpt.isPresent() && profileOpt.get().getDateOfBirth() != null) {
            age = Period.between(profileOpt.get().getDateOfBirth(), LocalDate.now(ZoneOffset.UTC)).getYears();
        }

        // 1. Fetch personal baselines (Personal Baseline Engine)
        BaselineProfile baselineProfile = baselineService.calculateBaselineProfile(userId);
        Map<String, StructuredHealthContext.BaselineContextItem> baselines = new HashMap<>();
        if (baselineProfile.getMetrics() != null) {
            for (org.os4all.modules.baseline.model.BaselineMetric bm : baselineProfile.getMetrics()) {
                baselines.put(bm.getMetric().toLowerCase(Locale.ROOT), new StructuredHealthContext.BaselineContextItem(
                        bm.getMetric(),
                        bm.getMean(),
                        bm.getStandardDeviation(),
                        bm.getBaselineRangeLow(),
                        bm.getBaselineRangeHigh(),
                        bm.getObservationCount(),
                        bm.isBaselineEstablished()
                ));
            }
        }

        // 2. Fetch recent continuous observations (last 7 days) and extract current measurements & symptoms
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        List<HealthObservation> obsList = observationRepository.findTimelineObservations(userId, sevenDaysAgo, Instant.now());
        List<StructuredHealthContext.RecentObservationItem> recentObservations = new ArrayList<>();
        List<String> userProvidedSymptoms = new ArrayList<>();
        Map<String, StructuredHealthContext.CurrentMeasurementItem> latestMeasurements = new LinkedHashMap<>();

        for (HealthObservation obs : obsList) {
            String typeName = obs.getObservationType().name();
            StructuredHealthContext.BaselineContextItem baseline = baselines.get(typeName.toLowerCase(Locale.ROOT));

            BigDecimal dev = null;
            if (baseline != null && baseline.mean() != null && obs.getValueNumeric() != null) {
                dev = obs.getValueNumeric().subtract(baseline.mean());
            }

            recentObservations.add(new StructuredHealthContext.RecentObservationItem(
                    typeName,
                    obs.getValueNumeric(),
                    obs.getUnit(),
                    obs.getTimestamp() != null ? obs.getTimestamp().toString() : Instant.now().toString(),
                    dev
            ));

            if (obs.getObservationType() == ObservationType.SYMPTOM) {
                String symptomText = obs.getValueText() != null ? obs.getValueText() : "Reported symptom";
                userProvidedSymptoms.add(symptomText);
            } else if (obs.getValueNumeric() != null) {
                latestMeasurements.putIfAbsent(typeName, new StructuredHealthContext.CurrentMeasurementItem(
                        typeName,
                        obs.getValueNumeric(),
                        obs.getUnit(),
                        obs.getTimestamp() != null ? obs.getTimestamp().toString() : Instant.now().toString()
                ));
            }
        }

        // 3. Trend Analysis & Signal Deviations (evaluates persistence, z-score, directions)
        List<HealthSignal> evaluatedSignals = anomalyEngine.evaluateHealthSignals(userId);
        List<StructuredHealthContext.SignalDeviationItem> deviations = new ArrayList<>();
        String dominantTrend = "STABLE";
        int maxDurationDays = 0;

        for (HealthSignal sig : evaluatedSignals) {
            if (sig.getDeviation() != null && sig.getDeviation().compareTo(BigDecimal.ZERO) != 0) {
                deviations.add(new StructuredHealthContext.SignalDeviationItem(
                        sig.getMetric(),
                        sig.getCurrentValue(),
                        sig.getBaselineMean(),
                        sig.getDeviation(),
                        sig.getZScore(),
                        sig.getDirection(),
                        sig.getPersistenceDays()
                ));
                if (sig.getPersistenceDays() > maxDurationDays) {
                    maxDurationDays = sig.getPersistenceDays();
                }
                if (!"STABLE".equalsIgnoreCase(sig.getDirection())) {
                    dominantTrend = sig.getDirection();
                }
            }
        }

        // 4. Anomaly Events from Rule-Based Anomaly Engine
        List<AnomalyEvent> anomalies = anomalyEngine.detectAnomalies(userId);
        HealthStatusSummary statusSummary = anomalyEngine.evaluateStatus(userId);

        List<StructuredHealthContext.DetectedAnomalyItem> activeAnomalies = new ArrayList<>();
        for (AnomalyEvent ae : anomalies) {
            activeAnomalies.add(new StructuredHealthContext.DetectedAnomalyItem(
                    ae.getPrimaryMetric(),
                    ae.getState().name(),
                    ae.getExplanation(),
                    BigDecimal.valueOf(ae.getDurationDays()),
                    ae.getConfidence(),
                    ae.isMultiSignalCorrelationDetected()
            ));
            if (ae.getDurationDays() > maxDurationDays) {
                maxDurationDays = ae.getDurationDays();
            }
        }

        // 5. Recent confirmed lab results
        Page<LabResult> labs = labResultRepository.findFiltered(userId, null, null, null, PageRequest.of(0, 15));
        List<StructuredHealthContext.LabBiomarkerItem> recentLabs = new ArrayList<>();
        for (LabResult lr : labs.getContent()) {
            if (Boolean.TRUE.equals(lr.getIsConfirmed())) {
                recentLabs.add(new StructuredHealthContext.LabBiomarkerItem(
                        lr.getBiomarker(),
                        lr.getValue(),
                        lr.getUnit(),
                        lr.getReferenceLow(),
                        lr.getReferenceHigh(),
                        lr.getCollectionDate() != null ? lr.getCollectionDate().toString() : null
                ));
            }
        }

        // 6. Previous AI insights (most recent 5)
        Page<AiInferenceLog> pastLogs = inferenceLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5));
        List<StructuredHealthContext.PreviousInsightItem> previousInsights = new ArrayList<>();
        for (AiInferenceLog logItem : pastLogs.getContent()) {
            previousInsights.add(new StructuredHealthContext.PreviousInsightItem(
                    logItem.getId() != null ? logItem.getId().toString() : "",
                    logItem.getStructuredSummary(),
                    logItem.getUrgency(),
                    logItem.getCreatedAt() != null ? logItem.getCreatedAt().toString() : ""
            ));
        }

        // 7. Uploaded reports metadata (most recent 10)
        Page<LabReport> pastReports = labReportRepository.findFiltered(userId, null, null, PageRequest.of(0, 10));
        List<StructuredHealthContext.UploadedReportItem> uploadedReports = new ArrayList<>();
        for (LabReport report : pastReports.getContent()) {
            uploadedReports.add(new StructuredHealthContext.UploadedReportItem(
                    report.getId() != null ? report.getId().toString() : "",
                    report.getFileName() != null ? report.getFileName() : report.getReportTitle(),
                    report.getReportTitle(),
                    report.getOcrStatus(),
                    report.getResults() != null ? report.getResults().size() : 0,
                    Boolean.TRUE.equals(report.getReviewRequired()),
                    report.getCreatedAt() != null ? report.getCreatedAt().toString() : ""
            ));
        }

        // Generate traceable workflow ID for this health evaluation pipeline run
        String workflowId = "WF-" + UUID.randomUUID().toString();

        return new StructuredHealthContext(
                workflowId,
                StructuredHealthContext.CURRENT_VERSION,
                userPseudonym,
                sex,
                age,
                statusSummary.getAggregateState().name(),
                new ArrayList<>(latestMeasurements.values()),
                baselines,
                deviations,
                dominantTrend,
                maxDurationDays,
                recentObservations,
                activeAnomalies,
                userProvidedSymptoms,
                recentLabs,
                List.of(), // Populated if external evidence is retrieved
                previousInsights,
                uploadedReports
        );
    }
}
