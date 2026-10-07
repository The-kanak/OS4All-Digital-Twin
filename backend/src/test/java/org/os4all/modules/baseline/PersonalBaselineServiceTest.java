package org.os4all.modules.baseline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os4all.modules.baseline.model.BaselineMetric;
import org.os4all.modules.baseline.service.PersonalBaselineService;
import org.os4all.modules.baseline.service.PersonalBaselineService.MetricDataPoint;
import org.os4all.modules.ingestion.repository.HealthObservationRepository;
import org.os4all.modules.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PersonalBaselineServiceTest {

    private PersonalBaselineService baselineService;
    private HealthObservationRepository mockObservationRepo;
    private UserRepository mockUserRepo;

    @BeforeEach
    void setUp() {
        mockObservationRepo = mock(HealthObservationRepository.class);
        mockUserRepo = mock(UserRepository.class);
        baselineService = new PersonalBaselineService(mockObservationRepo, mockUserRepo);
    }

    private MetricDataPoint point(double value, int daysAgo) {
        return new MetricDataPoint(
                BigDecimal.valueOf(value),
                Instant.now().minus(daysAgo, ChronoUnit.DAYS),
                "bpm"
        );
    }

    @Test
    @DisplayName("1. Insufficient data: < 5 observations must NOT establish baseline")
    void testInsufficientData() {
        List<MetricDataPoint> sparsePoints = List.of(
                point(68.0, 3),
                point(70.0, 2),
                point(69.0, 1)
        );

        BaselineMetric result = baselineService.computeBaselineForMetric("resting_heart_rate", sparsePoints);

        assertNotNull(result);
        assertFalse(result.isBaselineEstablished(), "Baseline should not be established with only 3 points");
        assertEquals(3, result.getObservationCount());
        assertEquals("INSUFFICIENT_DATA", result.getTrendDirection());
        assertTrue(result.getStatusMessage().contains("Insufficient historical data"));
        assertTrue(result.getInterpretation().contains("Need 2 more observation(s)"));
        assertNull(result.getZScore(), "Z-score should be null when baseline is not established");
        assertNull(result.getBaselineRangeLow(), "Baseline range should be null when not established");
    }

    @Test
    @DisplayName("2. Stable data: consistent readings produce low std dev, STABLE trend, and expected baseline range")
    void testStableData() {
        List<MetricDataPoint> stablePoints = List.of(
                point(60.0, 7),
                point(61.0, 6),
                point(60.0, 5),
                point(61.0, 4),
                point(60.0, 3),
                point(60.0, 2),
                point(61.0, 1)
        );

        BaselineMetric result = baselineService.computeBaselineForMetric("resting_heart_rate", stablePoints);

        assertTrue(result.isBaselineEstablished());
        assertEquals(7, result.getObservationCount());
        assertEquals(new BigDecimal("60.00"), result.getMin());
        assertEquals(new BigDecimal("61.00"), result.getMax());
        assertEquals(new BigDecimal("60.43"), result.getMean());
        assertEquals(new BigDecimal("60.00"), result.getMedian());
        assertNotNull(result.getStandardDeviation());
        assertTrue(result.getStandardDeviation().doubleValue() < 1.0);
        assertEquals("STABLE", result.getTrendDirection());
        assertTrue(result.getInterpretation().contains("consistent with your personal baseline"));
        assertTrue(result.getInterpretation().contains("not a medical diagnosis"));
    }

    @Test
    @DisplayName("3. Increasing trend: recent spike produces positive z-score and INCREASING trend")
    void testIncreasingTrend() {
        // Historical baseline ~ 60 bpm, then recent rises to 78, 80, 82 bpm
        List<MetricDataPoint> increasingPoints = List.of(
                point(60.0, 9),
                point(61.0, 8),
                point(60.0, 7),
                point(62.0, 6),
                point(61.0, 5),
                point(78.0, 3),
                point(80.0, 2),
                point(82.0, 1)
        );

        BaselineMetric result = baselineService.computeBaselineForMetric("resting_heart_rate", increasingPoints);

        assertTrue(result.isBaselineEstablished());
        assertEquals(8, result.getObservationCount());
        assertEquals("INCREASING", result.getTrendDirection());
        assertNotNull(result.getZScore());
        assertTrue(result.getZScore() > 1.0, "Z-score should be elevated for increasing recent average");
        assertTrue(result.getDeviationFromBaseline().doubleValue() > 0);
        assertTrue(result.getInterpretation().contains("elevated"));
        assertTrue(result.getInterpretation().contains("not a medical diagnosis"));
    }

    @Test
    @DisplayName("4. Decreasing trend: recent drop produces negative z-score and DECREASING trend")
    void testDecreasingTrend() {
        // Historical baseline ~ 85 bpm, then recent drops to 62, 60, 59 bpm
        List<MetricDataPoint> decreasingPoints = List.of(
                point(85.0, 9),
                point(86.0, 8),
                point(84.0, 7),
                point(85.0, 6),
                point(86.0, 5),
                point(62.0, 3),
                point(60.0, 2),
                point(59.0, 1)
        );

        BaselineMetric result = baselineService.computeBaselineForMetric("resting_heart_rate", decreasingPoints);

        assertTrue(result.isBaselineEstablished());
        assertEquals(8, result.getObservationCount());
        assertEquals("DECREASING", result.getTrendDirection());
        assertNotNull(result.getZScore());
        assertTrue(result.getZScore() < -1.0, "Z-score should be negative for decreasing recent average");
        assertTrue(result.getDeviationFromBaseline().doubleValue() < 0);
        assertTrue(result.getInterpretation().contains("lower"));
        assertTrue(result.getInterpretation().contains("not a medical diagnosis"));
    }

    @Test
    @DisplayName("5. Noisy data: high variance calculates wider baseline bounds and median handles dispersion")
    void testNoisyData() {
        List<MetricDataPoint> noisyPoints = List.of(
                point(50.0, 7),
                point(85.0, 6),
                point(52.0, 5),
                point(90.0, 4),
                point(48.0, 3),
                point(88.0, 2),
                point(60.0, 1)
        );

        BaselineMetric result = baselineService.computeBaselineForMetric("heart_rate", noisyPoints);

        assertTrue(result.isBaselineEstablished());
        assertEquals(7, result.getObservationCount());
        assertTrue(result.getStandardDeviation().doubleValue() > 15.0, "Standard deviation should reflect high noise");
        // Check that baseline low and high spread wide to accommodate personal volatility
        assertTrue(result.getBaselineRangeHigh().doubleValue() > result.getMean().doubleValue());
        assertTrue(result.getBaselineRangeLow().doubleValue() < result.getMean().doubleValue());
    }

    @Test
    @DisplayName("6. Missing values: null values in sequence are gracefully ignored")
    void testMissingValues() {
        List<MetricDataPoint> withNulls = new ArrayList<>();
        withNulls.add(point(70.0, 8));
        withNulls.add(new MetricDataPoint(null, Instant.now().minus(7, ChronoUnit.DAYS), "bpm"));
        withNulls.add(point(72.0, 6));
        withNulls.add(new MetricDataPoint(null, Instant.now().minus(5, ChronoUnit.DAYS), "bpm"));
        withNulls.add(point(71.0, 4));
        withNulls.add(point(70.0, 3));
        withNulls.add(point(72.0, 2));

        BaselineMetric result = baselineService.computeBaselineForMetric("resting_heart_rate", withNulls);

        // 5 valid non-null points out of 7
        assertEquals(5, result.getObservationCount());
        assertTrue(result.isBaselineEstablished());
        assertEquals(new BigDecimal("71.00"), result.getMean());
        assertEquals(new BigDecimal("71.00"), result.getMedian());
    }

    @Test
    @DisplayName("7. Outliers: extreme outlier does not break median, min and max correctly bounded")
    void testOutliers() {
        // Normal physiological series around 70, with one sensor artifact glitch of 210 bpm
        List<MetricDataPoint> outlierPoints = List.of(
                point(68.0, 7),
                point(70.0, 6),
                point(69.0, 5),
                point(71.0, 4),
                point(210.0, 3), // sensor glitch outlier
                point(70.0, 2),
                point(69.0, 1)
        );

        BaselineMetric result = baselineService.computeBaselineForMetric("resting_heart_rate", outlierPoints);

        assertTrue(result.isBaselineEstablished());
        assertEquals(7, result.getObservationCount());
        assertEquals(new BigDecimal("68.00"), result.getMin());
        assertEquals(new BigDecimal("210.00"), result.getMax());
        // Median remains robust against the single 210 outlier
        assertEquals(new BigDecimal("70.00"), result.getMedian());
        // Mean is pulled upwards by outlier
        assertTrue(result.getMean().doubleValue() > 80.0);
    }
}
