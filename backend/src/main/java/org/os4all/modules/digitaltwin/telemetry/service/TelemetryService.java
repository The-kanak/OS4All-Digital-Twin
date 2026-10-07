package org.os4all.modules.digitaltwin.telemetry.service;

import org.os4all.core.exception.ResourceNotFoundException;
import org.os4all.modules.baseline.model.BaselineMetric;
import org.os4all.modules.baseline.model.BaselineProfile;
import org.os4all.modules.baseline.service.PersonalBaselineService;
import org.os4all.modules.digitaltwin.dto.DigitalTwinStateDto;
import org.os4all.modules.digitaltwin.dto.SimulationControlResponse;
import org.os4all.modules.digitaltwin.service.DigitalTwinService;
import org.os4all.modules.digitaltwin.service.DigitalTwinSimulationEngine;
import org.os4all.modules.digitaltwin.telemetry.dto.*;
import org.os4all.modules.digitaltwin.telemetry.entity.PatientTelemetry;
import org.os4all.modules.digitaltwin.telemetry.repository.PatientTelemetryRepository;
import org.os4all.modules.ingestion.entity.LifestyleObservation;
import org.os4all.modules.ingestion.entity.ObservationType;
import org.os4all.modules.ingestion.entity.VitalMeasurement;
import org.os4all.modules.ingestion.repository.HealthObservationRepository;
import org.os4all.modules.user.entity.User;
import org.os4all.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final UserRepository userRepository;
    private final PatientTelemetryRepository telemetryRepository;
    private final HealthObservationRepository observationRepository;
    private final DigitalTwinService digitalTwinService;
    private final DigitalTwinSimulationEngine simulationEngine;
    private final PersonalBaselineService baselineService;

    public TelemetryService(
            UserRepository userRepository,
            PatientTelemetryRepository telemetryRepository,
            HealthObservationRepository observationRepository,
            DigitalTwinService digitalTwinService,
            DigitalTwinSimulationEngine simulationEngine,
            PersonalBaselineService baselineService
    ) {
        this.userRepository = userRepository;
        this.telemetryRepository = telemetryRepository;
        this.observationRepository = observationRepository;
        this.digitalTwinService = digitalTwinService;
        this.simulationEngine = simulationEngine;
        this.baselineService = baselineService;
    }

    /**
     * Ingests a new 5-minute telemetry packet, validates bounds, computes velocity,
     * mirrors to observations, and updates living Digital Twin state.
     */
    @Transactional
    public TelemetryPacketDto ingestTelemetry(TelemetryIngestRequest req) {
        if (req.patientId() == null) {
            throw new IllegalArgumentException("patientId must not be null");
        }

        User user = userRepository.findById(req.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + req.patientId()));

        validateTelemetryBounds(req);

        Instant ts = req.timestamp() != null ? req.timestamp() : Instant.now();

        // Calculate velocity if not explicitly provided
        BigDecimal velocity = req.glucoseVelocity();
        if (velocity == null && req.glucose() != null) {
            Optional<PatientTelemetry> prevOpt = telemetryRepository.findTopByUserIdOrderByTimestampDesc(user.getId());
            if (prevOpt.isPresent() && prevOpt.get().getGlucose() != null) {
                PatientTelemetry prev = prevOpt.get();
                long mins = Math.max(1, Duration.between(prev.getTimestamp(), ts).toMinutes());
                BigDecimal diff = req.glucose().subtract(prev.getGlucose());
                velocity = diff.divide(BigDecimal.valueOf(mins), 2, RoundingMode.HALF_UP);
            } else {
                velocity = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
        }

        PatientTelemetry pt = new PatientTelemetry(user, ts);
        pt.setGlucose(req.glucose());
        pt.setGlucoseVelocity(velocity);
        pt.setHeartRate(req.heartRate());
        pt.setHrv(req.hrv());
        pt.setRestingHeartRate(req.restingHeartRate());
        pt.setSleepDurationHours(req.sleepDurationHours());
        pt.setSleepQualityScore(req.sleepQualityScore());
        pt.setSteps(req.steps());
        pt.setActivityLevel(req.activityLevel() != null ? req.activityLevel().toUpperCase() : "MODERATE");
        pt.setSource(req.source() != null ? req.source() : "SIMULATED_CGM_WEARABLE");
        pt.setScenario(req.scenario() != null ? req.scenario() : "LIVE_TELEMETRY");
        pt.setConfidence(BigDecimal.valueOf(0.980));

        pt = telemetryRepository.save(pt);

        // Mirror to HealthObservation table to keep baselines and historical timelines synchronous
        mirrorToObservations(user, pt);

        // Trigger Digital Twin state update with fused static EHR + dynamic telemetry
        try {
            digitalTwinService.updateDigitalTwinState(user.getId(), pt.getScenario());
        } catch (Exception e) {
            log.warn("Error recomputing digital twin state for patient {}: {}", user.getId(), e.getMessage());
        }

        return toDto(pt);
    }

    /**
     * Retrieves the single most recent telemetry packet for a patient.
     */
    @Transactional(readOnly = true)
    public TelemetryPacketDto getLatestTelemetry(UUID patientId) {
        User user = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));

        return telemetryRepository.findTopByUserIdOrderByTimestampDesc(patientId)
                .map(this::toDto)
                .orElseGet(() -> buildDefaultSnapshotTelemetry(user));
    }

    /**
     * Retrieves chronological telemetry history for a patient with strict patient isolation.
     */
    @Transactional(readOnly = true)
    public List<TelemetryPacketDto> getTelemetryHistory(UUID patientId, int limit, Instant since) {
        if (!userRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found: " + patientId);
        }

        int safeLimit = Math.min(Math.max(1, limit), 500);

        List<PatientTelemetry> list;
        if (since != null) {
            list = telemetryRepository.findRecentPoints(patientId, since);
            if (list.size() > safeLimit) {
                list = list.subList(list.size() - safeLimit, list.size());
            }
        } else {
            list = telemetryRepository.findByUserIdOrderByTimestampDesc(patientId, PageRequest.of(0, safeLimit));
            Collections.reverse(list); // Chronological ascending
        }

        return list.stream().map(this::toDto).toList();
    }

    /**
     * Returns structured stream metrics for chart rendering and trend analysis.
     */
    @Transactional(readOnly = true)
    public TelemetryStreamDto getTelemetryStream(UUID patientId, String rawMetric, int days, int limit) {
        User user = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));

        String metric = rawMetric != null ? rawMetric.trim().toLowerCase() : "glucose";
        Instant since = Instant.now().minus(Math.max(1, days), ChronoUnit.DAYS);

        List<PatientTelemetry> history = telemetryRepository.findRecentPoints(patientId, since);
        if (history.isEmpty()) {
            history = telemetryRepository.findByUserIdOrderByTimestampDesc(patientId, PageRequest.of(0, Math.max(1, limit)));
            Collections.reverse(history);
        }

        List<TelemetryStreamDto.TelemetryPointDto> points = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal min = null;
        BigDecimal max = null;
        int count = 0;

        for (PatientTelemetry pt : history) {
            BigDecimal val = extractMetricValue(pt, metric);
            if (val != null) {
                points.add(new TelemetryStreamDto.TelemetryPointDto(
                        val,
                        pt.getGlucoseVelocity(),
                        pt.getHeartRate(),
                        pt.getHrv(),
                        pt.getRestingHeartRate(),
                        pt.getSleepDurationHours(),
                        pt.getSteps(),
                        pt.getActivityLevel(),
                        pt.getTimestamp().toString(),
                        pt.getSource(),
                        pt.getConfidence()
                ));
                sum = sum.add(val);
                if (min == null || val.compareTo(min) < 0) min = val;
                if (max == null || val.compareTo(max) > 0) max = val;
                count++;
            }
        }

        BigDecimal mean = count > 0 ? sum.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP) : new BigDecimal("95.0");

        // Baseline profile lookup
        try {
            BaselineProfile profile = baselineService.calculateBaselineProfile(patientId);
            BaselineMetric bm = profile.getMetrics().stream()
                    .filter(m -> metric.equalsIgnoreCase(m.getMetric()))
                    .findFirst()
                    .orElse(null);
            if (bm != null && bm.getMean() != null) {
                mean = bm.getMean();
                if (bm.getMin() != null) min = bm.getMin();
                if (bm.getMax() != null) max = bm.getMax();
            }
        } catch (Exception ignored) {}

        TelemetryPacketDto latest = telemetryRepository.findTopByUserIdOrderByTimestampDesc(patientId)
                .map(this::toDto)
                .orElseGet(() -> buildDefaultSnapshotTelemetry(user));

        String unit = switch (metric) {
            case "heart_rate", "resting_heart_rate" -> "bpm";
            case "hrv" -> "ms";
            case "sleep", "sleep_duration" -> "hours";
            case "steps" -> "steps";
            default -> "mg/dL";
        };

        boolean isSimulating = simulationEngine.isSimulating(patientId);
        String scenario = latest != null ? latest.scenario() : "STABLE";

        return new TelemetryStreamDto(
                patientId,
                user.getFullName(),
                metric,
                unit,
                points,
                mean,
                min != null ? min : mean.multiply(new BigDecimal("0.85")).setScale(1, RoundingMode.HALF_UP),
                max != null ? max : mean.multiply(new BigDecimal("1.25")).setScale(1, RoundingMode.HALF_UP),
                latest,
                "Next 2 Hours",
                isSimulating,
                scenario,
                TelemetryPacketDto.DEFAULT_DISCLAIMER
        );
    }

    /**
     * Starts continuous dynamic simulation stream for the patient.
     */
    @Transactional
    public SimulationStatusDto startSimulation(UUID patientId, String scenario) {
        if (!userRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found: " + patientId);
        }

        if (scenario != null && !scenario.isBlank()) {
            simulationEngine.injectScenario(patientId, scenario);
        }
        SimulationControlResponse resp = simulationEngine.startSimulation(patientId);

        return buildSimulationStatus(patientId, resp, "Simulation stream active.");
    }

    /**
     * Stops / pauses dynamic simulation stream.
     */
    @Transactional
    public SimulationStatusDto stopSimulation(UUID patientId) {
        if (!userRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found: " + patientId);
        }

        SimulationControlResponse resp = simulationEngine.pauseSimulation(patientId);
        return buildSimulationStatus(patientId, resp, "Simulation stream paused.");
    }

    /**
     * Injects a deterministic simulation scenario (STABLE, POOR_SLEEP, HIGH_ACTIVITY, GLUCOSE_RISE, RECOVERY).
     */
    @Transactional
    public SimulationStatusDto injectScenario(UUID patientId, String scenario) {
        if (!userRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found: " + patientId);
        }

        SimulationControlResponse resp = simulationEngine.injectScenario(patientId, scenario);
        return buildSimulationStatus(patientId, resp, "Injected scenario: " + scenario);
    }

    /**
     * Returns live simulation status and telemetry snapshot for patient.
     */
    @Transactional(readOnly = true)
    public SimulationStatusDto getSimulationStatus(UUID patientId) {
        User user = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));

        var holder = simulationEngine.getSimulationState(patientId);
        TelemetryPacketDto latest = getLatestTelemetry(patientId);
        DigitalTwinStateDto twin = digitalTwinService.getDigitalTwinState(patientId);

        boolean running = holder != null && holder.isRunning();
        String activeScen = holder != null ? holder.activeScenario() : "STABLE";
        int ticks = holder != null ? holder.tickCount() : 0;
        Instant lastTick = holder != null ? holder.lastTickAt() : Instant.now();

        return new SimulationStatusDto(
                patientId,
                user.getFullName(),
                running,
                activeScen,
                ticks,
                lastTick,
                latest,
                twin.state(),
                twin.overallRiskScore(),
                twin.glucoseSpikeProbability(),
                twin.stateDrivers(),
                running ? "Simulation running" : "Simulation idle",
                SimulationStatusDto.DEFAULT_DISCLAIMER
        );
    }

    // --- Helpers ---

    private void validateTelemetryBounds(TelemetryIngestRequest req) {
        if (req.glucose() != null) {
            double g = req.glucose().doubleValue();
            if (g < 20.0 || g > 600.0) {
                throw new IllegalArgumentException("Glucose must be between 20.0 and 600.0 mg/dL (received: " + g + ")");
            }
        }
        if (req.heartRate() != null) {
            double hr = req.heartRate().doubleValue();
            if (hr < 30.0 || hr > 250.0) {
                throw new IllegalArgumentException("Heart rate must be between 30.0 and 250.0 bpm (received: " + hr + ")");
            }
        }
        if (req.hrv() != null) {
            double hrv = req.hrv().doubleValue();
            if (hrv < 0.0 || hrv > 300.0) {
                throw new IllegalArgumentException("HRV must be between 0.0 and 300.0 ms (received: " + hrv + ")");
            }
        }
        if (req.restingHeartRate() != null) {
            double rhr = req.restingHeartRate().doubleValue();
            if (rhr < 30.0 || rhr > 200.0) {
                throw new IllegalArgumentException("Resting heart rate must be between 30.0 and 200.0 bpm (received: " + rhr + ")");
            }
        }
        if (req.sleepDurationHours() != null) {
            double sl = req.sleepDurationHours().doubleValue();
            if (sl < 0.0 || sl > 24.0) {
                throw new IllegalArgumentException("Sleep duration must be between 0.0 and 24.0 hours (received: " + sl + ")");
            }
        }
        if (req.sleepQualityScore() != null) {
            double sq = req.sleepQualityScore().doubleValue();
            if (sq < 0.0 || sq > 100.0) {
                throw new IllegalArgumentException("Sleep quality score must be between 0.0 and 100.0 (received: " + sq + ")");
            }
        }
        if (req.steps() != null) {
            int st = req.steps();
            if (st < 0 || st > 100000) {
                throw new IllegalArgumentException("Steps must be between 0 and 100,000 (received: " + st + ")");
            }
        }
    }

    private void mirrorToObservations(User user, PatientTelemetry pt) {
        Instant ts = pt.getTimestamp();
        String src = pt.getSource();

        if (pt.getGlucose() != null) {
            saveVital(user, ObservationType.BLOOD_GLUCOSE, "Continuous CGM", pt.getGlucose(), "mg/dL", ts, src);
        }
        if (pt.getHeartRate() != null) {
            saveVital(user, ObservationType.HEART_RATE, "Continuous Heart Rate", pt.getHeartRate(), "bpm", ts, src);
        }
        if (pt.getHrv() != null) {
            saveVital(user, ObservationType.HRV, "Heart Rate Variability", pt.getHrv(), "ms", ts, src);
        }
        if (pt.getSleepDurationHours() != null) {
            LifestyleObservation sleepObs = new LifestyleObservation();
            sleepObs.setUser(user);
            sleepObs.setObservationType(ObservationType.SLEEP);
            sleepObs.setValueNumeric(pt.getSleepDurationHours());
            sleepObs.setDurationMinutes((int) (pt.getSleepDurationHours().doubleValue() * 60));
            sleepObs.setUnit("hours");
            sleepObs.setTimestamp(ts);
            sleepObs.setSource(src);
            sleepObs.setConfidence(BigDecimal.valueOf(0.980));
            observationRepository.save(sleepObs);
        }
        if (pt.getSteps() != null) {
            saveVital(user, ObservationType.STEPS, "Step Counter", BigDecimal.valueOf(pt.getSteps()), "steps", ts, src);
        }
    }

    private void saveVital(User user, ObservationType type, String name, BigDecimal val, String unit, Instant ts, String src) {
        VitalMeasurement vm = new VitalMeasurement();
        vm.setUser(user);
        vm.setObservationType(type);
        vm.setVitalName(name);
        vm.setValueNumeric(val);
        vm.setUnit(unit);
        vm.setStandardValueNumeric(val);
        vm.setStandardUnit(unit);
        vm.setTimestamp(ts);
        vm.setSource(src);
        vm.setConfidence(BigDecimal.valueOf(0.980));
        observationRepository.save(vm);
    }

    private BigDecimal extractMetricValue(PatientTelemetry pt, String metric) {
        return switch (metric) {
            case "heart_rate" -> pt.getHeartRate();
            case "resting_heart_rate" -> pt.getRestingHeartRate();
            case "hrv" -> pt.getHrv();
            case "sleep", "sleep_duration" -> pt.getSleepDurationHours();
            case "steps" -> pt.getSteps() != null ? BigDecimal.valueOf(pt.getSteps()) : null;
            default -> pt.getGlucose();
        };
    }

    private TelemetryPacketDto buildDefaultSnapshotTelemetry(User user) {
        return new TelemetryPacketDto(
                UUID.randomUUID(),
                user.getId(),
                user.getFullName(),
                Instant.now(),
                new BigDecimal("95.0"),
                BigDecimal.ZERO,
                new BigDecimal("68.0"),
                new BigDecimal("52.0"),
                new BigDecimal("62.0"),
                new BigDecimal("7.5"),
                new BigDecimal("85.0"),
                8200,
                "MODERATE",
                "DEFAULT_SNAPSHOT",
                "STABLE",
                BigDecimal.valueOf(0.950),
                TelemetryPacketDto.DEFAULT_DISCLAIMER
        );
    }

    private SimulationStatusDto buildSimulationStatus(UUID patientId, SimulationControlResponse resp, String msg) {
        User u = userRepository.findById(patientId).orElse(null);
        TelemetryPacketDto latest = getLatestTelemetry(patientId);

        return new SimulationStatusDto(
                patientId,
                u != null ? u.getFullName() : "Synthetic Patient",
                "RUNNING".equalsIgnoreCase(resp.status()),
                resp.scenario(),
                0,
                Instant.now(),
                latest,
                resp.currentTwinState(),
                resp.overallRiskScore(),
                resp.glucoseSpikeProbability(),
                resp.immediateDrivers(),
                msg,
                SimulationStatusDto.DEFAULT_DISCLAIMER
        );
    }

    private TelemetryPacketDto toDto(PatientTelemetry pt) {
        return new TelemetryPacketDto(
                pt.getId(),
                pt.getUser().getId(),
                pt.getUser().getFullName(),
                pt.getTimestamp(),
                pt.getGlucose(),
                pt.getGlucoseVelocity(),
                pt.getHeartRate(),
                pt.getHrv(),
                pt.getRestingHeartRate(),
                pt.getSleepDurationHours(),
                pt.getSleepQualityScore(),
                pt.getSteps(),
                pt.getActivityLevel(),
                pt.getSource(),
                pt.getScenario(),
                pt.getConfidence(),
                TelemetryPacketDto.DEFAULT_DISCLAIMER
        );
    }
}
