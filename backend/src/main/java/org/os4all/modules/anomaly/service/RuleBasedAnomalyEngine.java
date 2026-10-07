package org.os4all.modules.anomaly.service;

import org.os4all.core.exception.ResourceNotFoundException;
import org.os4all.modules.anomaly.model.AnomalyEvent;
import org.os4all.modules.anomaly.model.HealthSignal;
import org.os4all.modules.anomaly.model.HealthState;
import org.os4all.modules.anomaly.model.HealthStatusSummary;
import org.os4all.modules.baseline.model.BaselineMetric;
import org.os4all.modules.baseline.model.BaselineProfile;
import org.os4all.modules.baseline.service.PersonalBaselineService;
import org.os4all.modules.ingestion.entity.HealthObservation;
import org.os4all.modules.ingestion.repository.HealthObservationRepository;
import org.os4all.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Transparent, deterministic rule-based Trend and Anomaly Engine.
 * Evaluates:
 * 1. Deviation from personal baseline
 * 2. Persistence across observation days
 * 3. Magnitude (sigma departure / percentage)
 * 4. Number of affected signals
 * 5. Data confidence
 * 6. Recency
 *
 * Multi-Signal Correlation Layer:
 * Detects synergistic patterns (e.g., Sleep ↓ + Resting HR ↑ + HRV ↓)
 * without hardcoding or diagnosing diseases.
 */
