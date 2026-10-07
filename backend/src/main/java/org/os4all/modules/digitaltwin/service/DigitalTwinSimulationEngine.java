package org.os4all.modules.digitaltwin.service;

import org.os4all.modules.digitaltwin.dto.SimulationControlResponse;
import org.os4all.modules.digitaltwin.entity.TwinStateType;
import org.os4all.modules.ingestion.entity.HealthObservation;
import org.os4all.modules.ingestion.entity.LifestyleObservation;
import org.os4all.modules.ingestion.entity.ObservationType;
import org.os4all.modules.ingestion.entity.VitalMeasurement;
import org.os4all.modules.ingestion.repository.HealthObservationRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DigitalTwinSimulationEngine {

    private static final Logger log = LoggerFactory.getLogger(DigitalTwinSimulationEngine.class);

    private final UserRepository userRepository;
    private final HealthObservationRepository observationRepository;
    private final DigitalTwinService digitalTwinService;
    private final org.os4all.modules.digitaltwin.telemetry.repository.PatientTelemetryRepository telemetryRepository;

    // Simulation runtime state tracking per user
    private final Map<UUID, SimulationStateHolder> runtimeSimulations = new HashMap<>();

    public record SimulationStateHolder(
            String activeScenario,
            boolean isRunning,
            int tickCount,
            Instant lastTickAt
    ) {}

    public DigitalTwinSimulationEngine(
            UserRepository userRepository,
            HealthObservationRepository observationRepository,
            DigitalTwinService digitalTwinService,
            org.os4all.modules.digitaltwin.telemetry.repository.PatientTelemetryRepository telemetryRepository
    ) {
        this.userRepository = userRepository;
        this.observationRepository = observationRepository;
        this.digitalTwinService = digitalTwinService;
        this.telemetryRepository = telemetryRepository;
    }

    public boolean isSimulating(UUID patientId) {
        SimulationStateHolder state = runtimeSimulations.get(patientId);
        return state != null && state.isRunning();
    }

    public SimulationStateHolder getSimulationState(UUID patientId) {
        return runtimeSimulations.get(patientId);
    }

    /**
     * Starts continuous simulation for patient.
     */
    @Transactional
    public SimulationControlResponse startSimulation(UUID patientId) {
        User u = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        SimulationStateHolder state = new SimulationStateHolder("ACTIVE_STREAM", true, 0, Instant.now());
        runtimeSimulations.put(patientId, state);

        // Generate next reading
        generateNextReading(patientId);

        var twin = digitalTwinService.getDigitalTwinState(patientId);
        return new SimulationControlResponse(
                patientId,
                "ACTIVE_STREAM",
                "RUNNING",
                "Continuous wearable and CGM stream active at 1x real-time rate.",
                twin.state(),
                twin.overallRiskScore(),
                twin.glucoseSpikeProbability(),
                twin.stateDrivers(),
                twin.vitalsSnapshot(),
                Map.of("isRunning", true, "simulationRate", "1x"),
                Instant.now().toString()
        );
    }

    /**
     * Pauses the simulation.
     */
    public SimulationControlResponse pauseSimulation(UUID patientId) {
        SimulationStateHolder current = runtimeSimulations.get(patientId);
        if (current != null) {
            runtimeSimulations.put(patientId, new SimulationStateHolder(current.activeScenario(), false, current.tickCount(), Instant.now()));
        }

        var twin = digitalTwinService.getDigitalTwinState(patientId);
        return new SimulationControlResponse(
                patientId,
                current != null ? current.activeScenario() : "STABLE",
                "PAUSED",
                "Simulation telemetry paused. Physiological state held in steady-state.",
                twin.state(),
                twin.overallRiskScore(),
                twin.glucoseSpikeProbability(),
                twin.stateDrivers(),
                twin.vitalsSnapshot(),
                Map.of("isRunning", false),
                Instant.now().toString()
        );
    }

    /**
     * Resets patient to baseline homeostatic state.
     */
    @Transactional
    public SimulationControlResponse resetSimulation(UUID patientId) {
        return injectScenario(patientId, "STABLE_PATIENT");
    }

    /**
     * Generates a single tick / next reading with subtle physiological noise.
     */
    @Transactional
    public SimulationControlResponse generateNextReading(UUID patientId) {
        User u = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        Instant now = Instant.now();
        SimulationStateHolder current = runtimeSimulations.getOrDefault(
                patientId,
                new SimulationStateHolder("STABLE_PATIENT", true, 1, now)
        );

        int tick = current.tickCount() + 1;
        runtimeSimulations.put(patientId, new SimulationStateHolder(current.activeScenario(), true, tick, now));

        // Generate reading based on active scenario
        double glucoseVal;
        double velocity;
        double hrVal;
        double hrvVal;
        double rhrVal;
        double sleepHrs;
        double sleepQuality;
        int steps;
        String activity;

        String sc = current.activeScenario().toUpperCase();
        if (sc.contains("GLUCOSE") || sc.contains("SPIKE") || sc.contains("RISE")) {
            glucoseVal = 120.0 + Math.min(75.0, tick * 14.0);
            velocity = 2.4;
            hrVal = 76.0 + Math.min(10.0, tick * 2.0);
            hrvVal = Math.max(26.0, 42.0 - (tick * 3.0));
            rhrVal = 75.0;
            sleepHrs = 5.0;
            sleepQuality = 40.0;
            steps = 3800;
            activity = "SEDENTARY";
        } else if (sc.contains("RECOVERY")) {
            glucoseVal = Math.max(90.0, 165.0 - (tick * 15.0));
            velocity = -1.8;
            hrVal = Math.max(60.0, 78.0 - (tick * 3.0));
            hrvVal = Math.min(58.0, 38.0 + (tick * 4.0));
            rhrVal = 61.0;
            sleepHrs = 8.5;
            sleepQuality = 88.0;
            steps = 10500 + (tick * 300);
            activity = "LIGHT";
        } else if (sc.contains("POOR_SLEEP") || sc.contains("SLEEP")) {
            glucoseVal = 112.0 + Math.min(20.0, tick * 3.0);
            velocity = 0.6;
            hrVal = 74.0 + (tick % 3);
            hrvVal = 34.0;
            rhrVal = 75.0;
            sleepHrs = 4.5;
            sleepQuality = 42.0;
            steps = 4200;
            activity = "SEDENTARY";
        } else if (sc.contains("ACTIVITY") || sc.contains("HIGH")) {
            glucoseVal = 86.0 + ((tick % 3) * 1.0);
            velocity = -0.1;
            hrVal = 115.0;
            hrvVal = 62.0;
            rhrVal = 58.0;
            sleepHrs = 8.2;
            sleepQuality = 90.0;
            steps = 14500 + (tick * 400);
            activity = "VIGOROUS";
        } else {
            glucoseVal = 92.0 + ((tick % 3) * 1.0);
            velocity = 0.0;
            hrVal = 62.0 + ((tick % 2) * 1.0);
            hrvVal = 55.0;
            rhrVal = 60.0;
            sleepHrs = 7.8;
            sleepQuality = 86.0;
            steps = 9200;
            activity = "MODERATE";
        }

        saveObservation(u, ObservationType.BLOOD_GLUCOSE, "Continuous CGM", BigDecimal.valueOf(glucoseVal).setScale(1, RoundingMode.HALF_UP), "mg/dL", now);
        saveObservation(u, ObservationType.HEART_RATE, "Continuous Heart Rate", BigDecimal.valueOf(hrVal).setScale(1, RoundingMode.HALF_UP), "bpm", now);
        saveObservation(u, ObservationType.HRV, "Nocturnal HRV", BigDecimal.valueOf(hrvVal).setScale(1, RoundingMode.HALF_UP), "ms", now);

        org.os4all.modules.digitaltwin.telemetry.entity.PatientTelemetry pt = new org.os4all.modules.digitaltwin.telemetry.entity.PatientTelemetry(u, now);
        pt.setGlucose(BigDecimal.valueOf(glucoseVal).setScale(1, RoundingMode.HALF_UP));
        pt.setGlucoseVelocity(BigDecimal.valueOf(velocity).setScale(2, RoundingMode.HALF_UP));
        pt.setHeartRate(BigDecimal.valueOf(hrVal).setScale(1, RoundingMode.HALF_UP));
        pt.setHrv(BigDecimal.valueOf(hrvVal).setScale(1, RoundingMode.HALF_UP));
        pt.setRestingHeartRate(BigDecimal.valueOf(rhrVal).setScale(1, RoundingMode.HALF_UP));
        pt.setSleepDurationHours(BigDecimal.valueOf(sleepHrs).setScale(2, RoundingMode.HALF_UP));
        pt.setSleepQualityScore(BigDecimal.valueOf(sleepQuality).setScale(1, RoundingMode.HALF_UP));
        pt.setSteps(steps);
        pt.setActivityLevel(activity);
        pt.setSource("SIMULATED_CGM_WEARABLE");
        pt.setScenario(current.activeScenario());
        pt.setConfidence(BigDecimal.valueOf(0.980));
        telemetryRepository.save(pt);

        digitalTwinService.updateDigitalTwinState(patientId, current.activeScenario());
        var twin = digitalTwinService.getDigitalTwinState(patientId);

        return new SimulationControlResponse(
                patientId,
                current.activeScenario(),
                "TICK_GENERATED",
                String.format("Generated tick #%d. Simulated glucose: %.1f mg/dL (velocity: %+.2f mg/dL/min), HR: %.1f bpm, HRV: %.1f ms.", tick, glucoseVal, velocity, hrVal, hrvVal),
                twin.state(),
                twin.overallRiskScore(),
                twin.glucoseSpikeProbability(),
                twin.stateDrivers(),
                twin.vitalsSnapshot(),
                Map.of("tick", tick, "glucose", glucoseVal, "heartRate", hrVal, "velocity", velocity),
                now.toString()
        );
    }

    /**
     * Injects one of the 5 required competition scenarios:
     * 1. STABLE PATIENT
     * 2. POOR SLEEP -> METABOLIC DRIFT
     * 3. HIGH ACTIVITY
     * 4. GLUCOSE SPIKE RISK
     * 5. RECOVERY AFTER INTERVENTION
     */
    @Transactional
    public SimulationControlResponse injectScenario(UUID patientId, String rawScenario) {
        User u = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        String scenario = normalizeScenario(rawScenario);
        Instant now = Instant.now();

        // Clear user's recent observations and telemetry to seed deterministic scenario sequence
        observationRepository.deleteByUserId(patientId);
        telemetryRepository.deleteByUserId(patientId);

        switch (scenario) {
            case "POOR_SLEEP" -> seedPoorSleepMetabolicDriftScenario(u, now);
            case "HIGH_ACTIVITY" -> seedHighActivityScenario(u, now);
            case "GLUCOSE_RISE", "GLUCOSE_SPIKE" -> seedGlucoseSpikeRiskScenario(u, now);
            case "RECOVERY" -> seedRecoveryAfterInterventionScenario(u, now);
            default -> seedStablePatientScenario(u, now);
        }

        runtimeSimulations.put(patientId, new SimulationStateHolder(scenario, true, 0, now));
        digitalTwinService.updateDigitalTwinState(patientId, scenario);
        var twin = digitalTwinService.getDigitalTwinState(patientId);

        String description = switch (scenario) {
            case "POOR_SLEEP" -> "Injected Scenario 2: Cumulative Sleep Deprivation → Autonomic Strain & Metabolic Drift. HRV dropped to 33ms, Sleep deficit of 3.1 hrs, Glycemic variability elevating.";
            case "HIGH_ACTIVITY" -> "Injected Scenario 3: High Physical Activity & Aerobic Zone 2 Training. 12,400 daily steps, enhanced insulin sensitivity via GLUT4 muscular uptake.";
            case "GLUCOSE_RISE", "GLUCOSE_SPIKE" -> "Injected Scenario 4: High Glycemic Carb Ingestion with Impaired Autonomic Tone. Postprandial glucose spike imminent (78.5% probability within 2 hours).";
            case "RECOVERY" -> "Injected Scenario 5: Recovery After Lifestyle Intervention. 8.5 hours restorative sleep, 20-min post-meal walk, autonomic tone stabilized.";
            default -> "Injected Scenario 1: Stable Patient Homeostasis. 45 days of consistent personal baseline observations (glucose ~92 mg/dL, HRV ~55 ms, sleep ~7.8 hrs).";
        };

        return new SimulationControlResponse(
                patientId,
                scenario,
                "SCENARIO_INJECTED",
                description,
                twin.state(),
                twin.overallRiskScore(),
                twin.glucoseSpikeProbability(),
                twin.stateDrivers(),
                twin.vitalsSnapshot(),
                Map.of("scenario", scenario, "seedDays", 45),
                now.toString()
        );
    }

    // --- Scenario Seeders ---

    private void seedStablePatientScenario(User u, Instant now) {
        for (int day = 45; day >= 0; day--) {
            Instant ts = now.minus(day, ChronoUnit.DAYS);
            seedBaseVitals(u, ts, 60.0 + ((day % 3) * 0.5), 55.0 + ((day % 4) * 0.5), 7.8, 98.4, 9200, 92.0 + ((day % 3) * 1.5), 36.6);
        }
    }

    private void seedPoorSleepMetabolicDriftScenario(User u, Instant now) {
        // Days 45 down to 8: Stable
        for (int day = 45; day >= 8; day--) {
            Instant ts = now.minus(day, ChronoUnit.DAYS);
            seedBaseVitals(u, ts, 60.0, 55.0, 7.8, 98.4, 9200, 92.0, 36.6);
        }

        // Days 7 down to 0: Poor sleep progressive drift
        double[] sleepHrs = {6.2, 5.8, 5.4, 5.0, 4.8, 4.7, 4.6, 4.5};
        double[] rhrVals  = {63.0, 65.0, 67.5, 70.0, 72.5, 74.5, 75.5, 76.5};
        double[] hrvVals  = {51.0, 48.0, 44.0, 41.0, 37.0, 35.0, 34.0, 33.0};
        double[] glucoseVals = {96.0, 99.0, 103.0, 108.0, 114.0, 118.0, 122.0, 126.0};

        for (int i = 0; i < 8; i++) {
            Instant ts = now.minus(7 - i, ChronoUnit.DAYS);
            seedBaseVitals(u, ts, rhrVals[i], hrvVals[i], sleepHrs[i], 96.5, 5200, glucoseVals[i], 37.1);
        }
    }

    private void seedHighActivityScenario(User u, Instant now) {
        for (int day = 45; day >= 5; day--) {
            Instant ts = now.minus(day, ChronoUnit.DAYS);
            seedBaseVitals(u, ts, 60.0, 55.0, 7.8, 98.4, 9200, 92.0, 36.6);
        }

        for (int day = 4; day >= 0; day--) {
            Instant ts = now.minus(day, ChronoUnit.DAYS);
            seedBaseVitals(u, ts, 58.0, 62.0, 8.2, 99.0, 14500, 85.0, 36.5);
        }
    }

    private void seedGlucoseSpikeRiskScenario(User u, Instant now) {
        // First 40 days stable
        for (int day = 45; day >= 6; day--) {
            Instant ts = now.minus(day, ChronoUnit.DAYS);
            seedBaseVitals(u, ts, 60.0, 55.0, 7.8, 98.4, 9200, 92.0, 36.6);
        }

        // Days 5 down to 1: Accumulated sleep debt
        for (int day = 5; day >= 1; day--) {
            Instant ts = now.minus(day, ChronoUnit.DAYS);
            seedBaseVitals(u, ts, 73.0, 36.0, 5.1, 97.0, 4800, 110.0, 36.9);
        }

        // Today (Day 0): Acute post-meal rapid climb
        Instant tsToday = now.minus(1, ChronoUnit.HOURS);
        seedBaseVitals(u, tsToday, 78.0, 32.0, 4.8, 96.8, 4100, 142.0, 37.0);

        // Current immediate reading: rising at +24 mg/dL
        saveObservation(u, ObservationType.BLOOD_GLUCOSE, "Dexcom CGM Continuous", new BigDecimal("166.0"), "mg/dL", now);

        org.os4all.modules.digitaltwin.telemetry.entity.PatientTelemetry ptAcute = new org.os4all.modules.digitaltwin.telemetry.entity.PatientTelemetry(u, now);
        ptAcute.setGlucose(new BigDecimal("166.0"));
        ptAcute.setGlucoseVelocity(new BigDecimal("2.40"));
        ptAcute.setHeartRate(new BigDecimal("82.0"));
        ptAcute.setHrv(new BigDecimal("30.0"));
        ptAcute.setRestingHeartRate(new BigDecimal("76.0"));
        ptAcute.setSleepDurationHours(new BigDecimal("4.80"));
        ptAcute.setSleepQualityScore(new BigDecimal("40.0"));
        ptAcute.setSteps(4100);
        ptAcute.setActivityLevel("SEDENTARY");
        ptAcute.setSource("SIMULATED_CGM_WEARABLE");
        ptAcute.setScenario("GLUCOSE_RISE");
        ptAcute.setConfidence(BigDecimal.valueOf(0.980));
        telemetryRepository.save(ptAcute);
    }

    private void seedRecoveryAfterInterventionScenario(User u, Instant now) {
        // Historical days had drift
        for (int day = 45; day >= 4; day--) {
            Instant ts = now.minus(day, ChronoUnit.DAYS);
            seedBaseVitals(u, ts, 72.0, 38.0, 5.5, 97.2, 6000, 115.0, 36.8);
        }

        // Days 3 to 0: Interventions applied (sleep recovery, hydration, post-meal walking)
        double[] sleep = {7.5, 8.0, 8.4, 8.5};
        double[] rhr   = {68.0, 64.0, 61.5, 59.5};
        double[] hrv   = {44.0, 49.0, 53.0, 56.0};
        double[] gluc  = {108.0, 98.0, 93.0, 90.0};

        for (int i = 0; i < 4; i++) {
            Instant ts = now.minus(3 - i, ChronoUnit.DAYS);
            seedBaseVitals(u, ts, rhr[i], hrv[i], sleep[i], 98.6, 10200, gluc[i], 36.5);
        }
    }

    private void seedBaseVitals(User u, Instant ts, double rhr, double hrv, double sleepHrs, double spo2, double steps, double glucose, double temp) {
        // RHR
        VitalMeasurement rhrObs = new VitalMeasurement();
        rhrObs.setUser(u);
        rhrObs.setObservationType(ObservationType.HEART_RATE);
        rhrObs.setVitalName("Resting Heart Rate");
        rhrObs.setValueNumeric(BigDecimal.valueOf(rhr).setScale(1, RoundingMode.HALF_UP));
        rhrObs.setUnit("bpm");
        rhrObs.setTimestamp(ts);
        rhrObs.setSource("SYNTHETIC_WEARABLE");
        rhrObs.setConfidence(BigDecimal.valueOf(0.98));
        observationRepository.save(rhrObs);

        // HRV
        VitalMeasurement hrvObs = new VitalMeasurement();
        hrvObs.setUser(u);
        hrvObs.setObservationType(ObservationType.HRV);
        hrvObs.setVitalName("Heart Rate Variability");
        hrvObs.setValueNumeric(BigDecimal.valueOf(hrv).setScale(1, RoundingMode.HALF_UP));
        hrvObs.setUnit("ms");
        hrvObs.setTimestamp(ts);
        hrvObs.setSource("SYNTHETIC_WEARABLE");
        hrvObs.setConfidence(BigDecimal.valueOf(0.97));
        observationRepository.save(hrvObs);

        // Sleep
        LifestyleObservation sleepObs = new LifestyleObservation();
        sleepObs.setUser(u);
        sleepObs.setObservationType(ObservationType.SLEEP);
        sleepObs.setDurationMinutes((int) (sleepHrs * 60));
        sleepObs.setValueNumeric(BigDecimal.valueOf(sleepHrs).setScale(1, RoundingMode.HALF_UP));
        sleepObs.setUnit("hours");
        sleepObs.setTimestamp(ts);
        sleepObs.setSource("SYNTHETIC_TRACKER");
        sleepObs.setConfidence(BigDecimal.valueOf(0.99));
        observationRepository.save(sleepObs);

        // SpO2
        VitalMeasurement spo2Obs = new VitalMeasurement();
        spo2Obs.setUser(u);
        spo2Obs.setObservationType(ObservationType.OXYGEN_SATURATION);
        spo2Obs.setVitalName("Blood Oxygen (SpO2)");
        spo2Obs.setValueNumeric(BigDecimal.valueOf(spo2).setScale(1, RoundingMode.HALF_UP));
        spo2Obs.setUnit("%");
        spo2Obs.setTimestamp(ts);
        spo2Obs.setSource("SYNTHETIC_WEARABLE");
        spo2Obs.setConfidence(BigDecimal.valueOf(0.99));
        observationRepository.save(spo2Obs);

        // Steps
        LifestyleObservation stepsObs = new LifestyleObservation();
        stepsObs.setUser(u);
        stepsObs.setObservationType(ObservationType.STEPS);
        stepsObs.setValueNumeric(BigDecimal.valueOf(steps));
        stepsObs.setUnit("steps");
        stepsObs.setTimestamp(ts);
        stepsObs.setSource("SYNTHETIC_TRACKER");
        stepsObs.setConfidence(BigDecimal.valueOf(0.98));
        observationRepository.save(stepsObs);

        // CGM Glucose
        VitalMeasurement glucoseObs = new VitalMeasurement();
        glucoseObs.setUser(u);
        glucoseObs.setObservationType(ObservationType.BLOOD_GLUCOSE);
        glucoseObs.setVitalName("Continuous Glucose (CGM)");
        glucoseObs.setValueNumeric(BigDecimal.valueOf(glucose).setScale(1, RoundingMode.HALF_UP));
        glucoseObs.setUnit("mg/dL");
        glucoseObs.setTimestamp(ts);
        glucoseObs.setSource("SYNTHETIC_CGM");
        glucoseObs.setConfidence(BigDecimal.valueOf(0.98));
        observationRepository.save(glucoseObs);

        // Temp
        VitalMeasurement tempObs = new VitalMeasurement();
        tempObs.setUser(u);
        tempObs.setObservationType(ObservationType.BODY_TEMPERATURE);
        tempObs.setVitalName("Body Temperature");
        tempObs.setValueNumeric(BigDecimal.valueOf(temp).setScale(2, RoundingMode.HALF_UP));
        tempObs.setUnit("°C");
        tempObs.setTimestamp(ts);
        tempObs.setSource("SYNTHETIC_WEARABLE");
        tempObs.setConfidence(BigDecimal.valueOf(0.99));
        observationRepository.save(tempObs);

        // Also persist corresponding PatientTelemetry packet
        org.os4all.modules.digitaltwin.telemetry.entity.PatientTelemetry pt = new org.os4all.modules.digitaltwin.telemetry.entity.PatientTelemetry(u, ts);
        pt.setGlucose(BigDecimal.valueOf(glucose).setScale(1, RoundingMode.HALF_UP));
        pt.setGlucoseVelocity(BigDecimal.ZERO);
        pt.setHeartRate(BigDecimal.valueOf(rhr + 4.0).setScale(1, RoundingMode.HALF_UP));
        pt.setHrv(BigDecimal.valueOf(hrv).setScale(1, RoundingMode.HALF_UP));
        pt.setRestingHeartRate(BigDecimal.valueOf(rhr).setScale(1, RoundingMode.HALF_UP));
        pt.setSleepDurationHours(BigDecimal.valueOf(sleepHrs).setScale(2, RoundingMode.HALF_UP));
        pt.setSleepQualityScore(BigDecimal.valueOf(sleepHrs >= 7.0 ? 85.0 : 45.0));
        pt.setSteps((int) steps);
        pt.setActivityLevel(steps > 12000 ? "VIGOROUS" : steps > 7000 ? "MODERATE" : "SEDENTARY");
        pt.setSource("SIMULATED_CGM_WEARABLE");
        pt.setScenario("SIMULATED_BASELINE");
        pt.setConfidence(BigDecimal.valueOf(0.980));
        telemetryRepository.save(pt);
    }

    private void saveObservation(User u, ObservationType type, String name, BigDecimal val, String unit, Instant ts) {
        VitalMeasurement obs = new VitalMeasurement();
        obs.setUser(u);
        obs.setObservationType(type);
        obs.setVitalName(name);
        obs.setValueNumeric(val);
        obs.setUnit(unit);
        obs.setTimestamp(ts);
        obs.setSource("SYNTHETIC_LIVE_SENSOR");
        obs.setConfidence(BigDecimal.valueOf(0.99));
        observationRepository.save(obs);
    }

    private String normalizeScenario(String raw) {
        if (raw == null) return "STABLE";
        String s = raw.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        if (s.contains("SLEEP") || s.contains("POOR")) return "POOR_SLEEP";
        if (s.contains("ACTIVITY") || s.contains("HIGH") || s.contains("EXERCISE")) return "HIGH_ACTIVITY";
        if (s.contains("SPIKE") || s.contains("RISE") || s.contains("GLUCOSE")) return "GLUCOSE_RISE";
        if (s.contains("RECOVERY") || s.contains("INTERVENTION")) return "RECOVERY";
        if (s.contains("STABLE")) return "STABLE";
        return "STABLE";
    }

    public org.os4all.modules.digitaltwin.dto.DigitalTwinStateDto getDigitalTwinState(UUID patientId) {
        return digitalTwinService.getDigitalTwinState(patientId);
    }
}
