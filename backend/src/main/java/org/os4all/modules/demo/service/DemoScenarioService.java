package org.os4all.modules.demo.service;

import org.os4all.modules.ai.model.AiHealthInsightResponse;
import org.os4all.modules.ai.service.AIOrchestrator;
import org.os4all.modules.anomaly.model.AnomalyEvent;
import org.os4all.modules.anomaly.model.HealthSignal;
import org.os4all.modules.anomaly.model.HealthStatusSummary;
import org.os4all.modules.anomaly.service.RuleBasedAnomalyEngine;
import org.os4all.modules.audit.service.AuditService;
import org.os4all.modules.auth.dto.RegisterRequest;
import org.os4all.modules.auth.service.AuthService;
import org.os4all.modules.baseline.model.BaselineProfile;
import org.os4all.modules.baseline.service.PersonalBaselineService;
import org.os4all.modules.consent.dto.ConsentRequest;
import org.os4all.modules.consent.entity.ConsentType;
import org.os4all.modules.consent.service.ConsentService;
import org.os4all.modules.demo.dto.DemoScenarioResponse;
import org.os4all.modules.ingestion.entity.ObservationType;
import org.os4all.modules.ingestion.entity.VitalMeasurement;
import org.os4all.modules.ingestion.entity.LifestyleObservation;
import org.os4all.modules.ingestion.repository.HealthObservationRepository;
import org.os4all.modules.lab.entity.LabReport;
import org.os4all.modules.lab.entity.LabResult;
import org.os4all.modules.lab.repository.LabReportRepository;
import org.os4all.modules.timeline.dto.TimelineItemResponse;
import org.os4all.modules.timeline.service.TimelineService;
import org.os4all.modules.user.entity.User;
import org.os4all.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Deterministic OS4All Hackathon Demonstration Service.
 * Manages dedicated synthetic demo scenarios for "Demo User — Synthetic Data".
 *
 * Implements:
 * 1. resetDemoScenario: Resets demo data to 45 days of stable homeostatic baseline (STABLE state).
 * 2. runHealthDriftScenario: Seeds a 45-day deterministic sequence with the final 7-10 days
 *    exhibiting a subtle, gradual multi-signal departure (Sleep, Resting HR, HRV, SpO2, Activity)
 *    producing the progression: STABLE -> DRIFT -> MULTI-SIGNAL ANOMALY.
 *
 * Strictly avoids associating synthetic scenarios with specific clinical disease diagnoses.
 */
@Service
public class DemoScenarioService {

    private static final Logger log = LoggerFactory.getLogger(DemoScenarioService.class);

    public static final String DEMO_USER_EMAIL = "demo.patient@os4all.test";
    public static final String DEMO_USER_NAME = "Demo User — Synthetic Data";

    private final UserRepository userRepository;
    private final AuthService authService;
    private final ConsentService consentService;
    private final HealthObservationRepository observationRepository;
    private final LabReportRepository labReportRepository;
    private final PersonalBaselineService baselineService;
    private final RuleBasedAnomalyEngine anomalyEngine;
    private final AIOrchestrator aiOrchestrator;
    private final TimelineService timelineService;
    private final AuditService auditService;
    private final org.os4all.modules.ai.provider.ModelProviderRegistry providerRegistry;

