package org.os4all.modules.baseline.service;

import org.os4all.core.exception.ResourceNotFoundException;
import org.os4all.modules.baseline.model.BaselineMetric;
import org.os4all.modules.baseline.model.BaselineProfile;
import org.os4all.modules.ingestion.entity.HealthObservation;
import org.os4all.modules.ingestion.entity.LifestyleObservation;
import org.os4all.modules.ingestion.entity.ObservationType;
import org.os4all.modules.ingestion.entity.VitalMeasurement;
import org.os4all.modules.ingestion.repository.HealthObservationRepository;
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

@Service
public class PersonalBaselineService {

    private static final Logger log = LoggerFactory.getLogger(PersonalBaselineService.class);

    // Minimum observation threshold required to establish a reliable baseline
    public static final int MIN_OBSERVATIONS_THRESHOLD = 5;
    // Number of recent points used to calculate the recent average
    public static final int RECENT_WINDOW_SIZE = 3;

    // Supported continuous metrics
    public static final String METRIC_RESTING_HEART_RATE = "resting_heart_rate";
    public static final String METRIC_HEART_RATE = "heart_rate";
    public static final String METRIC_HRV = "hrv";
    public static final String METRIC_SPO2 = "spo2";
    public static final String METRIC_SLEEP_DURATION = "sleep_duration";
    public static final String METRIC_STEPS = "steps";
    public static final String METRIC_TEMPERATURE = "temperature";

    public static final List<String> SUPPORTED_METRICS = List.of(
            METRIC_RESTING_HEART_RATE,
            METRIC_HEART_RATE,
            METRIC_HRV,
            METRIC_SPO2,
            METRIC_SLEEP_DURATION,
            METRIC_STEPS,
            METRIC_TEMPERATURE
    );

    private final HealthObservationRepository observationRepository;
    private final UserRepository userRepository;

    public PersonalBaselineService(
            HealthObservationRepository observationRepository,
            UserRepository userRepository
    ) {
        this.observationRepository = observationRepository;
        this.userRepository = userRepository;
    }

    public record MetricDataPoint(BigDecimal value, Instant timestamp, String unit) {}

    @Transactional(readOnly = true)
    public BaselineProfile calculateBaselineProfile(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        // Pull observations for user from last 90 days window
        Instant windowStart = Instant.now().minus(90, ChronoUnit.DAYS);
        List<HealthObservation> rawObservations = observationRepository.findObservationsForBaseline(userId, windowStart);

        Map<String, List<MetricDataPoint>> partitionedData = partitionObservationsByMetric(rawObservations);

        BaselineProfile profile = new BaselineProfile(userId);
        for (String metricKey : SUPPORTED_METRICS) {
            List<MetricDataPoint> dataPoints = partitionedData.getOrDefault(metricKey, Collections.emptyList());
            BaselineMetric metricBaseline = computeBaselineForMetric(metricKey, dataPoints);
            profile.addMetric(metricBaseline);
        }

        return profile;
    }

    @Transactional(readOnly = true)
    public BaselineMetric calculateSingleMetricBaseline(UUID userId, String rawMetricKey) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        String canonicalMetric = canonicalizeMetricKey(rawMetricKey);
        Instant windowStart = Instant.now().minus(90, ChronoUnit.DAYS);
        List<HealthObservation> rawObservations = observationRepository.findObservationsForBaseline(userId, windowStart);

        Map<String, List<MetricDataPoint>> partitionedData = partitionObservationsByMetric(rawObservations);
        List<MetricDataPoint> dataPoints = partitionedData.getOrDefault(canonicalMetric, Collections.emptyList());

