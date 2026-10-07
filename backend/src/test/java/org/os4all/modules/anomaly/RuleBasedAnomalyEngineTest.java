package org.os4all.modules.anomaly;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os4all.modules.anomaly.model.AnomalyEvent;
import org.os4all.modules.anomaly.model.HealthSignal;
import org.os4all.modules.anomaly.model.HealthState;
import org.os4all.modules.anomaly.model.HealthStatusSummary;
import org.os4all.modules.anomaly.service.RuleBasedAnomalyEngine;
import org.os4all.modules.baseline.model.BaselineMetric;
import org.os4all.modules.baseline.service.PersonalBaselineService;
import org.os4all.modules.ingestion.entity.HealthObservation;
import org.os4all.modules.ingestion.entity.ObservationType;
import org.os4all.modules.ingestion.entity.VitalMeasurement;
import org.os4all.modules.ingestion.repository.HealthObservationRepository;
import org.os4all.modules.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RuleBasedAnomalyEngineTest {

    private RuleBasedAnomalyEngine engine;
    private PersonalBaselineService mockBaselineService;
    private HealthObservationRepository mockObservationRepo;
    private UserRepository mockUserRepo;

    @BeforeEach
    void setUp() {
        mockBaselineService = mock(PersonalBaselineService.class);
        mockObservationRepo = mock(HealthObservationRepository.class);
        mockUserRepo = mock(UserRepository.class);
        engine = new RuleBasedAnomalyEngine(mockBaselineService, mockObservationRepo, mockUserRepo);
    }

    private VitalMeasurement createVital(ObservationType type, String vitalName, double value, int daysAgo, double confidence) {
        VitalMeasurement vm = new VitalMeasurement();
        vm.setObservationType(type);
        vm.setVitalName(vitalName);
        vm.setValueNumeric(BigDecimal.valueOf(value));
        vm.setUnit("bpm");
        vm.setTimestamp(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        vm.setConfidence(BigDecimal.valueOf(confidence));
        return vm;
    }

    @Test
    @DisplayName("1. STABLE state: signal within baseline bounds produces STABLE state")
    void testStableState() {
        BaselineMetric baseline = new BaselineMetric();
        baseline.setMetric("resting_heart_rate");
        baseline.setUnit("bpm");
        baseline.setBaselineEstablished(true);
        baseline.setObservationCount(10);
        baseline.setMean(new BigDecimal("60.00"));
        baseline.setRecentAverage(new BigDecimal("61.00"));
        baseline.setDeviationFromBaseline(new BigDecimal("1.00"));
        baseline.setDeviationPercentage(new BigDecimal("1.67"));
        baseline.setStandardDeviation(new BigDecimal("2.00"));
        baseline.setZScore(0.5); // < 1.5 sigma
        baseline.setLastObservedAt(Instant.now().minus(1, ChronoUnit.DAYS));

        List<HealthObservation> history = List.of(
                createVital(ObservationType.HEART_RATE, "Resting Heart Rate", 61.0, 1, 1.0)
        );

        HealthSignal signal = engine.evaluateSingleSignal(baseline, history);

        assertEquals(HealthState.STABLE, signal.getState());
        assertEquals("ELEVATED", signal.getDirection());
        assertTrue(signal.getExplanation().contains("consistent with your personal baseline"));
        assertFalse(signal.getExplanation().contains("disease"));
    }

    @Test
    @DisplayName("2. DRIFT state: persistent deviation over multiple days triggers DRIFT state")
    void testDriftState() {
        BaselineMetric baseline = new BaselineMetric();
        baseline.setMetric("resting_heart_rate");
        baseline.setUnit("bpm");
        baseline.setBaselineEstablished(true);
        baseline.setObservationCount(14);
        baseline.setMean(new BigDecimal("60.00"));
        baseline.setRecentAverage(new BigDecimal("64.00"));
        baseline.setDeviationFromBaseline(new BigDecimal("4.00"));
        baseline.setDeviationPercentage(new BigDecimal("6.67"));
        baseline.setStandardDeviation(new BigDecimal("2.00"));
        baseline.setZScore(2.0); // Between 1.5 and 2.5 sigma
        baseline.setLastObservedAt(Instant.now().minus(1, ChronoUnit.DAYS));

        // 4 consecutive days above mean + 1.0*std (>= 62)
        List<HealthObservation> history = List.of(
                createVital(ObservationType.HEART_RATE, "Resting Heart Rate", 64.0, 4, 1.0),
                createVital(ObservationType.HEART_RATE, "Resting Heart Rate", 63.5, 3, 1.0),
                createVital(ObservationType.HEART_RATE, "Resting Heart Rate", 64.2, 2, 1.0),
                createVital(ObservationType.HEART_RATE, "Resting Heart Rate", 64.0, 1, 1.0)
        );

        HealthSignal signal = engine.evaluateSingleSignal(baseline, history);

        assertEquals(HealthState.DRIFT, signal.getState());
        assertTrue(signal.getPersistenceDays() >= 3);
        assertTrue(signal.getExplanation().contains("drifted"));
        assertTrue(signal.getExplanation().contains("personal baseline"));
    }

    @Test
    @DisplayName("3. ANOMALY state: acute departure (> 2.5 sigma) triggers ANOMALY with structured explanation")
    void testAnomalyState() {
        BaselineMetric baseline = new BaselineMetric();
        baseline.setMetric("resting_heart_rate");
        baseline.setUnit("bpm");
        baseline.setBaselineEstablished(true);
        baseline.setObservationCount(10);
        baseline.setMean(new BigDecimal("60.00"));
        baseline.setRecentAverage(new BigDecimal("78.00"));
        baseline.setDeviationFromBaseline(new BigDecimal("18.00"));
        baseline.setDeviationPercentage(new BigDecimal("30.00"));
        baseline.setStandardDeviation(new BigDecimal("3.00"));
        baseline.setZScore(6.0); // Acute departure > 2.5 sigma
        baseline.setLastObservedAt(Instant.now().minus(1, ChronoUnit.DAYS));

        List<HealthObservation> history = List.of(
                createVital(ObservationType.HEART_RATE, "Resting Heart Rate", 77.0, 2, 0.98),
                createVital(ObservationType.HEART_RATE, "Resting Heart Rate", 79.0, 1, 0.99)
        );

        HealthSignal signal = engine.evaluateSingleSignal(baseline, history);

        assertEquals(HealthState.ANOMALY, signal.getState());
        assertEquals("ELEVATED", signal.getDirection());
        assertTrue(signal.getExplanation().contains("markedly elevated"));
        assertTrue(signal.getExplanation().contains("personal historical average"));
        assertFalse(signal.getExplanation().contains("disease"));
    }

    @Test
    @DisplayName("4. FOLLOW_UP state: single spike or stale reading requires follow-up observation")
    void testFollowUpState() {
        // High z-score but only 1 isolated day (not persistent yet)
        BaselineMetric baseline = new BaselineMetric();
        baseline.setMetric("resting_heart_rate");
        baseline.setUnit("bpm");
        baseline.setBaselineEstablished(true);
        baseline.setObservationCount(10);
        baseline.setMean(new BigDecimal("60.00"));
        baseline.setRecentAverage(new BigDecimal("64.00"));
        baseline.setDeviationFromBaseline(new BigDecimal("4.00"));
        baseline.setStandardDeviation(new BigDecimal("2.2"));
        baseline.setZScore(1.82); // >= 1.5 sigma but persistence < 3 days
        baseline.setLastObservedAt(Instant.now().minus(1, ChronoUnit.DAYS));

        List<HealthObservation> history = List.of(
                createVital(ObservationType.HEART_RATE, "Resting Heart Rate", 64.0, 1, 1.0)
        );

        HealthSignal signal = engine.evaluateSingleSignal(baseline, history);

        assertEquals(HealthState.FOLLOW_UP, signal.getState());
        assertTrue(signal.getExplanation().contains("Follow-up observations needed"));
    }

    @Test
    @DisplayName("5. Multi-Signal Correlation: Sleep ↓ + Resting HR ↑ + HRV ↓ triggers compound anomaly pattern")
    void testMultiSignalCorrelationLayer() {
        UUID userId = UUID.randomUUID();

        // 1. Sleep signal: decreased by -1.5 hours
        HealthSignal sleepSignal = new HealthSignal();
        sleepSignal.setMetric(PersonalBaselineService.METRIC_SLEEP_DURATION);
        sleepSignal.setBaselineMean(new BigDecimal("7.5"));
        sleepSignal.setCurrentValue(new BigDecimal("6.0"));
        sleepSignal.setDeviation(new BigDecimal("-1.5"));
        sleepSignal.setPersistenceDays(3);
        sleepSignal.setDataConfidence(new BigDecimal("0.95"));
        sleepSignal.setState(HealthState.DRIFT);
        sleepSignal.setExplanation("Sleep duration is 1.5 hrs below baseline");

        // 2. Resting HR signal: elevated by +6.0 bpm
        HealthSignal rhrSignal = new HealthSignal();
        rhrSignal.setMetric(PersonalBaselineService.METRIC_RESTING_HEART_RATE);
        rhrSignal.setBaselineMean(new BigDecimal("62.0"));
        rhrSignal.setCurrentValue(new BigDecimal("68.0"));
        rhrSignal.setDeviation(new BigDecimal("6.0"));
        rhrSignal.setPersistenceDays(3);
        rhrSignal.setDataConfidence(new BigDecimal("0.98"));
        rhrSignal.setState(HealthState.DRIFT);
        rhrSignal.setExplanation("Resting HR is 6.0 bpm above baseline");

        // 3. HRV signal: declined by -12.0 ms
        HealthSignal hrvSignal = new HealthSignal();
        hrvSignal.setMetric(PersonalBaselineService.METRIC_HRV);
        hrvSignal.setBaselineMean(new BigDecimal("55.0"));
        hrvSignal.setCurrentValue(new BigDecimal("43.0"));
        hrvSignal.setDeviation(new BigDecimal("-12.0"));
        hrvSignal.setPersistenceDays(3);
        hrvSignal.setDataConfidence(new BigDecimal("0.96"));
        hrvSignal.setState(HealthState.DRIFT);
        hrvSignal.setExplanation("HRV declined by 12.0 ms");

        List<HealthSignal> signals = List.of(sleepSignal, rhrSignal, hrvSignal);

        List<AnomalyEvent> anomalies = engine.detectAnomaliesFromSignals(userId, signals);

        assertNotNull(anomalies);
        assertFalse(anomalies.isEmpty());

        // Verify compound multi-signal anomaly event
        AnomalyEvent multiPattern = anomalies.stream()
                .filter(AnomalyEvent::isMultiSignalCorrelationDetected)
                .findFirst()
                .orElse(null);

        assertNotNull(multiPattern, "Multi-signal correlation event must be created");
        assertEquals(HealthState.ANOMALY, multiPattern.getState());
        assertEquals("Multi-Signal Autonomic Recovery Strain Pattern", multiPattern.getPatternName());
        assertEquals(3, multiPattern.getAffectedSignals().size());
        assertTrue(multiPattern.getExplanation().contains("Sleep duration decreased"));
        assertTrue(multiPattern.getExplanation().contains("Resting Heart Rate elevated"));
        assertTrue(multiPattern.getExplanation().contains("HRV declined"));
        assertTrue(multiPattern.getExplanation().contains("personal historical baseline"));
        // Never claim disease
        assertFalse(multiPattern.getExplanation().toLowerCase().contains("disease"));
    }
}