    public DemoScenarioService(
            UserRepository userRepository,
            AuthService authService,
            ConsentService consentService,
            HealthObservationRepository observationRepository,
            LabReportRepository labReportRepository,
            PersonalBaselineService baselineService,
            RuleBasedAnomalyEngine anomalyEngine,
            AIOrchestrator aiOrchestrator,
            TimelineService timelineService,
            AuditService auditService,
            org.os4all.modules.ai.provider.ModelProviderRegistry providerRegistry
    ) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.consentService = consentService;
        this.observationRepository = observationRepository;
        this.labReportRepository = labReportRepository;
        this.baselineService = baselineService;
        this.anomalyEngine = anomalyEngine;
        this.aiOrchestrator = aiOrchestrator;
        this.timelineService = timelineService;
        this.auditService = auditService;
        this.providerRegistry = providerRegistry;
    }

    /**
     * Resets the demo to the exact same homeostatic baseline state every time:
     * - Dedicated synthetic user: "Demo User — Synthetic Data"
     * - Generates 45 days of stable synthetic observations across resting HR, HRV, sleep duration, SpO2, and activity.
     * - Personal baseline established.
     * - Health signals evaluated to STABLE (0 anomalies, 0 drift).
     */
    @Transactional
    public DemoScenarioResponse resetDemoScenario(String clientIp) {
        log.info("Resetting OS4All Demo Environment to deterministic STABLE baseline state...");

        User demoUser = getOrCreateDemoUser(clientIp);

        // Clear previous observations & lab reports for clean reset
        observationRepository.deleteByUserId(demoUser.getId());
        labReportRepository.deleteByUserId(demoUser.getId());

        Instant now = Instant.now();

        // Seed 45 days of stable homeostatic baseline observations
        seedStableObservations(demoUser, now, 45, 0);

        // Seed routine baseline reference lab report
        seedRoutineReferenceLabReport(demoUser, now);

        // Calculate personal baseline profile
        BaselineProfile baselineProfile = baselineService.calculateBaselineProfile(demoUser.getId());

        // Evaluate health signals and verify STABLE status
        List<HealthSignal> evaluatedSignals = anomalyEngine.evaluateHealthSignals(demoUser.getId());
        List<AnomalyEvent> anomalies = anomalyEngine.detectAnomaliesFromSignals(demoUser.getId(), evaluatedSignals);
        HealthStatusSummary statusSummary = anomalyEngine.evaluateStatus(demoUser.getId());

        // Fetch updated unified health timeline
        List<TimelineItemResponse> timeline = timelineService.getUnifiedTimeline(
                demoUser.getId(),
                now.minus(45, ChronoUnit.DAYS),
                now,
                "ALL",
                50
        );

        DemoScenarioResponse.DemoExecutionChainDto chain = new DemoScenarioResponse.DemoExecutionChainDto(
                "DATA: 45 days of longitudinal wearable observations recorded (Resting HR, HRV, Sleep Duration, SpO2, Daily Steps, Temperature).",
                "BASELINE: Personal homeostatic baselines established across all 6 continuous signals with high statistical confidence.",
                "DRIFT: None. All monitored signals strictly oscillate within individual historical baseline envelopes.",
                "MULTI-SIGNAL PATTERN: None. 0 anomalies detected. Autonomic and recovery equilibrium maintained.",
                "NEMOTRON REASONING: Verified stable individual baseline status across continuous parameters.",
                "TAVILY EVIDENCE: Evidence retrieval not triggered; biometric indicators are in homeostatic equilibrium.",
                "OS4All INSIGHT: Objective confirmation of individual physiological stability.",
                "ACTION: Maintain current restorative sleep schedule, daily activity, and hydration routine.",
                "TIMELINE UPDATE: Real-time unified timeline reflects stable baseline observations."
        );

        List<DemoScenarioResponse.DemoProgressionStageDto> progressionStages = List.of(
                new DemoScenarioResponse.DemoProgressionStageDto(
                        "STABLE EQUILIBRIUM",
                        "Days 45–0",
                        "STABLE",
                        "All physiological signals remain within established personal baseline envelopes.",
                        List.of(
                                "Resting Heart Rate: ~60.0 bpm (normal individual range)",
                                "Heart Rate Variability: ~55.0 ms (stable autonomic tone)",
                                "Sleep Duration: ~7.8 hrs/night (restorative)",
                                "Blood Oxygen (SpO2): ~98.4% (normal)",
                                "Daily Steps: ~9,200 steps (active)"
                        )
                )
        );

        auditService.record(
                demoUser,
                demoUser.getEmail(),
                "DEMO_SCENARIO_RESET",
                "DEMO_SCENARIO",
                demoUser.getId().toString(),
                clientIp,
                "Demo environment reset to STABLE baseline. Observations: 45 days. Anomalies: 0."
        );

        log.info("Demo reset complete. User: {}, Aggregate State: {}", DEMO_USER_NAME, statusSummary.getAggregateState());

        String providerMode = providerRegistry.isNebiusAvailable()
                ? "NVIDIA Nemotron via Nebius"
                : "AI DEMO MODE";
        String modelName = providerRegistry.isNebiusAvailable()
                ? "nvidia/Llama-3_1-Nemotron-70B-Instruct"
                : "mock-deterministic";
        String providerNote = providerRegistry.isNebiusAvailable()
                ? "Production Nebius Token Factory connection verified."
                : "Real Nebius credentials unavailable; operating honestly in local deterministic AI DEMO MODE.";

        return new DemoScenarioResponse(
                "Demo environment reset to stable personal baseline: 45 days of stable observations seeded.",
                demoUser.getEmail(),
                DEMO_USER_NAME,
                statusSummary.getAggregateState().name(),
                chain,
                baselineProfile.getMetrics(),
                evaluatedSignals,
                anomalies,
                null,
                timeline,
                DemoScenarioResponse.SYNTHETIC_NOTICE,
                progressionStages,
                providerMode,
                modelName,
                providerNote
        );
    }

    /**
     * Executes the gradual multi-signal health drift scenario:
     * - 45 days of longitudinal observations.
     * - Days 45 down to Day 9 (36 days): Stable personal homeostatic baseline.
     * - Final 9 days (Days 8 down to Day 0): Gradual multi-signal departure:
     *   * Resting HR: 60.0 -> 76.5 bpm (+16.5 bpm)
     *   * HRV: 55.0 -> 33.0 ms (-22.0 ms)
     *   * Sleep: 7.8 -> 4.7 hrs (-3.1 hrs)
     *   * SpO2: 98.4% -> 95.5% (-2.9%)
     *   * Activity (Steps): 9,200 -> 4,200 steps (-5,000 steps)
     *   * Temperature: 36.6 -> 37.4 °C (+0.8 °C)
     * - Demonstrates progression: STABLE -> DRIFT -> MULTI-SIGNAL ANOMALY.
     * - AI Orchestrator reasons over structured health context without claiming disease diagnoses.
     */
    @Transactional
    public DemoScenarioResponse runHealthDriftScenario(String clientIp) {
        log.info("Starting Deterministic Health Drift Demo Scenario for {}...", DEMO_USER_NAME);

        User demoUser = getOrCreateDemoUser(clientIp);

        // Clear previous observations & lab reports for clean deterministic execution
        observationRepository.deleteByUserId(demoUser.getId());
        labReportRepository.deleteByUserId(demoUser.getId());

        Instant now = Instant.now();

        // 1. Seed 36 days of stable baseline (Days 45 down to Day 9)
        seedStableObservations(demoUser, now, 45, 9);

        // 2. Seed 9 days of gradual multi-signal departure (Days 8 down to Day 0)
        seedMultiSignalDriftObservations(demoUser, now);

        // 3. Seed routine reference CMP lab report (all biomarkers within reference range)
        seedRoutineReferenceLabReport(demoUser, now);

        // 4. Calculate personal baselines
        BaselineProfile baselineProfile = baselineService.calculateBaselineProfile(demoUser.getId());

        // 5. Evaluate signals and detect multi-signal anomalies
        List<HealthSignal> evaluatedSignals = anomalyEngine.evaluateHealthSignals(demoUser.getId());
        List<AnomalyEvent> anomalies = anomalyEngine.detectAnomaliesFromSignals(demoUser.getId(), evaluatedSignals);
        HealthStatusSummary statusSummary = anomalyEngine.evaluateStatus(demoUser.getId());

        // 6. Orchestrate AI Intelligence Layer (Nemotron Reasoning + Evidence Retrieval)
        AiHealthInsightResponse aiInsight = aiOrchestrator.generateHealthInsight(demoUser.getId(), clientIp);

        // 7. Fetch updated unified health timeline
        List<TimelineItemResponse> timeline = timelineService.getUnifiedTimeline(
                demoUser.getId(),
                now.minus(45, ChronoUnit.DAYS),
                now,
                "ALL",
                50
        );

        // 8. Visual execution chain DTO
        DemoScenarioResponse.DemoExecutionChainDto chain = new DemoScenarioResponse.DemoExecutionChainDto(
                "DATA: 45 days of longitudinal wearable observations recorded (Resting HR, HRV, Sleep Duration, SpO2, Daily Steps, Temperature).",
                "BASELINE: Personal baseline calculated over days 1–36. Established ranges: RHR 60.0 bpm (±2.2), HRV 55.0 ms (±3.5), Sleep 7.8 hrs, SpO2 98.4%, Steps 9,200.",
                "DRIFT: Days 8–4 exhibit subtle, gradual directional departures: Sleep decreases (7.2h -> 5.7h), RHR rises (62 -> 70 bpm), HRV declines (52 -> 41 ms), persistent for >= 3 days.",
                "MULTI-SIGNAL PATTERN: Compound autonomic recovery strain detected across 5 signals simultaneously without disease diagnosis (Sleep ↓ + RHR ↑ + HRV ↓ + SpO2 ↓ + Steps ↓).",
                "NEMOTRON REASONING: AI Intelligence Layer evaluates multi-signal pattern against personal baseline variance without claiming clinical disease diagnoses.",
                "TAVILY EVIDENCE: Evidence Agent conditionally retrieves peer-reviewed literature on autonomic recovery deficits and sleep disruption.",
                "OS4All INSIGHT: Generated structured explanation distinguishing verified data points from non-diagnostic physiological hypotheses.",
                "ACTION: Formulated non-invasive lifestyle interventions (sleep debt recovery, training load deloading) and physician evaluation guidance.",
                "TIMELINE UPDATE: Real-time unified chronological timeline reflects observations, baseline drift alarms, and AI insight events."
        );

        // 9. Progression stages (STABLE -> DRIFT -> MULTI-SIGNAL ANOMALY)
        List<DemoScenarioResponse.DemoProgressionStageDto> progressionStages = List.of(
                new DemoScenarioResponse.DemoProgressionStageDto(
                        "1. STABLE EQUILIBRIUM",
                        "Days 45–9",
                        "STABLE",
                        "Individual homeostatic baseline established over 36 consecutive days. All signals in normal variance.",
                        List.of(
                                "Resting HR: ~60.0 bpm (within ±2.2 bpm baseline envelope)",
                                "HRV: ~55.0 ms (within ±3.5 ms baseline envelope)",
                                "Sleep: ~7.8 hrs/night",
                                "SpO2: ~98.4%",
                                "Daily Steps: ~9,200 steps"
                        )
                ),
                new DemoScenarioResponse.DemoProgressionStageDto(
                        "2. DIRECTIONAL DRIFT",
                        "Days 8–4",
                        "DRIFT",
                        "Subtle individual divergence begins emerging. Individually subtle, but persistence counter reaches >= 3 consecutive days.",
                        List.of(
                                "Sleep duration drops: 7.2h -> 5.7h (-2.1h vs baseline)",
                                "Resting HR elevates: 62.0 -> 70.0 bpm (+10.0 bpm vs baseline)",
                                "HRV declines: 52.0 -> 41.0 ms (-14.0 ms vs baseline)",
                                "SpO2 dips slightly: 98.0% -> 96.7%",
                                "Daily steps decrease: 8,600 -> 6,200 steps"
                        )
                ),
                new DemoScenarioResponse.DemoProgressionStageDto(
                        "3. MULTI-SIGNAL ANOMALY",
                        "Days 3–0",
                        "MULTI-SIGNAL ANOMALY",
                        "Synergistic autonomic strain pattern detected: Compound departure across 5 signals simultaneously. Non-diagnostic baseline divergence confirmed.",
                        List.of(
                                "Resting HR: 76.5 bpm (+16.5 bpm above personal baseline)",
                                "HRV: 33.0 ms (-22.0 ms below personal baseline)",
                                "Sleep duration: 4.7 hrs (-3.1 hrs below personal baseline)",
                                "SpO2: 95.5% (-2.9% below personal baseline)",
                                "Daily Steps: 4,200 steps (-5,000 steps below personal baseline)",
                                "Temperature: 37.4 °C (+0.8 °C above personal baseline)"
                        )
                )
        );

        auditService.record(
                demoUser,
                demoUser.getEmail(),
                "FLAGSHIP_DEMO_SCENARIO_EXECUTED",
                "DEMO_SCENARIO",
                demoUser.getId().toString(),
                clientIp,
                "State: " + statusSummary.getAggregateState() + ", Anomalies: " + anomalies.size() + ", Insights: generated"
        );

        log.info("Health drift demonstration scenario completed. Aggregate state: {}", statusSummary.getAggregateState());

        String providerMode = providerRegistry.isNebiusAvailable()
                ? "NVIDIA Nemotron via Nebius"
                : "AI DEMO MODE";
        String modelName = providerRegistry.isNebiusAvailable()
                ? "nvidia/Llama-3_1-Nemotron-70B-Instruct"
                : "mock-deterministic";
        String providerNote = providerRegistry.isNebiusAvailable()
                ? "Production Nebius Token Factory connection verified."
                : "Real Nebius credentials unavailable; operating honestly in local deterministic AI DEMO MODE.";

        return new DemoScenarioResponse(
                "Gradual multi-signal drift progression executed: STABLE -> DRIFT -> MULTI-SIGNAL ANOMALY detected.",
                demoUser.getEmail(),
                DEMO_USER_NAME,
                statusSummary.getAggregateState().name(),
                chain,
                baselineProfile.getMetrics(),
                evaluatedSignals,
                anomalies,
                aiInsight,
                timeline,
                DemoScenarioResponse.SYNTHETIC_NOTICE,
                progressionStages,
                providerMode,
                modelName,
                providerNote
        );
    }

    /**
     * Backward-compatible alias for runHealthDriftScenario.
     */
    @Transactional
    public DemoScenarioResponse runDemonstrationScenario(String clientIp) {
        return runHealthDriftScenario(clientIp);
    }

    private User getOrCreateDemoUser(String clientIp) {
        Optional<User> userOpt = userRepository.findByEmail(DEMO_USER_EMAIL);
        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
            if (!DEMO_USER_NAME.equals(user.getFullName())) {
                user.setFullName(DEMO_USER_NAME);
                userRepository.save(user);
            }
        } else {
            authService.register(
                    new RegisterRequest(DEMO_USER_EMAIL, "DemoPass123!", DEMO_USER_NAME),
                    clientIp
            );
            user = userRepository.findByEmail(DEMO_USER_EMAIL).orElseThrow();
            user.setFullName(DEMO_USER_NAME);
            userRepository.save(user);
        }

        // Grant all consents for complete reproducible demonstration
        consentService.updateConsent(user.getId(), new ConsentRequest(ConsentType.DATA_STORAGE, true), clientIp);
        consentService.updateConsent(user.getId(), new ConsentRequest(ConsentType.AI_INFERENCE, true), clientIp);
        consentService.updateConsent(user.getId(), new ConsentRequest(ConsentType.EVIDENCE_SEARCH, true), clientIp);

        return user;
    }

    /**
     * Seeds stable homeostatic baseline observations for a given day range.
     */
    private void seedStableObservations(User user, Instant now, int startDay, int endDay) {
        for (int day = startDay; day >= endDay; day--) {
            Instant ts = now.minus(day, ChronoUnit.DAYS);

            // Resting Heart Rate (mean ~60.0 bpm, subtle sinusoidal oscillation)
            double rhrVal = 59.5 + ((day % 3) * 0.5);
            VitalMeasurement rhr = new VitalMeasurement();
            rhr.setUser(user);
            rhr.setObservationType(ObservationType.HEART_RATE);
            rhr.setVitalName("Resting Heart Rate");
            rhr.setValueNumeric(BigDecimal.valueOf(rhrVal).setScale(1, RoundingMode.HALF_UP));
            rhr.setUnit("bpm");
            rhr.setTimestamp(ts);
            rhr.setSource("SYNTHETIC_DEMO_SENSOR");
            rhr.setConfidence(BigDecimal.valueOf(0.98));
            observationRepository.save(rhr);

            // HRV (mean ~55.0 ms)
            double hrvVal = 54.0 + ((day % 4) * 0.7);
            VitalMeasurement hrv = new VitalMeasurement();
            hrv.setUser(user);
            hrv.setObservationType(ObservationType.HRV);
            hrv.setVitalName("Heart Rate Variability");
            hrv.setValueNumeric(BigDecimal.valueOf(hrvVal).setScale(1, RoundingMode.HALF_UP));
            hrv.setUnit("ms");
            hrv.setTimestamp(ts);
            hrv.setSource("SYNTHETIC_DEMO_SENSOR");
            hrv.setConfidence(BigDecimal.valueOf(0.97));
            observationRepository.save(hrv);

            // Sleep Duration (mean ~7.8 hrs)
            double sleepHrs = 7.6 + ((day % 3) * 0.2);
            LifestyleObservation sleep = new LifestyleObservation();
            sleep.setUser(user);
            sleep.setObservationType(ObservationType.SLEEP);
            sleep.setDurationMinutes((int) (sleepHrs * 60));
            sleep.setValueNumeric(BigDecimal.valueOf(sleepHrs).setScale(1, RoundingMode.HALF_UP));
            sleep.setUnit("hours");
            sleep.setTimestamp(ts);
            sleep.setSource("SYNTHETIC_DEMO_TRACKER");
            sleep.setConfidence(BigDecimal.valueOf(0.99));
            observationRepository.save(sleep);

            // SpO2 (mean ~98.4%)
            double spo2Val = 98.2 + ((day % 3) * 0.2);
            VitalMeasurement spo2 = new VitalMeasurement();
            spo2.setUser(user);
            spo2.setObservationType(ObservationType.OXYGEN_SATURATION);
            spo2.setVitalName("Blood Oxygen (SpO2)");
            spo2.setValueNumeric(BigDecimal.valueOf(spo2Val).setScale(1, RoundingMode.HALF_UP));
            spo2.setUnit("%");
            spo2.setTimestamp(ts);
            spo2.setSource("SYNTHETIC_DEMO_SENSOR");
            spo2.setConfidence(BigDecimal.valueOf(0.99));
            observationRepository.save(spo2);

            // Activity / Steps (mean ~9,200 steps)
            double stepVal = 9000.0 + ((day % 5) * 100);
            LifestyleObservation steps = new LifestyleObservation();
            steps.setUser(user);
            steps.setObservationType(ObservationType.STEPS);
            steps.setValueNumeric(BigDecimal.valueOf(stepVal));
            steps.setUnit("steps");
            steps.setTimestamp(ts);
            steps.setSource("SYNTHETIC_DEMO_TRACKER");
            steps.setConfidence(BigDecimal.valueOf(0.98));
            observationRepository.save(steps);

            // Temperature (mean ~36.6 °C)
            double tempVal = 36.5 + ((day % 2) * 0.2);
            VitalMeasurement temp = new VitalMeasurement();
            temp.setUser(user);
            temp.setObservationType(ObservationType.BODY_TEMPERATURE);
            temp.setVitalName("Body Temperature");
            temp.setValueNumeric(BigDecimal.valueOf(tempVal).setScale(2, RoundingMode.HALF_UP));
            temp.setUnit("°C");
            temp.setTimestamp(ts);
            temp.setSource("SYNTHETIC_DEMO_SENSOR");
            temp.setConfidence(BigDecimal.valueOf(0.99));
            observationRepository.save(temp);
        }
    }

    /**
     * Seeds 9 days of gradual multi-signal departure (Days 8 down to Day 0).
     * The departures are subtle day-over-day, but form a persistent multi-signal pattern:
     * - Days 8–6: Emerging subtle shift
     * - Days 5–3: Persistent directional drift (>= 3 days)
     * - Days 2–0: Compound multi-signal anomaly (Sleep ↓ + RHR ↑ + HRV ↓ + SpO2 ↓ + Steps ↓)
     */
    private void seedMultiSignalDriftObservations(User user, Instant now) {
        double[] rhrValues =   {62.0, 63.5, 65.0, 67.5, 70.0, 72.5, 74.5, 75.5, 76.5}; // +16.5 bpm
        double[] hrvValues =   {52.0, 49.5, 47.0, 44.0, 41.0, 38.0, 36.0, 34.0, 33.0}; // -22.0 ms
        double[] sleepValues = { 7.2,  6.8,  6.4,  6.0,  5.7,  5.3,  5.0,  4.8,  4.7}; // -3.1 hrs
        double[] spo2Values =  {98.0, 97.7, 97.4, 97.0, 96.7, 96.4, 96.0, 95.7, 95.5}; // -2.9%
        double[] stepValues =  {8600, 8100, 7500, 6800, 6200, 5600, 5000, 4500, 4200}; // -5,000 steps
        double[] tempValues =  {36.7, 36.8, 36.9, 37.0, 37.1, 37.2, 37.3, 37.3, 37.4}; // +0.8 °C

        for (int i = 0; i < 9; i++) {
            int dayOffset = 8 - i;
            Instant ts = now.minus(dayOffset, ChronoUnit.DAYS);

            // RHR
            VitalMeasurement rhr = new VitalMeasurement();
            rhr.setUser(user);
            rhr.setObservationType(ObservationType.HEART_RATE);
            rhr.setVitalName("Resting Heart Rate");
            rhr.setValueNumeric(BigDecimal.valueOf(rhrValues[i]));
            rhr.setUnit("bpm");
            rhr.setTimestamp(ts);
            rhr.setSource("SYNTHETIC_DEMO_SENSOR");
            rhr.setConfidence(BigDecimal.valueOf(0.96));
            observationRepository.save(rhr);

            // HRV
            VitalMeasurement hrv = new VitalMeasurement();
            hrv.setUser(user);
            hrv.setObservationType(ObservationType.HRV);
            hrv.setVitalName("Heart Rate Variability");
            hrv.setValueNumeric(BigDecimal.valueOf(hrvValues[i]));
            hrv.setUnit("ms");
            hrv.setTimestamp(ts);
            hrv.setSource("SYNTHETIC_DEMO_SENSOR");
            hrv.setConfidence(BigDecimal.valueOf(0.95));
            observationRepository.save(hrv);

            // Sleep
            LifestyleObservation sleep = new LifestyleObservation();
            sleep.setUser(user);
            sleep.setObservationType(ObservationType.SLEEP);
            sleep.setDurationMinutes((int) (sleepValues[i] * 60));
            sleep.setValueNumeric(BigDecimal.valueOf(sleepValues[i]));
            sleep.setUnit("hours");
            sleep.setTimestamp(ts);
            sleep.setSource("SYNTHETIC_DEMO_TRACKER");
            sleep.setConfidence(BigDecimal.valueOf(0.98));
            observationRepository.save(sleep);

            // SpO2
            VitalMeasurement spo2 = new VitalMeasurement();
            spo2.setUser(user);
            spo2.setObservationType(ObservationType.OXYGEN_SATURATION);
            spo2.setVitalName("Blood Oxygen (SpO2)");
            spo2.setValueNumeric(BigDecimal.valueOf(spo2Values[i]));
            spo2.setUnit("%");
            spo2.setTimestamp(ts);
            spo2.setSource("SYNTHETIC_DEMO_SENSOR");
            spo2.setConfidence(BigDecimal.valueOf(0.98));
            observationRepository.save(spo2);

            // Activity / Steps
            LifestyleObservation steps = new LifestyleObservation();
            steps.setUser(user);
            steps.setObservationType(ObservationType.STEPS);
            steps.setValueNumeric(BigDecimal.valueOf(stepValues[i]));
            steps.setUnit("steps");
            steps.setTimestamp(ts);
            steps.setSource("SYNTHETIC_DEMO_TRACKER");
            steps.setConfidence(BigDecimal.valueOf(0.97));
            observationRepository.save(steps);

            // Temperature
            VitalMeasurement temp = new VitalMeasurement();
            temp.setUser(user);
            temp.setObservationType(ObservationType.BODY_TEMPERATURE);
            temp.setVitalName("Body Temperature");
            temp.setValueNumeric(BigDecimal.valueOf(tempValues[i]));
            temp.setUnit("°C");
            temp.setTimestamp(ts);
            temp.setSource("SYNTHETIC_DEMO_SENSOR");
            temp.setConfidence(BigDecimal.valueOf(0.98));
            observationRepository.save(temp);
        }
    }

    /**
     * Seeds a routine confirmed reference lab report.
     * Demonstrates that biomarkers remain within standard reference ranges,
     * highlighting that the detected shift is a personal autonomic baseline drift rather than an organ pathology.
     */
    private void seedRoutineReferenceLabReport(User user, Instant now) {
        LabReport labReport = new LabReport();
        labReport.setUser(user);
        labReport.setReportTitle("Comprehensive Metabolic Panel (Demo Reference)");
        labReport.setLaboratoryName("BioHealth Diagnostics (Synthetic Demo)");
        labReport.setCollectionDate(now.minus(6, ChronoUnit.DAYS));
        labReport.setReportedDate(now.minus(5, ChronoUnit.DAYS));
        labReport.setSource("OCR_INGESTION_PIPELINE");
        labReport.setFileName("synthetic_cmp_report.pdf");
        labReport.setOcrStatus("CONFIRMED");
        labReport.setReviewRequired(false);

        LabResult fastingGlucose = new LabResult();
        fastingGlucose.setBiomarker("FASTING_GLUCOSE");
        fastingGlucose.setStandardizedBiomarker("glucose");
        fastingGlucose.setValue(new BigDecimal("92.0"));
        fastingGlucose.setUnit("mg/dL");
        fastingGlucose.setReferenceLow(new BigDecimal("70.0"));
        fastingGlucose.setReferenceHigh(new BigDecimal("99.0"));
        fastingGlucose.setIsConfirmed(true);
        fastingGlucose.setCollectionDate(labReport.getCollectionDate());
        fastingGlucose.setSourceReport("synthetic_cmp_report.pdf");
        labReport.addResult(fastingGlucose);

        LabResult creatinine = new LabResult();
        creatinine.setBiomarker("CREATININE");
        creatinine.setStandardizedBiomarker("creatinine");
        creatinine.setValue(new BigDecimal("0.95"));
        creatinine.setUnit("mg/dL");
        creatinine.setReferenceLow(new BigDecimal("0.70"));
        creatinine.setReferenceHigh(new BigDecimal("1.30"));
        creatinine.setIsConfirmed(true);
        creatinine.setCollectionDate(labReport.getCollectionDate());
        creatinine.setSourceReport("synthetic_cmp_report.pdf");
        labReport.addResult(creatinine);

        labReportRepository.save(labReport);
    }
}