@Service
public class RuleBasedAnomalyEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleBasedAnomalyEngine.class);

    private final PersonalBaselineService baselineService;
    private final HealthObservationRepository observationRepository;
    private final UserRepository userRepository;

    public RuleBasedAnomalyEngine(
            PersonalBaselineService baselineService,
            HealthObservationRepository observationRepository,
            UserRepository userRepository
    ) {
        this.baselineService = baselineService;
        this.observationRepository = observationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Evaluates all health signals for a given user against personal baseline.
     */
    @Transactional(readOnly = true)
    public List<HealthSignal> evaluateHealthSignals(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        BaselineProfile profile = baselineService.calculateBaselineProfile(userId);
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        List<HealthObservation> userHistory = observationRepository.findObservationsForBaseline(userId, thirtyDaysAgo);

        List<HealthSignal> evaluatedSignals = new ArrayList<>();
        for (BaselineMetric metric : profile.getMetrics()) {
            HealthSignal signal = evaluateSingleSignal(metric, userHistory);
            evaluatedSignals.add(signal);
        }

        return evaluatedSignals;
    }

    /**
     * Evaluates active anomaly events, including single-signal departures and multi-signal correlations.
     */
    @Transactional(readOnly = true)
    public List<AnomalyEvent> detectAnomalies(UUID userId) {
        List<HealthSignal> signals = evaluateHealthSignals(userId);
        return detectAnomaliesFromSignals(userId, signals);
    }

    /**
     * Generates comprehensive system status summary (GET /api/v1/health/status).
     */
    @Transactional(readOnly = true)
    public HealthStatusSummary evaluateStatus(UUID userId) {
        List<HealthSignal> signals = evaluateHealthSignals(userId);
        List<AnomalyEvent> anomalies = detectAnomaliesFromSignals(userId, signals);

        HealthStatusSummary summary = new HealthStatusSummary(userId);
        summary.setSignals(signals);
        summary.setActiveSignalsCount(signals.size());
        summary.setActiveAnomalies(anomalies);
        summary.setActiveAnomaliesCount(anomalies.size());

        long deviatingCount = signals.stream()
                .filter(s -> s.getState() != HealthState.STABLE)
                .count();
        summary.setDeviatingSignalsCount((int) deviatingCount);

        // Check for multi-signal patterns
        Optional<AnomalyEvent> multiPattern = anomalies.stream()
                .filter(AnomalyEvent::isMultiSignalCorrelationDetected)
                .findFirst();

        if (multiPattern.isPresent()) {
            summary.setMultiSignalPatternActive(true);
            summary.setPatternSummary(multiPattern.get().getPatternName() + ": " + multiPattern.get().getExplanation());
        } else {
            summary.setMultiSignalPatternActive(false);
            summary.setPatternSummary("No multi-signal systemic patterns detected.");
        }

        // Determine aggregate status
        HealthState aggregateState = HealthState.STABLE;
        if (anomalies.stream().anyMatch(a -> a.getState() == HealthState.ANOMALY)) {
            aggregateState = HealthState.ANOMALY;
            summary.setStatusHeadline("Acute departure detected from personal baseline across biometric signals.");
        } else if (anomalies.stream().anyMatch(a -> a.getState() == HealthState.FOLLOW_UP)) {
            aggregateState = HealthState.FOLLOW_UP;
            summary.setStatusHeadline("Follow-up verification needed for recent signal fluctuations.");
        } else if (anomalies.stream().anyMatch(a -> a.getState() == HealthState.DRIFT)) {
            aggregateState = HealthState.DRIFT;
            summary.setStatusHeadline("Persistent directional drift observed relative to personal historical average.");
        } else {
            summary.setStatusHeadline("All monitored biometric signals are stable within your personal baseline.");
        }

        summary.setAggregateState(aggregateState);
        return summary;
    }

    /**
     * Deterministic rule-based evaluation of an individual signal.
     */
    public HealthSignal evaluateSingleSignal(BaselineMetric baseline, List<HealthObservation> history) {
        HealthSignal signal = new HealthSignal();
        signal.setMetric(baseline.getMetric());
        signal.setUnit(baseline.getUnit());
        signal.setBaselineMean(baseline.getMean());
        signal.setCurrentValue(baseline.getRecentAverage() != null ? baseline.getRecentAverage() : baseline.getMean());
        signal.setDeviation(baseline.getDeviationFromBaseline());
        signal.setDeviationPercentage(baseline.getDeviationPercentage());
        signal.setZScore(baseline.getZScore());
        signal.setLastObservedAt(baseline.getLastObservedAt());

        // Direction
        if (baseline.getDeviationFromBaseline() != null) {
            if (baseline.getDeviationFromBaseline().doubleValue() > 0.001) {
                signal.setDirection("ELEVATED");
            } else if (baseline.getDeviationFromBaseline().doubleValue() < -0.001) {
                signal.setDirection("DEPRESSED");
            } else {
                signal.setDirection("STABLE");
            }
        } else {
            signal.setDirection("STABLE");
        }

        // Extract relevant observations for this metric from history to measure persistence and confidence
        List<HealthObservation> metricObs = filterObservationsForMetric(baseline.getMetric(), history);
        int persistenceDays = calculatePersistenceDays(metricObs, baseline.getMean(), baseline.getStandardDeviation());
        int consecutiveDeviations = calculateConsecutiveDeviations(metricObs, baseline.getMean(), baseline.getStandardDeviation());
        BigDecimal avgConfidence = calculateAverageConfidence(metricObs);

        signal.setPersistenceDays(persistenceDays);
        signal.setConsecutiveDeviatingReadings(consecutiveDeviations);
        signal.setDataConfidence(avgConfidence);

        // State Machine determination for single signal
        HealthState state;
        String explanation;

        if (!baseline.isBaselineEstablished()) {
            state = HealthState.STABLE;
            explanation = String.format("Awaiting sufficient historical data for %s (%d/%d observations).",
                    formatMetricLabel(baseline.getMetric()), baseline.getObservationCount(), baseline.getRequiredObservations());
        } else {
            Double z = baseline.getZScore();
            double absZ = z != null ? Math.abs(z) : 0.0;
            double absDevPct = baseline.getDeviationPercentage() != null ? Math.abs(baseline.getDeviationPercentage().doubleValue()) : 0.0;

            // Recency check: if last observation is older than 14 days, flag for follow-up verification
            boolean isStale = isObservationStale(baseline.getLastObservedAt(), 14);

            if (isStale) {
                state = HealthState.FOLLOW_UP;
                explanation = String.format("Last recorded %s observation was over 14 days ago. Updated log recommended for continuous baseline verification.",
                        formatMetricLabel(baseline.getMetric()));
            } else if (absZ >= 2.5 || (absDevPct >= 20.0 && consecutiveDeviations >= 2)) {
                // ANOMALY: Marked or acute departure from personal baseline
                state = HealthState.ANOMALY;
                explanation = String.format("%s is markedly %s (%s%.2f %s, %s%.2fσ) relative to your personal historical average for %d consecutive reading(s).",
                        formatMetricLabel(baseline.getMetric()),
                        signal.getDirection().toLowerCase(Locale.ROOT),
                        signal.getDeviation().doubleValue() >= 0 ? "+" : "",
                        signal.getDeviation().doubleValue(),
                        signal.getUnit(),
                        z != null && z >= 0 ? "+" : "",
                        z != null ? z : 0.0,
                        consecutiveDeviations
                );
            } else if ((absZ >= 1.5 || absDevPct >= 10.0) && persistenceDays >= 3) {
                // DRIFT: Persistent directional departure over multiple days
                state = HealthState.DRIFT;
                explanation = String.format("%s has drifted %s from your personal baseline for %d days (recent deviation: %s%.2f %s).",
                        formatMetricLabel(baseline.getMetric()),
                        signal.getDirection().toLowerCase(Locale.ROOT),
                        persistenceDays,
                        signal.getDeviation().doubleValue() >= 0 ? "+" : "",
                        signal.getDeviation().doubleValue(),
                        signal.getUnit()
                );
            } else if (absZ >= 1.5 && persistenceDays < 3) {
                // Emerging shift or single spike: mark for follow-up observation before declaring drift
                state = HealthState.FOLLOW_UP;
                explanation = String.format("%s showed an initial departure of %s%.2f %s. Follow-up observations needed to confirm if this represents a persistent trend.",
                        formatMetricLabel(baseline.getMetric()),
                        signal.getDeviation().doubleValue() >= 0 ? "+" : "",
                        signal.getDeviation().doubleValue(),
                        signal.getUnit()
                );
            } else {
                // STABLE
                state = HealthState.STABLE;
                explanation = String.format("%s is consistent with your personal baseline (deviation of %s%.2f %s).",
                        formatMetricLabel(baseline.getMetric()),
                        signal.getDeviation() != null && signal.getDeviation().doubleValue() >= 0 ? "+" : "",
                        signal.getDeviation() != null ? signal.getDeviation().doubleValue() : 0.0,
                        signal.getUnit()
                );
            }
        }

        signal.setState(state);
        signal.setExplanation(explanation);
        return signal;
    }

    /**
     * Multi-Signal Correlation Layer:
     * Cross-evaluates synergistic biological departures.
     * Example: Sleep ↓ + Resting HR ↑ + HRV ↓ persistent for multiple days.
     */
    public List<AnomalyEvent> detectAnomaliesFromSignals(UUID userId, List<HealthSignal> signals) {
        List<AnomalyEvent> anomalies = new ArrayList<>();
        Map<String, HealthSignal> signalMap = new HashMap<>();
        for (HealthSignal s : signals) {
            signalMap.put(s.getMetric(), s);
        }

        // 1. Check Multi-Signal Synergistic Patterns
        // Pattern A: Autonomic Recovery Strain (Sleep ↓ + Resting HR ↑ + HRV ↓)
        HealthSignal sleep = signalMap.get(PersonalBaselineService.METRIC_SLEEP_DURATION);
        HealthSignal rhr = signalMap.get(PersonalBaselineService.METRIC_RESTING_HEART_RATE);
        HealthSignal hrv = signalMap.get(PersonalBaselineService.METRIC_HRV);

        if (isAutonomicStrainPattern(sleep, rhr, hrv)) {
            AnomalyEvent multiEvent = new AnomalyEvent();
            multiEvent.setUserId(userId);
            multiEvent.setState(HealthState.ANOMALY);
            multiEvent.setPrimaryMetric("Multi-Signal: Sleep / RHR / HRV");
            multiEvent.setAffectedSignals(List.of(
                    PersonalBaselineService.METRIC_SLEEP_DURATION,
                    PersonalBaselineService.METRIC_RESTING_HEART_RATE,
                    PersonalBaselineService.METRIC_HRV
            ));
            multiEvent.setMultiSignalCorrelationDetected(true);
            multiEvent.setPatternName("Multi-Signal Autonomic Recovery Strain Pattern");

            int maxDuration = Math.max(sleep.getPersistenceDays(), Math.max(rhr.getPersistenceDays(), hrv.getPersistenceDays()));
            multiEvent.setDurationDays(Math.max(maxDuration, 2));

            BigDecimal avgConfidence = sleep.getDataConfidence()
                    .add(rhr.getDataConfidence())
                    .add(hrv.getDataConfidence())
                    .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            multiEvent.setConfidence(avgConfidence);

            multiEvent.setBaselineComparison(String.format(
                    "Sleep (%s%.1f hrs from baseline), Resting HR (+%.1f bpm from baseline), HRV (-%.1f ms from baseline)",
                    sleep.getDeviation().doubleValue() >= 0 ? "+" : "", sleep.getDeviation().doubleValue(),
                    rhr.getDeviation().doubleValue(),
                    Math.abs(hrv.getDeviation().doubleValue())
            ));

            multiEvent.setReason("Coordinated departure across 3 autonomic recovery signals detected over multiple consecutive days.");
            multiEvent.setExplanation(String.format(
                    "Multi-signal correlation detected: Sleep duration decreased (-%.1f hrs), Resting Heart Rate elevated (+%.1f bpm), and HRV declined (-%.1f ms) simultaneously for %d days. This compounded pattern indicates cumulative physiological stress or recovery debt relative to your personal historical baseline.",
                    Math.abs(sleep.getDeviation().doubleValue()),
                    rhr.getDeviation().doubleValue(),
                    Math.abs(hrv.getDeviation().doubleValue()),
                    multiEvent.getDurationDays()
            ));

            anomalies.add(multiEvent);
        }

        // Pattern B: Respiratory / Temperature Strain (Temp ↑ + SpO2 ↓)
        HealthSignal temp = signalMap.get(PersonalBaselineService.METRIC_TEMPERATURE);
        HealthSignal spo2 = signalMap.get(PersonalBaselineService.METRIC_SPO2);
        if (isRespiratoryStrainPattern(temp, spo2)) {
            AnomalyEvent respEvent = new AnomalyEvent();
            respEvent.setUserId(userId);
            respEvent.setState(HealthState.ANOMALY);
            respEvent.setPrimaryMetric("Multi-Signal: Temperature / SpO2");
            respEvent.setAffectedSignals(List.of(PersonalBaselineService.METRIC_TEMPERATURE, PersonalBaselineService.METRIC_SPO2));
            respEvent.setMultiSignalCorrelationDetected(true);
            respEvent.setPatternName("Elevated Temperature & SpO2 Variance Pattern");
            respEvent.setDurationDays(Math.max(temp.getPersistenceDays(), spo2.getPersistenceDays()));
            respEvent.setConfidence(temp.getDataConfidence().add(spo2.getDataConfidence()).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP));
            respEvent.setBaselineComparison(String.format("Temperature (+%.2f °C from baseline), SpO2 (-%.1f%% from baseline)",
                    temp.getDeviation().doubleValue(), Math.abs(spo2.getDeviation().doubleValue())));
            respEvent.setReason("Synchronized shift in thermal regulation and blood oxygen saturation.");
            respEvent.setExplanation(String.format(
                    "Temperature elevated (+%.2f °C) alongside depressed oxygen saturation (-%.1f%%) relative to your historical baseline for %d days. This multi-signal variance warrants close observation and medical provider consultation if persistent.",
                    temp.getDeviation().doubleValue(), Math.abs(spo2.getDeviation().doubleValue()), respEvent.getDurationDays()));
            anomalies.add(respEvent);
        }

        // 2. Add Individual Signal Departures (when not subsumed by multi-signal pattern or to provide granular detail)
        for (HealthSignal signal : signals) {
            if (signal.getState() == HealthState.ANOMALY || signal.getState() == HealthState.DRIFT || signal.getState() == HealthState.FOLLOW_UP) {
                // If this metric is already part of an active multi-signal anomaly, avoid duplicating the primary alarm
                boolean alreadyInMulti = anomalies.stream()
                        .filter(AnomalyEvent::isMultiSignalCorrelationDetected)
                        .anyMatch(a -> a.getAffectedSignals().contains(signal.getMetric()));

                if (!alreadyInMulti || signal.getState() == HealthState.ANOMALY) {
                    AnomalyEvent singleEvent = new AnomalyEvent();
                    singleEvent.setUserId(userId);
                    singleEvent.setState(signal.getState());
                    singleEvent.setPrimaryMetric(signal.getMetric());
                    singleEvent.setAffectedSignals(List.of(signal.getMetric()));
                    singleEvent.setMultiSignalCorrelationDetected(false);
                    singleEvent.setPatternName(formatMetricLabel(signal.getMetric()) + " " + signal.getState().name());
                    singleEvent.setDurationDays(Math.max(signal.getPersistenceDays(), 1));
                    singleEvent.setConfidence(signal.getDataConfidence());
                    singleEvent.setBaselineComparison(String.format(
                            "Current %s %s vs Personal Baseline Mean %s %s (Δ %s%.2f %s)",
                            signal.getCurrentValue(), signal.getUnit(),
                            signal.getBaselineMean(), signal.getUnit(),
                            signal.getDeviation().doubleValue() >= 0 ? "+" : "",
                            signal.getDeviation().doubleValue(), signal.getUnit()
                    ));
                    singleEvent.setReason(String.format("Statistical departure: %s persisted for %d day(s).",
                            signal.getState().name(), singleEvent.getDurationDays()));
                    singleEvent.setExplanation(signal.getExplanation());
                    anomalies.add(singleEvent);
                }
            }
        }

        return anomalies;
    }

    private boolean isAutonomicStrainPattern(HealthSignal sleep, HealthSignal rhr, HealthSignal hrv) {
        if (sleep == null || rhr == null || hrv == null) return false;
        if (sleep.getBaselineMean() == null || rhr.getBaselineMean() == null || hrv.getBaselineMean() == null) return false;

        boolean sleepReduced = sleep.getDeviation() != null && sleep.getDeviation().doubleValue() <= -0.75; // >= 45 mins less sleep
        boolean rhrElevated = rhr.getDeviation() != null && rhr.getDeviation().doubleValue() >= 4.0; // >= 4 bpm above baseline
        boolean hrvDepressed = hrv.getDeviation() != null && hrv.getDeviation().doubleValue() <= -5.0; // >= 5 ms below baseline

        int matchCount = 0;
        if (sleepReduced) matchCount++;
        if (rhrElevated) matchCount++;
        if (hrvDepressed) matchCount++;

        // At least 2 of 3 concordant signals, with at least one persistent for >= 2 days
        int maxDays = Math.max(sleep.getPersistenceDays(), Math.max(rhr.getPersistenceDays(), hrv.getPersistenceDays()));
        return matchCount >= 2 && maxDays >= 2;
    }

    private boolean isRespiratoryStrainPattern(HealthSignal temp, HealthSignal spo2) {
        if (temp == null || spo2 == null) return false;
        if (temp.getBaselineMean() == null || spo2.getBaselineMean() == null) return false;

        boolean tempElevated = temp.getDeviation() != null && temp.getDeviation().doubleValue() >= 0.5; // +0.5 °C
        boolean spo2Depressed = spo2.getDeviation() != null && spo2.getDeviation().doubleValue() <= -1.5; // -1.5%

        return tempElevated && spo2Depressed;
    }

    private int calculatePersistenceDays(List<HealthObservation> obs, BigDecimal mean, BigDecimal stdDev) {
        if (obs == null || obs.isEmpty() || mean == null || stdDev == null || stdDev.doubleValue() < 0.0001) {
            return 0;
        }

        // Sort ascending by time
        List<HealthObservation> sorted = new ArrayList<>(obs);
        sorted.sort(Comparator.comparing(HealthObservation::getTimestamp));

        double meanVal = mean.doubleValue();
        double stdVal = stdDev.doubleValue();
        int consecutiveCount = 0;
        Instant persistenceStart = null;
        Instant persistenceEnd = null;

        // Traverse backwards from most recent
        for (int i = sorted.size() - 1; i >= 0; i--) {
            HealthObservation o = sorted.get(i);
            BigDecimal valBd = o.getStandardValueNumeric() != null ? o.getStandardValueNumeric() : o.getValueNumeric();
            if (valBd == null && o instanceof org.os4all.modules.ingestion.entity.LifestyleObservation lo && lo.getDurationMinutes() != null) {
                valBd = BigDecimal.valueOf(lo.getDurationMinutes() / 60.0);
            }
            if (valBd == null) continue;

            double diff = Math.abs(valBd.doubleValue() - meanVal);
            if (diff >= 0.75 * stdVal) { // deviating point
                consecutiveCount++;
                if (persistenceEnd == null) {
                    persistenceEnd = o.getTimestamp();
                }
                persistenceStart = o.getTimestamp();
            } else {
                break;
            }
        }

        if (persistenceStart != null && persistenceEnd != null) {
            long days = Duration.between(persistenceStart, persistenceEnd).toDays() + 1;
            return Math.max((int) days, consecutiveCount);
        }
        return consecutiveCount;
    }

    private int calculateConsecutiveDeviations(List<HealthObservation> obs, BigDecimal mean, BigDecimal stdDev) {
        if (obs == null || obs.isEmpty() || mean == null || stdDev == null || stdDev.doubleValue() < 0.0001) {
            return 0;
        }

        List<HealthObservation> sorted = new ArrayList<>(obs);
        sorted.sort(Comparator.comparing(HealthObservation::getTimestamp));

        double meanVal = mean.doubleValue();
        double stdVal = stdDev.doubleValue();
        int consecutive = 0;

        for (int i = sorted.size() - 1; i >= 0; i--) {
            HealthObservation o = sorted.get(i);
            BigDecimal valBd = o.getStandardValueNumeric() != null ? o.getStandardValueNumeric() : o.getValueNumeric();
            if (valBd == null) continue;

            double diff = Math.abs(valBd.doubleValue() - meanVal);
            if (diff >= 1.0 * stdVal) {
                consecutive++;
            } else {
                break;
            }
        }
        return consecutive;
    }

    private BigDecimal calculateAverageConfidence(List<HealthObservation> obs) {
        if (obs == null || obs.isEmpty()) {
            return BigDecimal.valueOf(1.0);
        }
        double sum = 0.0;
        int count = 0;
        for (HealthObservation o : obs) {
            if (o.getConfidence() != null) {
                sum += o.getConfidence().doubleValue();
                count++;
            }
        }
        return count > 0 ? BigDecimal.valueOf(sum / count).setScale(2, RoundingMode.HALF_UP) : BigDecimal.valueOf(1.0);
    }

    private List<HealthObservation> filterObservationsForMetric(String metricName, List<HealthObservation> list) {
        if (list == null) return Collections.emptyList();
        String m = metricName.toLowerCase(Locale.ROOT);

        return list.stream().filter(o -> {
            String obsTypeStr = o.getObservationType().name().toLowerCase(Locale.ROOT);
            if (m.contains("resting") && m.contains("heart")) {
                if (o instanceof org.os4all.modules.ingestion.entity.VitalMeasurement vm && vm.getVitalName() != null) {
                    return vm.getVitalName().toLowerCase(Locale.ROOT).contains("resting");
                }
                return false;
            }
            if (m.equals(PersonalBaselineService.METRIC_HEART_RATE)) {
                return obsTypeStr.equals("heart_rate");
            }
            if (m.equals(PersonalBaselineService.METRIC_HRV)) {
                return obsTypeStr.equals("hrv");
            }
            if (m.equals(PersonalBaselineService.METRIC_SPO2)) {
                return obsTypeStr.equals("oxygen_saturation");
            }
            if (m.equals(PersonalBaselineService.METRIC_TEMPERATURE)) {
                return obsTypeStr.equals("body_temperature");
            }
            if (m.equals(PersonalBaselineService.METRIC_STEPS)) {
                return obsTypeStr.equals("steps");
            }
            if (m.equals(PersonalBaselineService.METRIC_SLEEP_DURATION)) {
                return obsTypeStr.equals("sleep");
            }
            return false;
        }).toList();
    }

    private boolean isObservationStale(Instant lastObservedAt, int staleDays) {
        if (lastObservedAt == null) return true;
        return Instant.now().minus(staleDays, ChronoUnit.DAYS).isAfter(lastObservedAt);
    }

    private String formatMetricLabel(String metricKey) {
        if (metricKey == null) return "Metric";
        return switch (metricKey) {
            case PersonalBaselineService.METRIC_RESTING_HEART_RATE -> "Resting Heart Rate";
            case PersonalBaselineService.METRIC_HEART_RATE -> "Heart Rate";
            case PersonalBaselineService.METRIC_HRV -> "HRV";
            case PersonalBaselineService.METRIC_SPO2 -> "SpO2 (Blood Oxygen)";
            case PersonalBaselineService.METRIC_SLEEP_DURATION -> "Sleep Duration";
            case PersonalBaselineService.METRIC_STEPS -> "Daily Steps";
            case PersonalBaselineService.METRIC_TEMPERATURE -> "Body Temperature";
            default -> metricKey.replace("_", " ");
        };
    }
}