        return computeBaselineForMetric(canonicalMetric, dataPoints);
    }

    /**
     * Pure statistical calculation for a series of values.
     * Can be invoked directly by unit tests or domain logic.
     */
    public BaselineMetric computeBaselineForMetric(String metricName, List<MetricDataPoint> points) {
        BaselineMetric metric = new BaselineMetric();
        metric.setMetric(metricName);
        metric.setRequiredObservations(MIN_OBSERVATIONS_THRESHOLD);

        // Filter out missing/null values and handle empty series
        List<MetricDataPoint> validPoints = points != null ?
                points.stream().filter(p -> p.value() != null).toList() : Collections.emptyList();

        int count = validPoints.size();
        metric.setObservationCount(count);

        if (count > 0) {
            metric.setLastObservedAt(validPoints.get(count - 1).timestamp());
            metric.setUnit(resolveUnit(metricName, validPoints.get(count - 1).unit()));
        } else {
            metric.setUnit(getDefaultUnit(metricName));
        }

        // 1. Guard against insufficient data (fewer than MIN_OBSERVATIONS_THRESHOLD points)
        if (count < MIN_OBSERVATIONS_THRESHOLD) {
            metric.setBaselineEstablished(false);
            metric.setStatusMessage(String.format(
                    "Insufficient historical data (%d of %d required observations). An accurate personal baseline cannot be established yet.",
                    count, MIN_OBSERVATIONS_THRESHOLD
            ));
            metric.setTrendDirection("INSUFFICIENT_DATA");
            metric.setInterpretation(String.format(
                    "Need %d more observation(s) to compute your personalized baseline.",
                    MIN_OBSERVATIONS_THRESHOLD - count
            ));

            if (count > 0) {
                // Compute preliminary raw averages without declaring established baseline
                List<Double> rawValues = validPoints.stream().map(p -> p.value().doubleValue()).toList();
                metric.setMin(BigDecimal.valueOf(Collections.min(rawValues)).setScale(2, RoundingMode.HALF_UP));
                metric.setMax(BigDecimal.valueOf(Collections.max(rawValues)).setScale(2, RoundingMode.HALF_UP));
                double sum = rawValues.stream().mapToDouble(Double::doubleValue).sum();
                metric.setMean(BigDecimal.valueOf(sum / count).setScale(2, RoundingMode.HALF_UP));
                metric.setMedian(computeMedian(rawValues));
            }
            return metric;
        }

        // 2. Sufficient historical data available: compute full baseline statistics
        metric.setBaselineEstablished(true);
        metric.setStatusMessage("Personal baseline established from " + count + " historical observations.");

        List<Double> values = validPoints.stream().map(p -> p.value().doubleValue()).toList();

        // Min & Max
        double minVal = Collections.min(values);
        double maxVal = Collections.max(values);
        metric.setMin(BigDecimal.valueOf(minVal).setScale(2, RoundingMode.HALF_UP));
        metric.setMax(BigDecimal.valueOf(maxVal).setScale(2, RoundingMode.HALF_UP));

        // Mean
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        double meanVal = sum / count;
        BigDecimal meanBd = BigDecimal.valueOf(meanVal).setScale(2, RoundingMode.HALF_UP);
        metric.setMean(meanBd);

        // Median
        BigDecimal medianBd = computeMedian(values);
        metric.setMedian(medianBd);

        // Sample Standard Deviation (with Bessel's correction, N-1)
        double varianceSum = 0.0;
        for (double v : values) {
            varianceSum += Math.pow(v - meanVal, 2);
        }
        double stdDevVal = Math.sqrt(varianceSum / (count - 1));
        BigDecimal stdDevBd = BigDecimal.valueOf(stdDevVal).setScale(2, RoundingMode.HALF_UP);
        metric.setStandardDeviation(stdDevBd);

        // Baseline Range: [mean - 1.96 * stdDev, mean + 1.96 * stdDev]
        double rangeLow = meanVal - (1.96 * stdDevVal);
        double rangeHigh = meanVal + (1.96 * stdDevVal);
        if (rangeLow < 0 && isNonNegativeMetric(metricName)) {
            rangeLow = 0.0;
        }
        metric.setBaselineRangeLow(BigDecimal.valueOf(rangeLow).setScale(2, RoundingMode.HALF_UP));
        metric.setBaselineRangeHigh(BigDecimal.valueOf(rangeHigh).setScale(2, RoundingMode.HALF_UP));

        // Recent Average (last 3 observations)
        int recentWindow = Math.min(RECENT_WINDOW_SIZE, count);
        metric.setRecentObservationCount(recentWindow);
        List<Double> recentSublist = values.subList(count - recentWindow, count);
        double recentSum = recentSublist.stream().mapToDouble(Double::doubleValue).sum();
        double recentAvgVal = recentSum / recentWindow;
        BigDecimal recentAvgBd = BigDecimal.valueOf(recentAvgVal).setScale(2, RoundingMode.HALF_UP);
        metric.setRecentAverage(recentAvgBd);

        // Deviation from personal baseline
        double deviationVal = recentAvgVal - meanVal;
        metric.setDeviationFromBaseline(BigDecimal.valueOf(deviationVal).setScale(2, RoundingMode.HALF_UP));

        if (meanVal != 0) {
            double pct = (deviationVal / meanVal) * 100.0;
            metric.setDeviationPercentage(BigDecimal.valueOf(pct).setScale(2, RoundingMode.HALF_UP));
        }

        // Z-score calculation (when statistically appropriate: stdDev > 0)
        Double zScore = null;
        if (stdDevVal > 0.0001) {
            zScore = deviationVal / stdDevVal;
            metric.setZScore(BigDecimal.valueOf(zScore).setScale(2, RoundingMode.HALF_UP).doubleValue());
        } else {
            metric.setZScore(null); // When standard deviation is 0 (all points identical), z-score is undefined
        }

        // Trend Direction & Interpretation
        determineTrendAndInterpretation(metric, zScore, deviationVal, meanVal, metric.getUnit());

        return metric;
    }

    private void determineTrendAndInterpretation(
            BaselineMetric metric,
            Double zScore,
            double deviationVal,
            double meanVal,
            String unit
    ) {
        String direction;
        String desc;

        double pctDeviation = meanVal != 0 ? (deviationVal / meanVal) * 100.0 : 0.0;

        if (zScore != null) {
            if (zScore >= 1.0 || pctDeviation >= 7.5) {
                direction = "INCREASING";
                desc = String.format("Recent average is elevated (+%.2f %s, +%.2fσ) above your personal baseline average.",
                        Math.abs(deviationVal), unit, zScore);
            } else if (zScore <= -1.0 || pctDeviation <= -7.5) {
                direction = "DECREASING";
                desc = String.format("Recent average is lower (-%.2f %s, %.2fσ) than your personal baseline average.",
                        Math.abs(deviationVal), unit, zScore);
            } else {
                direction = "STABLE";
                desc = String.format("Recent readings are consistent with your personal baseline (deviation of %s%.2f %s).",
                        deviationVal >= 0 ? "+" : "", deviationVal, unit);
            }
        } else {
            // No variance / standard deviation zero
            if (Math.abs(deviationVal) < 0.001) {
                direction = "STABLE";
                desc = "Readings are strictly constant and aligned with your historical pattern.";
            } else if (deviationVal > 0) {
                direction = "INCREASING";
                desc = String.format("Recent average increased by +%.2f %s compared to your historical baseline.", deviationVal, unit);
            } else {
                direction = "DECREASING";
                desc = String.format("Recent average decreased by -%.2f %s compared to your historical baseline.", Math.abs(deviationVal), unit);
            }
        }

        metric.setTrendDirection(direction);
        metric.setInterpretation(desc + " This statistical variation reflects your individual pattern and is not a medical diagnosis.");
    }

    private BigDecimal computeMedian(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        double median;
        if (n % 2 == 1) {
            median = sorted.get(n / 2);
        } else {
            median = (sorted.get((n / 2) - 1) + sorted.get(n / 2)) / 2.0;
        }
        return BigDecimal.valueOf(median).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, List<MetricDataPoint>> partitionObservationsByMetric(List<HealthObservation> list) {
        Map<String, List<MetricDataPoint>> map = new HashMap<>();
        for (String m : SUPPORTED_METRICS) {
            map.put(m, new ArrayList<>());
        }

        if (list == null) return map;

        for (HealthObservation obs : list) {
            if (obs.getValueNumeric() == null && obs.getStandardValueNumeric() == null) {
                // If lifestyle observation has durationMinutes, map that
                if (obs instanceof LifestyleObservation lifestyle && lifestyle.getDurationMinutes() != null) {
                    if (obs.getObservationType() == ObservationType.SLEEP) {
                        double hours = lifestyle.getDurationMinutes() / 60.0;
                        map.get(METRIC_SLEEP_DURATION).add(new MetricDataPoint(
                                BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP),
                                obs.getTimestamp(),
                                "hours"
                        ));
                    }
                }
                continue;
            }

            BigDecimal val = obs.getStandardValueNumeric() != null ? obs.getStandardValueNumeric() : obs.getValueNumeric();
            String u = obs.getStandardUnit() != null ? obs.getStandardUnit() : obs.getUnit();
            Instant ts = obs.getTimestamp();

            if (obs instanceof VitalMeasurement vital) {
                String vitalName = vital.getVitalName() != null ? vital.getVitalName().toLowerCase(Locale.ROOT) : "";
                if (vitalName.contains("resting") && vitalName.contains("heart")) {
                    map.get(METRIC_RESTING_HEART_RATE).add(new MetricDataPoint(val, ts, u != null ? u : "bpm"));
                    continue;
                }
            }

            switch (obs.getObservationType()) {
                case HEART_RATE -> {
                    // Check if resting
                    if (obs instanceof VitalMeasurement vital && vital.getVitalName() != null && vital.getVitalName().toLowerCase(Locale.ROOT).contains("resting")) {
                        map.get(METRIC_RESTING_HEART_RATE).add(new MetricDataPoint(val, ts, u != null ? u : "bpm"));
                    } else {
                        map.get(METRIC_HEART_RATE).add(new MetricDataPoint(val, ts, u != null ? u : "bpm"));
                    }
                }
                case HRV -> map.get(METRIC_HRV).add(new MetricDataPoint(val, ts, u != null ? u : "ms"));
                case OXYGEN_SATURATION -> map.get(METRIC_SPO2).add(new MetricDataPoint(val, ts, u != null ? u : "%"));
                case BODY_TEMPERATURE -> map.get(METRIC_TEMPERATURE).add(new MetricDataPoint(val, ts, u != null ? u : "°C"));
                case STEPS -> map.get(METRIC_STEPS).add(new MetricDataPoint(val, ts, u != null ? u : "steps"));
                case SLEEP -> {
                    // If stored as duration in minutes or hours
                    double hours = val.doubleValue();
                    if (hours > 24) {
                        // Assuming minutes e.g. 450 mins -> 7.5 hrs
                        hours = hours / 60.0;
                    }
                    map.get(METRIC_SLEEP_DURATION).add(new MetricDataPoint(
                            BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP),
                            ts, "hours"
                    ));
                }
                default -> {
                    // Other observation types
                }
            }
        }

        return map;
    }

    public String canonicalizeMetricKey(String raw) {
        if (raw == null) return METRIC_HEART_RATE;
        String clean = raw.trim().toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        return switch (clean) {
            case "rhr", "resting_heart_rate", "resting_hr", "resting" -> METRIC_RESTING_HEART_RATE;
            case "hr", "heart_rate", "heartrate", "pulse" -> METRIC_HEART_RATE;
            case "hrv", "heart_rate_variability" -> METRIC_HRV;
            case "spo2", "oxygen", "oxygen_saturation", "o2", "blood_oxygen" -> METRIC_SPO2;
            case "sleep", "sleep_duration", "sleep_hours" -> METRIC_SLEEP_DURATION;
            case "steps", "step_count", "daily_steps" -> METRIC_STEPS;
            case "temperature", "body_temperature", "temp" -> METRIC_TEMPERATURE;
            default -> clean;
        };
    }

    private String getDefaultUnit(String metric) {
        return switch (metric) {
            case METRIC_RESTING_HEART_RATE, METRIC_HEART_RATE -> "bpm";
            case METRIC_HRV -> "ms";
            case METRIC_SPO2 -> "%";
            case METRIC_SLEEP_DURATION -> "hours";
            case METRIC_STEPS -> "steps";
            case METRIC_TEMPERATURE -> "°C";
            default -> "units";
        };
    }

    private String resolveUnit(String metric, String observedUnit) {
        if (observedUnit != null && !observedUnit.trim().isEmpty()) {
            return observedUnit;
        }
        return getDefaultUnit(metric);
    }

    private boolean isNonNegativeMetric(String metric) {
        return !METRIC_TEMPERATURE.equals(metric);
    }
}
