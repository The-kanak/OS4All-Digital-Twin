package org.os4all.modules.digitaltwin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.os4all.core.exception.ResourceNotFoundException;
import org.os4all.modules.baseline.model.BaselineMetric;
import org.os4all.modules.baseline.model.BaselineProfile;
import org.os4all.modules.baseline.service.PersonalBaselineService;
import org.os4all.modules.digitaltwin.dto.*;
import org.os4all.modules.digitaltwin.engine.MetabolicPredictionEngine;
import org.os4all.modules.digitaltwin.entity.*;
import org.os4all.modules.digitaltwin.repository.DigitalTwinPredictionRepository;
import org.os4all.modules.digitaltwin.repository.DigitalTwinStateRepository;
import org.os4all.modules.digitaltwin.repository.HistoricalMedicalRecordRepository;
import org.os4all.modules.ingestion.entity.*;
import org.os4all.modules.ingestion.repository.HealthObservationRepository;
import org.os4all.modules.lab.entity.LabResult;
import org.os4all.modules.lab.repository.LabResultRepository;
import org.os4all.modules.user.entity.User;
import org.os4all.modules.user.entity.UserProfile;
import org.os4all.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.os4all.modules.digitaltwin.telemetry.entity.PatientTelemetry;
import org.os4all.modules.digitaltwin.telemetry.repository.PatientTelemetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DigitalTwinService {

    private static final Logger log = LoggerFactory.getLogger(DigitalTwinService.class);

    private final UserRepository userRepository;
    private final HistoricalMedicalRecordRepository medicalRecordRepository;
    private final DigitalTwinStateRepository twinStateRepository;
    private final DigitalTwinPredictionRepository predictionRepository;
    private final HealthObservationRepository observationRepository;
    private final LabResultRepository labResultRepository;
    private final PersonalBaselineService baselineService;
    private final ObjectMapper objectMapper;
    private final PatientTelemetryRepository telemetryRepository;

    @Autowired
    public DigitalTwinService(
            UserRepository userRepository,
            HistoricalMedicalRecordRepository medicalRecordRepository,
            DigitalTwinStateRepository twinStateRepository,
            DigitalTwinPredictionRepository predictionRepository,
            HealthObservationRepository observationRepository,
            LabResultRepository labResultRepository,
            PersonalBaselineService baselineService,
            ObjectMapper objectMapper,
            PatientTelemetryRepository telemetryRepository
    ) {
        this.userRepository = userRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.twinStateRepository = twinStateRepository;
        this.predictionRepository = predictionRepository;
        this.observationRepository = observationRepository;
        this.labResultRepository = labResultRepository;
        this.baselineService = baselineService;
        this.objectMapper = objectMapper;
        this.telemetryRepository = telemetryRepository;
    }

    public DigitalTwinService(
            UserRepository userRepository,
            HistoricalMedicalRecordRepository medicalRecordRepository,
            DigitalTwinStateRepository twinStateRepository,
            DigitalTwinPredictionRepository predictionRepository,
            HealthObservationRepository observationRepository,
            LabResultRepository labResultRepository,
            PersonalBaselineService baselineService,
            ObjectMapper objectMapper
    ) {
        this(userRepository, medicalRecordRepository, twinStateRepository, predictionRepository,
                observationRepository, labResultRepository, baselineService, objectMapper, null);
    }

    /**
     * Lists all synthetic patients with current digital twin summary state.
     */
    @Transactional(readOnly = true)
    public List<PatientSummaryDto> getAllPatients() {
        List<User> users = userRepository.findAll();
        List<PatientSummaryDto> summaries = new ArrayList<>();

        for (User u : users) {
            UserProfile p = u.getProfile();
            Integer age = calculateAge(p != null ? p.getDateOfBirth() : null);
            BigDecimal bmi = calculateBmi(p != null ? p.getHeightCm() : null, p != null ? p.getWeightKg() : null);

            DigitalTwinState state = twinStateRepository.findTopByUserIdOrderByLastUpdatedAtDesc(u.getId())
                    .orElse(null);

            List<HistoricalMedicalRecord> records = medicalRecordRepository.findByUserIdOrderByDiagnosedDateDesc(u.getId());
            String primaryCondition = records.stream()
                    .filter(r -> "CONDITION".equalsIgnoreCase(r.getRecordType()) || "PREVIOUS_DIAGNOSIS".equalsIgnoreCase(r.getRecordType()))
                    .map(HistoricalMedicalRecord::getConditionOrDiagnosis)
                    .findFirst()
                    .orElse("No chronic diagnosis on file");

            List<String> medications = records.stream()
                    .filter(r -> r.getMedications() != null && !r.getMedications().isBlank())
                    .map(HistoricalMedicalRecord::getMedications)
                    .flatMap(m -> Arrays.stream(m.split(",")))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .limit(3)
                    .toList();

            summaries.add(new PatientSummaryDto(
                    u.getId(),
                    u.getFullName(),
                    u.getEmail(),
                    age,
                    p != null ? p.getBiologicalSex() : "UNSPECIFIED",
                    p != null ? p.getHeightCm() : null,
                    p != null ? p.getWeightKg() : null,
                    bmi,
                    p != null ? p.getBloodType() : "O+",
                    state != null ? state.getTwinState().name() : "STABLE",
                    state != null ? state.getOverallRiskScore() : BigDecimal.ZERO,
                    state != null ? state.getGlucoseSpikeProbability() : BigDecimal.valueOf(12.0),
                    primaryCondition,
                    medications,
                    state != null ? state.getLastUpdatedAt().toString() : Instant.now().toString()
            ));
        }

        return summaries;
    }

    /**
     * Gets detailed patient information including demographics, records, and biomarkers.
     */
    @Transactional(readOnly = true)
    public PatientDetailDto getPatientDetail(UUID patientId) {
        User u = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));

        UserProfile p = u.getProfile();
        Integer age = calculateAge(p != null ? p.getDateOfBirth() : null);
        BigDecimal bmi = calculateBmi(p != null ? p.getHeightCm() : null, p != null ? p.getWeightKg() : null);

        DigitalTwinState state = twinStateRepository.findTopByUserIdOrderByLastUpdatedAtDesc(patientId)
                .orElse(null);

        List<HistoricalMedicalRecord> records = medicalRecordRepository.findByUserIdOrderByDiagnosedDateDesc(patientId);
        List<PatientDetailDto.HistoricalRecordDto> recordDtos = records.stream().map(r ->
                new PatientDetailDto.HistoricalRecordDto(
                        r.getId(),
                        r.getRecordType(),
                        r.getConditionOrDiagnosis(),
                        r.getIcd10Code(),
                        r.getSeverity(),
                        r.getStatus(),
                        r.getDiagnosedDate(),
                        r.getMedications(),
                        r.getFamilyHistoryNotes(),
                        r.getClinicalNotes()
                )
        ).toList();

        List<LabResult> labs = labResultRepository.findByUserIdOrderByCollectionDateDesc(patientId);
        List<PatientDetailDto.LatestLabBiomarkerDto> labDtos = labs.stream().limit(15).map(l ->
                new PatientDetailDto.LatestLabBiomarkerDto(
                        l.getStandardizedBiomarker() != null ? l.getStandardizedBiomarker() : l.getBiomarker(),
                        l.getValue(),
                        l.getUnit(),
                        l.getReferenceLow(),
                        l.getReferenceHigh(),
                        l.getCollectionDate().toString()
                )
        ).toList();

        return new PatientDetailDto(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                p != null ? p.getDateOfBirth() : null,
                age,
                p != null ? p.getBiologicalSex() : "MALE",
                p != null ? p.getHeightCm() : new BigDecimal("178.0"),
                p != null ? p.getWeightKg() : new BigDecimal("75.0"),
                bmi,
                p != null ? p.getBloodType() : "O+",
                p != null ? p.getLifestyleNotes() : "Non-smoker, sedentary desk schedule, occasional exercise.",
                state != null ? state.getTwinState().name() : "STABLE",
                state != null ? state.getOverallRiskScore() : BigDecimal.ZERO,
                state != null ? state.getMetabolicRiskScore() : BigDecimal.ZERO,
                state != null ? state.getGlucoseSpikeProbability() : BigDecimal.valueOf(12.0),
                state != null ? state.getPredictionHorizon() : "Next 2 Hours",
                recordDtos,
                labDtos,
                "Research / Hackathon Prototype — Not a Medical Diagnosis."
        );
    }

    /**
     * Gets the full living Digital Twin state with baseline deviations and prediction snapshot.
     */
    @Transactional
    public DigitalTwinStateDto getDigitalTwinState(UUID patientId) {
        User u = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));

        DigitalTwinState twinState = twinStateRepository.findTopByUserIdOrderByLastUpdatedAtDesc(patientId)
                .orElseGet(() -> updateDigitalTwinState(patientId, "STABLE PATIENT"));

        BaselineProfile baselineProfile = baselineService.calculateBaselineProfile(patientId);
        List<DigitalTwinStateDto.BaselineDeviationDto> deviations = buildBaselineDeviations(baselineProfile);

        // Current snapshot vitals
        DigitalTwinStateDto.CurrentVitalsSnapshot vitalsSnapshot = extractCurrentVitalsSnapshot(patientId);

        // Latest prediction
        DigitalTwinPrediction latestPred = predictionRepository.findTopByUserIdOrderByCreatedAtDesc(patientId)
                .orElse(null);

        DigitalTwinStateDto.PredictionSnapshotDto predDto = null;
        if (latestPred != null) {
            List<String> topFactors = parseJsonList(latestPred.getTopContributingFactors());
            List<String> actions = parseJsonList(latestPred.getRecommendedClinicalActions());
            predDto = new DigitalTwinStateDto.PredictionSnapshotDto(
                    latestPred.getId(),
                    latestPred.getPredictionType(),
                    latestPred.getHorizonWindow(),
                    latestPred.getRiskLevel(),
                    latestPred.getProbability(),
                    latestPred.getHeadline(),
                    latestPred.getClinicalExplanation(),
                    topFactors,
                    latestPred.getHistoricalEvidence(),
                    actions,
                    latestPred.getConfidence(),
                    latestPred.getDisclaimer(),
                    latestPred.getProjectedGlucose120Min(),
                    latestPred.getGlucoseVelocity(),
                    latestPred.getProjectedDelta(),
                    latestPred.getTrajectoryDirection() != null ? latestPred.getTrajectoryDirection() : "STABLE"
            );
        }

        List<String> drivers = parseJsonList(twinState.getStateDrivers());
        BigDecimal delta120 = (twinState.getProjectedGlucose120Min() != null && vitalsSnapshot.glucose() != null) ?
                twinState.getProjectedGlucose120Min().subtract(vitalsSnapshot.glucose()).setScale(1, RoundingMode.HALF_UP) : null;

        return new DigitalTwinStateDto(
                patientId,
                u.getFullName(),
                twinState.getTwinState().name(),
                twinState.getOverallRiskScore(),
                twinState.getMetabolicRiskScore(),
                twinState.getGlucoseSpikeProbability(),
                twinState.getPredictionHorizon(),
                twinState.getConfidence(),
                drivers,
                twinState.getPhysiologicalStateSummary(),
                twinState.getLastSimulationScenario(),
                vitalsSnapshot,
                deviations,
                predDto,
                twinState.getLastUpdatedAt().toString(),
                "Research / Hackathon Prototype — Not a Medical Diagnosis.",
                twinState.getProjectedGlucose120Min(),
                twinState.getGlucoseVelocity(),
                delta120,
                twinState.getTrajectoryDirection() != null ? twinState.getTrajectoryDirection() : "STABLE"
        );
    }

    /**
     * Recomputes Digital Twin state and executes algorithmic MetabolicPredictionEngine.
     */
    @Transactional
    public DigitalTwinState updateDigitalTwinState(UUID patientId, String scenarioName) {
        User u = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));

        UserProfile profile = u.getProfile();
        BigDecimal bmi = calculateBmi(profile != null ? profile.getHeightCm() : null, profile != null ? profile.getWeightKg() : null);

        // 1. Calculate Baselines
        BaselineProfile baseline = baselineService.calculateBaselineProfile(patientId);

        // 2. Fetch Recent Wearable Observations (last 3-7 days)
        Instant now = Instant.now();
        List<HealthObservation> recentObs = observationRepository.findObservationsForBaseline(patientId, now.minus(7, ChronoUnit.DAYS));

        // 3. Extract Features for Prediction Engine
        MetabolicPredictionEngine.EngineeredFeatures features = engineerFeatures(baseline, recentObs, bmi, patientId);

        // 4. Run Algorithmic Prediction Engine
        MetabolicPredictionEngine.PredictionOutput prediction = MetabolicPredictionEngine.predict(features);

        // 5. Save or Update Digital Twin State
        DigitalTwinState state = twinStateRepository.findTopByUserIdOrderByLastUpdatedAtDesc(patientId)
                .orElseGet(() -> {
                    DigitalTwinState s = new DigitalTwinState();
                    s.setUser(u);
                    return s;
                });

        state.setTwinState(TwinStateType.valueOf(prediction.twinState()));
        state.setOverallRiskScore(prediction.overallRiskScore());
        state.setMetabolicRiskScore(prediction.metabolicRiskScore());
        state.setGlucoseSpikeProbability(prediction.spikeProbability());
        state.setPredictionHorizon(prediction.horizonWindow());
        state.setConfidence(prediction.confidence());
        state.setLastSimulationScenario(scenarioName);
        state.setLastUpdatedAt(now);

        try {
            state.setStateDrivers(objectMapper.writeValueAsString(prediction.stateDrivers()));
        } catch (Exception e) {
            state.setStateDrivers(String.join(", ", prediction.stateDrivers()));
        }

        state.setPhysiologicalStateSummary(String.format(
                "Digital Twin Status: %s. Glucose Spike Probability: %s%% within %s. Primary drivers: %s.",
                prediction.twinState(), prediction.spikeProbability(), prediction.horizonWindow(),
                String.join("; ", prediction.stateDrivers())
        ));

        if (prediction.trajectoryProjection() != null) {
            state.setProjectedGlucose120Min(prediction.trajectoryProjection().projectedGlucose120Min());
            state.setGlucoseVelocity(prediction.trajectoryProjection().glucoseVelocityMgDlPerMin());
            state.setTrajectoryDirection(prediction.trajectoryProjection().trajectoryDirection());
        }

        DigitalTwinState savedState = twinStateRepository.save(state);

        // 6. Save Prediction Record
        DigitalTwinPrediction predRecord = new DigitalTwinPrediction();
        predRecord.setUser(u);
        predRecord.setPredictionType(prediction.predictionType());
        predRecord.setHorizonWindow(prediction.horizonWindow());
        predRecord.setRiskLevel(prediction.riskLevel());
        predRecord.setProbability(prediction.spikeProbability());
        predRecord.setModelName("MetabolicPredictionEngine-v2.1");
        predRecord.setHeadline(prediction.headline());
        predRecord.setClinicalExplanation(prediction.clinicalExplanation());
        predRecord.setConfidence(prediction.confidence());
        predRecord.setSimulation(scenarioName != null && !scenarioName.equals("STABLE PATIENT"));
        predRecord.setHistoricalEvidence("Continuous CGM and wearable sensor telemetry cross-referenced with individual 45-day baseline normal ranges.");

        if (prediction.trajectoryProjection() != null) {
            predRecord.setCurrentGlucose(prediction.trajectoryProjection().currentGlucose());
            predRecord.setGlucoseVelocity(prediction.trajectoryProjection().glucoseVelocityMgDlPerMin());
            predRecord.setProjectedGlucose120Min(prediction.trajectoryProjection().projectedGlucose120Min());
            predRecord.setProjectedDelta(prediction.trajectoryProjection().projectedDeltaMgDl());
            predRecord.setTrajectoryDirection(prediction.trajectoryProjection().trajectoryDirection());
            try {
                predRecord.setTrajectoryPointsJson(objectMapper.writeValueAsString(prediction.trajectoryProjection().trajectoryPoints()));
            } catch (Exception e) {
                predRecord.setTrajectoryPointsJson("[]");
            }
        }

        List<String> actions = new ArrayList<>();
        if (prediction.spikeProbability().doubleValue() >= 55.0) {
            actions.add("Recommend 15–20 min light post-meal walk to activate GLUT4 muscular glucose clearance.");
            actions.add("Avoid rapid-acting glycemic carbohydrates in current dietary window.");
            actions.add("Prioritize 8+ hours nocturnal sleep to reverse sympathetic-driven insulin resistance.");
        } else {
            actions.add("Maintain balanced nutritional regimen and normal physical activity.");
            actions.add("Continue continuous biometric tracking to maintain personal baseline fidelity.");
        }

        try {
            predRecord.setTopContributingFactors(objectMapper.writeValueAsString(prediction.contributions()));
            predRecord.setRecommendedClinicalActions(objectMapper.writeValueAsString(actions));
        } catch (Exception e) {
            predRecord.setTopContributingFactors("[]");
            predRecord.setRecommendedClinicalActions("[]");
        }

        predictionRepository.save(predRecord);
        log.info("Digital Twin state successfully updated for user {}: {}", u.getEmail(), savedState.getTwinState());

        return savedState;
    }

    /**
     * Gets the latest metabolic prediction for doctor dashboard.
     */
    @Transactional(readOnly = true)
    public PredictionResultDto getLatestPrediction(UUID patientId) {
        User u = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));

        DigitalTwinPrediction pred = predictionRepository.findTopByUserIdOrderByCreatedAtDesc(patientId)
                .orElse(null);

        if (pred == null) {
            // Compute first
            updateDigitalTwinState(patientId, "INITIAL CALIBRATION");
            pred = predictionRepository.findTopByUserIdOrderByCreatedAtDesc(patientId).orElseThrow();
        }

        List<PredictionResultDto.ContributingFactorDto> factorDtos = new ArrayList<>();
        if (pred.getTopContributingFactors() != null) {
            try {
                List<MetabolicPredictionEngine.FeatureContribution> contributions = objectMapper.readValue(
                        pred.getTopContributingFactors(),
                        new TypeReference<>() {}
                );
                for (MetabolicPredictionEngine.FeatureContribution c : contributions) {
                    factorDtos.add(new PredictionResultDto.ContributingFactorDto(
                            c.featureName(), c.weight(), c.description(), c.impactDirection()
                    ));
                }
            } catch (Exception ignored) {}
        }

        List<String> actions = parseJsonList(pred.getRecommendedClinicalActions());

        List<PredictionResultDto.TrajectoryPointDto> trajPoints = new ArrayList<>();
        if (pred.getTrajectoryPointsJson() != null && !pred.getTrajectoryPointsJson().isBlank()) {
            try {
                trajPoints = objectMapper.readValue(
                        pred.getTrajectoryPointsJson(),
                        new TypeReference<>() {}
                );
            } catch (Exception ignored) {}
        }

        return new PredictionResultDto(
                pred.getId(),
                patientId,
                u.getFullName(),
                pred.getPredictionType(),
                pred.getHorizonWindow(),
                pred.getRiskLevel(),
                pred.getProbability(),
                pred.getModelName(),
                pred.getHeadline(),
                pred.getClinicalExplanation(),
                factorDtos,
                pred.getHistoricalEvidence(),
                actions,
                pred.getConfidence(),
                pred.isSimulation(),
                pred.getDisclaimer(),
                pred.getCreatedAt().toString(),
                pred.getCurrentGlucose(),
                pred.getGlucoseVelocity(),
                pred.getProjectedGlucose120Min(),
                pred.getProjectedDelta(),
                pred.getTrajectoryDirection() != null ? pred.getTrajectoryDirection() : "STABLE",
                trajPoints
        );
    }

    /**
     * Retrieves wearable stream points for a specific metric (CGM glucose, heart_rate, hrv, sleep, steps, spo2).
     */
    @Transactional(readOnly = true)
    public WearableStreamDto getWearablesStream(UUID patientId, String rawMetric, int daysLimit) {
        if (!userRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found: " + patientId);
        }

        String metric = rawMetric != null ? rawMetric.trim().toLowerCase() : "glucose";
        Instant since = Instant.now().minus(Math.max(1, daysLimit), ChronoUnit.DAYS);

        List<HealthObservation> obsList = observationRepository.findTimelineObservations(patientId, since, Instant.now());
        List<WearableStreamDto.DataPointDto> points = new ArrayList<>();

        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal min = null;
        BigDecimal max = null;
        int count = 0;

        for (HealthObservation obs : obsList) {
            boolean matches = matchesMetric(obs, metric);
            if (matches) {
                BigDecimal val = obs.getStandardValueNumeric() != null ? obs.getStandardValueNumeric() : obs.getValueNumeric();
                if (val == null && obs instanceof LifestyleObservation lo && lo.getDurationMinutes() != null) {
                    val = BigDecimal.valueOf(lo.getDurationMinutes() / 60.0).setScale(1, RoundingMode.HALF_UP);
                }
                if (val != null) {
                    points.add(new WearableStreamDto.DataPointDto(
                            val,
                            obs.getTimestamp().toString(),
                            obs.getSource(),
                            obs.getConfidence()
                    ));
                    sum = sum.add(val);
                    if (min == null || val.compareTo(min) < 0) min = val;
                    if (max == null || val.compareTo(max) > 0) max = val;
                    count++;
                }
            }
        }

        // Sort chronologically ascending
        points.sort(Comparator.comparing(WearableStreamDto.DataPointDto::timestampIso));

        // Get personal baseline for this metric
        BaselineMetric baselineMetric = null;
        try {
            baselineMetric = baselineService.calculateSingleMetricBaseline(patientId, metric);
        } catch (Exception ignored) {}

        BigDecimal mean = count > 0 ? sum.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        Map<String, Object> stats = new HashMap<>();
        stats.put("count", count);
        stats.put("average", mean);
        stats.put("min", min != null ? min : BigDecimal.ZERO);
        stats.put("max", max != null ? max : BigDecimal.ZERO);

        return new WearableStreamDto(
                patientId,
                metric,
                resolveUnitForMetric(metric),
                points,
                baselineMetric != null && baselineMetric.getMean() != null ? baselineMetric.getMean() : mean,
                baselineMetric != null && baselineMetric.getMin() != null ? baselineMetric.getMin() : min,
                baselineMetric != null && baselineMetric.getMax() != null ? baselineMetric.getMax() : max,
                stats
        );
    }

    private MetabolicPredictionEngine.EngineeredFeatures engineerFeatures(
            BaselineProfile baseline,
            List<HealthObservation> recentObs,
            BigDecimal bmi,
            UUID patientId
    ) {
        // Find latest glucose readings to compute slope and CV
        List<BigDecimal> glucoseValues = new ArrayList<>();
        BigDecimal latestGlucose = new BigDecimal("95.0");
        BigDecimal priorGlucose = new BigDecimal("92.0");

        for (HealthObservation o : recentObs) {
            if (o.getObservationType() == ObservationType.BLOOD_GLUCOSE && o.getValueNumeric() != null) {
                glucoseValues.add(o.getValueNumeric());
            }
        }

        if (glucoseValues.size() >= 2) {
            latestGlucose = glucoseValues.get(glucoseValues.size() - 1);
            priorGlucose = glucoseValues.get(glucoseValues.size() - 2);
        } else if (glucoseValues.size() == 1) {
            latestGlucose = glucoseValues.get(0);
        }

        // Slope = (latest - prior)
        BigDecimal slope = latestGlucose.subtract(priorGlucose);

        // CV = std / mean * 100
        BigDecimal cv = BigDecimal.valueOf(18.0); // normal baseline CV
        if (glucoseValues.size() >= 3) {
            double gMean = glucoseValues.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(95.0);
            double gVar = glucoseValues.stream().mapToDouble(v -> Math.pow(v.doubleValue() - gMean, 2)).average().orElse(0.0);
            double gStd = Math.sqrt(gVar);
            if (gMean > 0) {
                cv = BigDecimal.valueOf((gStd / gMean) * 100.0).setScale(1, RoundingMode.HALF_UP);
            }
        }

        // Deviations from baseline profile
        BaselineMetric hrvMetric = findMetric(baseline, PersonalBaselineService.METRIC_HRV);
        BaselineMetric rhrMetric = findMetric(baseline, PersonalBaselineService.METRIC_RESTING_HEART_RATE);
        BaselineMetric sleepMetric = findMetric(baseline, PersonalBaselineService.METRIC_SLEEP_DURATION);
        BaselineMetric stepMetric = findMetric(baseline, PersonalBaselineService.METRIC_STEPS);

        BigDecimal hrvDevPct = hrvMetric != null ? hrvMetric.getDeviationPercentage() : BigDecimal.ZERO;
        Double hrvZ = hrvMetric != null ? hrvMetric.getZScore() : null;

        BigDecimal rhrDevBpm = rhrMetric != null ? rhrMetric.getDeviationFromBaseline() : BigDecimal.ZERO;
        Double rhrZ = rhrMetric != null ? rhrMetric.getZScore() : null;

        // Sleep deficit: baselineMean - recentAvg
        BigDecimal sleepDeficit = BigDecimal.ZERO;
        if (sleepMetric != null && sleepMetric.getMean() != null && sleepMetric.getRecentAverage() != null) {
            sleepDeficit = sleepMetric.getMean().subtract(sleepMetric.getRecentAverage());
        }

        BigDecimal stepDevPct = stepMetric != null ? stepMetric.getDeviationPercentage() : BigDecimal.ZERO;

        // Fetch HbA1c and Fasting Glucose from lab results
        BigDecimal hba1c = new BigDecimal("5.4");
        BigDecimal fastingGlucose = new BigDecimal("88.0");
        List<LabResult> labs = labResultRepository.findByUserIdOrderByCollectionDateDesc(patientId);
        for (LabResult l : labs) {
            String b = l.getStandardizedBiomarker() != null ? l.getStandardizedBiomarker().toLowerCase() : "";
            if (b.contains("hba1c") || b.contains("a1c")) {
                hba1c = l.getValue();
            } else if (b.contains("glucose")) {
                fastingGlucose = l.getValue();
            }
        }

        // Check static EHR: Established Type 2 Diabetes diagnosis [Synthea FHIR]
        boolean hasT2D = medicalRecordRepository.findByUserIdOrderByDiagnosedDateDesc(patientId)
                .stream()
                .anyMatch(r -> r.getConditionOrDiagnosis() != null &&
                        (r.getConditionOrDiagnosis().toLowerCase().contains("diabetes") ||
                         "44054006".equals(r.getIcd10Code())));

        // Determine activity level from telemetry / step deviation
        String activityLevel = "MODERATE";
        if (stepDevPct != null && stepDevPct.doubleValue() >= 25.0) {
            activityLevel = "VIGOROUS";
        } else if (stepDevPct != null && stepDevPct.doubleValue() <= -25.0) {
            activityLevel = "SEDENTARY";
        }

        // Determine dynamic telemetry inputs if available
        BigDecimal dynamicVelocity = null;
        BigDecimal dynamicConfidence = BigDecimal.valueOf(0.98);
        Instant dynamicTimestamp = null;
        if (telemetryRepository != null) {
            Optional<org.os4all.modules.digitaltwin.telemetry.entity.PatientTelemetry> telOpt = telemetryRepository.findTopByUserIdOrderByTimestampDesc(patientId);
            if (telOpt.isPresent()) {
                org.os4all.modules.digitaltwin.telemetry.entity.PatientTelemetry tel = telOpt.get();
                if (tel.getGlucose() != null) {
                    latestGlucose = tel.getGlucose();
                }
                if (tel.getGlucoseVelocity() != null) {
                    dynamicVelocity = tel.getGlucoseVelocity();
                }
                if (tel.getConfidence() != null) {
                    dynamicConfidence = tel.getConfidence();
                }
                dynamicTimestamp = tel.getTimestamp();
                if (tel.getActivityLevel() != null && !tel.getActivityLevel().isBlank()) {
                    activityLevel = tel.getActivityLevel().toUpperCase();
                }
            }
        }

        if (dynamicVelocity == null) {
            dynamicVelocity = slope.divide(BigDecimal.valueOf(5.0), 2, RoundingMode.HALF_UP);
        }

        BaselineMetric glucoseMetric = findMetric(baseline, "glucose");
        BigDecimal baselineGlucoseMean = (glucoseMetric != null && glucoseMetric.getMean() != null)
                ? glucoseMetric.getMean()
                : fastingGlucose;

        return new MetabolicPredictionEngine.EngineeredFeatures(
                latestGlucose,
                slope,
                cv,
                hrvDevPct,
                hrvZ,
                rhrDevBpm,
                rhrZ,
                sleepDeficit,
                stepDevPct,
                bmi != null ? bmi : new BigDecimal("23.5"),
                hba1c,
                fastingGlucose,
                14, // 2 PM typical afternoon post-meal
                true, // post-prandial evaluation window
                hasT2D,
                activityLevel,
                dynamicVelocity,
                baselineGlucoseMean,
                dynamicConfidence,
                dynamicTimestamp
        );
    }

    private DigitalTwinStateDto.CurrentVitalsSnapshot extractCurrentVitalsSnapshot(UUID patientId) {
        Instant past3Days = Instant.now().minus(3, ChronoUnit.DAYS);
        List<HealthObservation> obs = observationRepository.findTimelineObservations(patientId, past3Days, Instant.now());

        BigDecimal hr = new BigDecimal("72.0");
        BigDecimal rhr = new BigDecimal("62.0");
        BigDecimal hrv = new BigDecimal("54.0");
        BigDecimal spo2 = new BigDecimal("98.5");
        BigDecimal glucose = new BigDecimal("95.0");
        BigDecimal sleep = new BigDecimal("7.5");
        BigDecimal steps = new BigDecimal("8500");
        BigDecimal temp = new BigDecimal("36.6");

        for (HealthObservation o : obs) {
            BigDecimal v = o.getStandardValueNumeric() != null ? o.getStandardValueNumeric() : o.getValueNumeric();
            if (v == null && o instanceof LifestyleObservation lo && lo.getDurationMinutes() != null) {
                v = BigDecimal.valueOf(lo.getDurationMinutes() / 60.0);
            }
            if (v == null) continue;

            if (o.getObservationType() == ObservationType.BLOOD_GLUCOSE) glucose = v;
            else if (o.getObservationType() == ObservationType.HRV) hrv = v;
            else if (o.getObservationType() == ObservationType.OXYGEN_SATURATION) spo2 = v;
            else if (o.getObservationType() == ObservationType.BODY_TEMPERATURE) temp = v;
            else if (o.getObservationType() == ObservationType.STEPS) steps = v;
            else if (o.getObservationType() == ObservationType.SLEEP) sleep = v;
            else if (o.getObservationType() == ObservationType.HEART_RATE) {
                if (o instanceof VitalMeasurement vm && vm.getVitalName() != null && vm.getVitalName().toLowerCase().contains("resting")) {
                    rhr = v;
                } else {
                    hr = v;
                }
            }
        }

        return new DigitalTwinStateDto.CurrentVitalsSnapshot(
                hr, rhr, hrv, spo2, glucose, sleep, steps, temp
        );
    }

    private List<DigitalTwinStateDto.BaselineDeviationDto> buildBaselineDeviations(BaselineProfile profile) {
        List<DigitalTwinStateDto.BaselineDeviationDto> list = new ArrayList<>();
        for (BaselineMetric m : profile.getMetrics()) {
            list.add(new DigitalTwinStateDto.BaselineDeviationDto(
                    m.getMetric(),
                    m.getMean(),
                    m.getRecentAverage() != null ? m.getRecentAverage() : m.getMean(),
                    m.getDeviationFromBaseline() != null ? m.getDeviationFromBaseline() : BigDecimal.ZERO,
                    m.getDeviationPercentage() != null ? m.getDeviationPercentage() : BigDecimal.ZERO,
                    m.getZScore(),
                    m.getUnit(),
                    m.getTrendDirection() != null ? m.getTrendDirection() : "STABLE",
                    m.getInterpretation()
            ));
        }
        return list;
    }

    private BaselineMetric findMetric(BaselineProfile profile, String metricKey) {
        return profile.getMetrics().stream()
                .filter(m -> m.getMetric().equalsIgnoreCase(metricKey))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesMetric(HealthObservation obs, String metric) {
        String type = obs.getObservationType().name().toLowerCase();
        if (metric.contains("glucose")) return obs.getObservationType() == ObservationType.BLOOD_GLUCOSE;
        if (metric.contains("hrv")) return obs.getObservationType() == ObservationType.HRV;
        if (metric.contains("spo2") || metric.contains("oxygen")) return obs.getObservationType() == ObservationType.OXYGEN_SATURATION;
        if (metric.contains("sleep")) return obs.getObservationType() == ObservationType.SLEEP;
        if (metric.contains("step")) return obs.getObservationType() == ObservationType.STEPS;
        if (metric.contains("temp")) return obs.getObservationType() == ObservationType.BODY_TEMPERATURE;
        if (metric.contains("heart") || metric.contains("hr")) {
            if (metric.contains("resting")) {
                return obs instanceof VitalMeasurement vm && vm.getVitalName() != null && vm.getVitalName().toLowerCase().contains("resting");
            }
            return obs.getObservationType() == ObservationType.HEART_RATE;
        }
        return false;
    }

    private String resolveUnitForMetric(String metric) {
        return switch (metric) {
            case "glucose", "blood_glucose", "cgm" -> "mg/dL";
            case "hrv" -> "ms";
            case "heart_rate", "resting_heart_rate" -> "bpm";
            case "spo2" -> "%";
            case "sleep", "sleep_duration" -> "hours";
            case "steps" -> "steps";
            case "temperature" -> "°C";
            default -> "units";
        };
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Arrays.stream(json.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    private Integer calculateAge(LocalDate dob) {
        if (dob == null) return 38;
        return (int) ChronoUnit.YEARS.between(dob, LocalDate.now());
    }

    private BigDecimal calculateBmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null || heightCm.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("23.8");
        }
        double hMeters = heightCm.doubleValue() / 100.0;
        double bmi = weightKg.doubleValue() / (hMeters * hMeters);
        return BigDecimal.valueOf(bmi).setScale(1, RoundingMode.HALF_UP);
    }
}
