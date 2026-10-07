package org.os4all.modules.digitaltwin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os4all.modules.digitaltwin.engine.MetabolicPredictionEngine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("2-Hour Glucose Trajectory Projection Engine Tests")
class TrajectoryProjectionTest {

    private MetabolicPredictionEngine.EngineeredFeatures createBaseFeatures(
            BigDecimal currentGlucose,
            BigDecimal velocity,
            boolean hasT2D,
            String activityLevel,
            BigDecimal confidence,
            Instant timestamp
    ) {
        return new MetabolicPredictionEngine.EngineeredFeatures(
                currentGlucose,
                BigDecimal.ZERO,
                new BigDecimal("16.0"),
                BigDecimal.ZERO,
                0.0,
                BigDecimal.ZERO,
                0.0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("24.0"),
                new BigDecimal("5.4"),
                new BigDecimal("90.0"),
                14,
                true,
                hasT2D,
                activityLevel,
                velocity,
                new BigDecimal("95.0"),
                confidence,
                timestamp
        );
    }

    @Test
    @DisplayName("1. Stable trajectory: velocity = 0 -> STABLE direction with minimal delta")
    void testStableTrajectory_VelocityZero() {
        MetabolicPredictionEngine.EngineeredFeatures features = createBaseFeatures(
                new BigDecimal("95.0"),
                BigDecimal.ZERO,
                false,
                "MODERATE",
                BigDecimal.valueOf(0.98),
                Instant.now()
        );

        MetabolicPredictionEngine.GlucoseTrajectoryProjection proj =
                MetabolicPredictionEngine.calculateTrajectoryProjection(features);

        assertNotNull(proj);
        assertEquals("STABLE", proj.trajectoryDirection());
        assertEquals(0, new BigDecimal("95.0").compareTo(proj.currentGlucose()));
        assertEquals("Next 2 Hours", proj.horizonWindow());
        assertTrue(proj.projectedDeltaMgDl().abs().doubleValue() < 5.0, "Delta should be minimal for zero velocity");
        assertEquals(5, proj.trajectoryPoints().size(), "Should have exactly 5 milestone points (0, 30, 60, 90, 120)");
        assertEquals(0, proj.trajectoryPoints().get(0).minuteOffset());
        assertEquals(120, proj.trajectoryPoints().get(4).minuteOffset());
    }

    @Test
    @DisplayName("2. Positive velocity: velocity > 0 -> RISING direction and higher 120-minute glucose")
    void testRisingTrajectory_PositiveVelocity() {
        MetabolicPredictionEngine.EngineeredFeatures features = createBaseFeatures(
                new BigDecimal("110.0"),
                new BigDecimal("1.20"), // +1.2 mg/dL/min
                false,
                "MODERATE",
                BigDecimal.valueOf(0.98),
                Instant.now()
        );

        MetabolicPredictionEngine.GlucoseTrajectoryProjection proj =
                MetabolicPredictionEngine.calculateTrajectoryProjection(features);

        assertNotNull(proj);
        assertEquals("RISING", proj.trajectoryDirection());
        assertTrue(proj.projectedGlucose120Min().doubleValue() > 110.0, "Projected 120min should be higher than baseline 110");
        assertTrue(proj.projectedDeltaMgDl().doubleValue() > 20.0, "Delta should reflect positive rise");
        assertTrue(proj.disclaimer().contains("Prototype"));
    }

    @Test
    @DisplayName("3. Negative velocity: velocity < 0 -> FALLING direction and lower 120-minute glucose")
    void testFallingTrajectory_NegativeVelocity() {
        MetabolicPredictionEngine.EngineeredFeatures features = createBaseFeatures(
                new BigDecimal("160.0"),
                new BigDecimal("-0.80"), // -0.8 mg/dL/min
                false,
                "MODERATE",
                BigDecimal.valueOf(0.98),
                Instant.now()
        );

        MetabolicPredictionEngine.GlucoseTrajectoryProjection proj =
                MetabolicPredictionEngine.calculateTrajectoryProjection(features);

        assertNotNull(proj);
        assertEquals("FALLING", proj.trajectoryDirection());
        assertTrue(proj.projectedGlucose120Min().doubleValue() < 160.0, "Projected 120min should be lower than baseline 160");
        assertTrue(proj.projectedDeltaMgDl().doubleValue() < -15.0, "Delta should reflect downward slope");
    }

    @Test
    @DisplayName("4. Extreme velocity bounds: clamped to safe physiologically reasonable limits")
    void testExtremeVelocityClamping() {
        // Test extreme high velocity (+10.0 mg/dL/min)
        MetabolicPredictionEngine.EngineeredFeatures extremeHigh = createBaseFeatures(
                new BigDecimal("250.0"),
                new BigDecimal("10.0"),
                true,
                "SEDENTARY",
                BigDecimal.valueOf(0.98),
                Instant.now()
        );

        MetabolicPredictionEngine.GlucoseTrajectoryProjection highProj =
                MetabolicPredictionEngine.calculateTrajectoryProjection(extremeHigh);

        assertEquals(new BigDecimal("3.00"), highProj.glucoseVelocityMgDlPerMin(), "Velocity should be clamped to +3.00 mg/dL/min");
        assertTrue(highProj.projectedGlucose120Min().doubleValue() <= 400.0, "Glucose should be clamped to max 400.0");

        // Test extreme low velocity (-10.0 mg/dL/min) on low starting glucose
        MetabolicPredictionEngine.EngineeredFeatures extremeLow = createBaseFeatures(
                new BigDecimal("60.0"),
                new BigDecimal("-10.0"),
                false,
                "VIGOROUS",
                BigDecimal.valueOf(0.98),
                Instant.now()
        );

        MetabolicPredictionEngine.GlucoseTrajectoryProjection lowProj =
                MetabolicPredictionEngine.calculateTrajectoryProjection(extremeLow);

        assertEquals(new BigDecimal("-3.00"), lowProj.glucoseVelocityMgDlPerMin(), "Velocity should be clamped to -3.00 mg/dL/min");
        assertTrue(lowProj.projectedGlucose120Min().doubleValue() >= 40.0, "Glucose should be clamped to min 40.0");
    }

    @Test
    @DisplayName("5. Null or missing velocity handling: defaults gracefully without throwing")
    void testNullVelocity_GracefulFallback() {
        MetabolicPredictionEngine.EngineeredFeatures nullVelFeatures = createBaseFeatures(
                new BigDecimal("105.0"),
                null,
                false,
                "MODERATE",
                BigDecimal.valueOf(0.95),
                Instant.now()
        );

        assertDoesNotThrow(() -> {
            MetabolicPredictionEngine.GlucoseTrajectoryProjection proj =
                    MetabolicPredictionEngine.calculateTrajectoryProjection(nullVelFeatures);
            assertNotNull(proj);
            assertNotNull(proj.glucoseVelocityMgDlPerMin());
            assertNotNull(proj.projectedGlucose120Min());
            assertNotNull(proj.trajectoryDirection());
        });
    }

    @Test
    @DisplayName("6. Low sensor confidence handling: dampens confidence and bounds projection")
    void testLowSensorConfidenceHandling() {
        MetabolicPredictionEngine.EngineeredFeatures lowConfidenceFeatures = createBaseFeatures(
                new BigDecimal("115.0"),
                new BigDecimal("1.00"),
                false,
                "MODERATE",
                new BigDecimal("0.45"), // low confidence 45%
                Instant.now()
        );

        MetabolicPredictionEngine.GlucoseTrajectoryProjection proj =
                MetabolicPredictionEngine.calculateTrajectoryProjection(lowConfidenceFeatures);

        assertNotNull(proj);
        assertTrue(proj.confidence().doubleValue() <= 0.65, "Confidence score should be degraded for low sensor reliability");
    }

    @Test
    @DisplayName("7. Stale telemetry handling: penalizes confidence and dampens velocity by 80%")
    void testStaleTelemetryHandling() {
        Instant staleTime = Instant.now().minus(90, ChronoUnit.MINUTES); // 90 min old (>60m stale threshold)

        MetabolicPredictionEngine.EngineeredFeatures freshFeatures = createBaseFeatures(
                new BigDecimal("100.0"),
                new BigDecimal("1.50"),
                false,
                "MODERATE",
                new BigDecimal("0.98"),
                Instant.now()
        );

        MetabolicPredictionEngine.EngineeredFeatures staleFeatures = createBaseFeatures(
                new BigDecimal("100.0"),
                new BigDecimal("1.50"),
                false,
                "MODERATE",
                new BigDecimal("0.98"),
                staleTime
        );

        MetabolicPredictionEngine.GlucoseTrajectoryProjection freshProj =
                MetabolicPredictionEngine.calculateTrajectoryProjection(freshFeatures);
        MetabolicPredictionEngine.GlucoseTrajectoryProjection staleProj =
                MetabolicPredictionEngine.calculateTrajectoryProjection(staleFeatures);

        assertTrue(staleProj.confidence().doubleValue() < freshProj.confidence().doubleValue(),
                "Stale telemetry should have significantly lower confidence");
        assertTrue(staleProj.projectedDeltaMgDl().doubleValue() < freshProj.projectedDeltaMgDl().doubleValue(),
                "Stale telemetry should have dampened delta due to velocity decay factor");
    }

    @Test
    @DisplayName("8. Static EHR integration: T2D diagnosis has impaired clearance curve vs non-diabetic")
    void testStaticEHR_T2DImpairedClearanceDifference() {
        BigDecimal initialRise = new BigDecimal("1.00"); // +1.0 mg/dL/min rise

        MetabolicPredictionEngine.EngineeredFeatures nonDiabetic = createBaseFeatures(
                new BigDecimal("100.0"),
                initialRise,
                false, // Non-diabetic (lambda = 0.015)
                "MODERATE",
                new BigDecimal("0.98"),
                Instant.now()
        );

        MetabolicPredictionEngine.EngineeredFeatures diabetic = createBaseFeatures(
                new BigDecimal("100.0"),
                initialRise,
                true,  // T2D (lambda = 0.010, lower insulin sensitivity)
                "MODERATE",
                new BigDecimal("0.98"),
                Instant.now()
        );

        MetabolicPredictionEngine.GlucoseTrajectoryProjection nonDiabeticProj =
                MetabolicPredictionEngine.calculateTrajectoryProjection(nonDiabetic);
        MetabolicPredictionEngine.GlucoseTrajectoryProjection diabeticProj =
                MetabolicPredictionEngine.calculateTrajectoryProjection(diabetic);

        assertTrue(diabeticProj.projectedGlucose120Min().doubleValue() > nonDiabeticProj.projectedGlucose120Min().doubleValue(),
                "Diabetic patient with impaired clearance should have higher projected 120m glucose than non-diabetic");
    }

    @Test
    @DisplayName("9. Trajectory points milestone structure: exact 5 milestones with timestamps")
    void testTrajectoryPointsMilestoneStructure() {
        MetabolicPredictionEngine.EngineeredFeatures features = createBaseFeatures(
                new BigDecimal("120.0"),
                new BigDecimal("0.50"),
                false,
                "MODERATE",
                new BigDecimal("0.98"),
                Instant.now()
        );

        MetabolicPredictionEngine.PredictionOutput output = MetabolicPredictionEngine.predict(features);

        assertNotNull(output.trajectoryProjection());
        List<MetabolicPredictionEngine.TrajectoryPoint> points = output.trajectoryProjection().trajectoryPoints();
        assertEquals(5, points.size());
        assertEquals(0, points.get(0).minuteOffset());
        assertEquals(30, points.get(1).minuteOffset());
        assertEquals(60, points.get(2).minuteOffset());
        assertEquals(90, points.get(3).minuteOffset());
        assertEquals(120, points.get(4).minuteOffset());

        for (MetabolicPredictionEngine.TrajectoryPoint pt : points) {
            assertNotNull(pt.projectedGlucose());
            assertNotNull(pt.trendDirection());
        }
    }

    @Test
    @DisplayName("10. GLUT4 Vigorous activity clearance: accelerates clearance and lowers curve")
    void testGlut4VigorousActivityClearance() {
        MetabolicPredictionEngine.EngineeredFeatures sedentary = createBaseFeatures(
                new BigDecimal("120.0"),
                new BigDecimal("1.00"),
                false,
                "SEDENTARY",
                new BigDecimal("0.98"),
                Instant.now()
        );

        MetabolicPredictionEngine.EngineeredFeatures vigorous = createBaseFeatures(
                new BigDecimal("120.0"),
                new BigDecimal("1.00"),
                false,
                "VIGOROUS",
                new BigDecimal("0.98"),
                Instant.now()
        );

        MetabolicPredictionEngine.GlucoseTrajectoryProjection sedProj =
                MetabolicPredictionEngine.calculateTrajectoryProjection(sedentary);
        MetabolicPredictionEngine.GlucoseTrajectoryProjection vigProj =
                MetabolicPredictionEngine.calculateTrajectoryProjection(vigorous);

        assertTrue(vigProj.projectedGlucose120Min().doubleValue() < sedProj.projectedGlucose120Min().doubleValue(),
                "Vigorous physical activity (GLUT4 activation) should result in lower 120m glucose than sedentary state");
    }

    @Test
    @DisplayName("11. Exact milestone numerical verification: v0 = 1.10 mg/dL/min, lambda = 0.010 min^-1")
    void testExactMilestoneNumericalVerification() {
        // Controlled rising test matching documentation: G0 = 125.0, v0 = 1.10 mg/dL/min, T2D (lambda = 0.010)
        MetabolicPredictionEngine.EngineeredFeatures features = createBaseFeatures(
                new BigDecimal("125.0"),
                new BigDecimal("1.10"),
                true, // T2D -> lambda = 0.010
                "MODERATE", // v_activity = 0.0
                BigDecimal.valueOf(0.98),
                Instant.now()
        );

        MetabolicPredictionEngine.GlucoseTrajectoryProjection proj =
                MetabolicPredictionEngine.calculateTrajectoryProjection(features);

        assertNotNull(proj);
        List<MetabolicPredictionEngine.TrajectoryPoint> points = proj.trajectoryPoints();
        assertEquals(5, points.size());

        // Theoretical values: G0 + (1.10 / 0.010) * (1 - exp(-0.010 * t)) = 125.0 + 110.0 * (1 - exp(-0.010 * t))
        // T=0: 125.0
        // T=30: 125.0 + 110.0 * (1 - e^-0.30) = 125.0 + 28.51 = 153.5
        // T=60: 125.0 + 110.0 * (1 - e^-0.60) = 125.0 + 49.63 = 174.6
        // T=90: 125.0 + 110.0 * (1 - e^-0.90) = 125.0 + 65.28 = 190.3
        // T=120: 125.0 + 110.0 * (1 - e^-1.20) = 125.0 + 76.87 = 201.9
        assertEquals(125.0, points.get(0).projectedGlucose().doubleValue(), 0.1);
        assertEquals(153.5, points.get(1).projectedGlucose().doubleValue(), 0.1);
        assertEquals(174.6, points.get(2).projectedGlucose().doubleValue(), 0.1);
        assertEquals(190.3, points.get(3).projectedGlucose().doubleValue(), 0.1);
        assertEquals(201.9, points.get(4).projectedGlucose().doubleValue(), 0.1);
        assertEquals(201.9, proj.projectedGlucose120Min().doubleValue(), 0.1);
        assertEquals(76.9, proj.projectedDeltaMgDl().doubleValue(), 0.1);
        assertEquals("RISING", proj.trajectoryDirection());
    }

    @Test
    @DisplayName("12. Prototype safety floor clamping: raw mathematical negative values bound at 40.0 mg/dL")
    void testPrototypeSafetyFloorClamping() {
        // Rapid recovery: G0 = 120.0, v0 = -1.80 mg/dL/min, T2D (lambda = 0.010)
        // Raw delta at 120 min: (-1.80 / 0.010) * (1 - exp(-1.20)) = -180.0 * 0.6988 = -125.8 mg/dL
        // Raw G(120) = 120.0 - 125.8 = -5.8 mg/dL -> Clamped at configured prototype safety floor 40.0 mg/dL
        MetabolicPredictionEngine.EngineeredFeatures features = createBaseFeatures(
                new BigDecimal("120.0"),
                new BigDecimal("-1.80"),
                true,
                "MODERATE",
                BigDecimal.valueOf(0.98),
                Instant.now()
        );

        MetabolicPredictionEngine.GlucoseTrajectoryProjection proj =
                MetabolicPredictionEngine.calculateTrajectoryProjection(features);

        assertNotNull(proj);
        assertEquals(40.0, proj.projectedGlucose120Min().doubleValue(), 0.01,
                "Projected glucose must clamp at configured prototype safety floor 40.0 mg/dL");
        assertEquals(-80.0, proj.projectedDeltaMgDl().doubleValue(), 0.01,
                "Projected delta must reflect clamp from 120.0 to 40.0 mg/dL (-80.0 mg/dL)");
        assertEquals("FALLING", proj.trajectoryDirection());
    }

    @Test
    @DisplayName("13. Activity velocity modifier: Formulation A combines v0 + v_activity before exponential decay")
    void testActivityVelocityModifierFormulationA() {
        // Baseline: G0 = 150.0, v0 = 1.00 mg/dL/min, non-diabetic (lambda = 0.015)
        // Vigorous: v_activity = -0.12 mg/dL/min, lambda = max(0.015, 0.022) = 0.022 min^-1
        // Effective velocity: v_eff = 1.00 - 0.12 = 0.88 mg/dL/min
        // Delta at 60 min: (0.88 / 0.022) * (1 - exp(-0.022 * 60)) = 40.0 * (1 - exp(-1.32)) = 40.0 * 0.73286 = 29.31 mg/dL
        // Expected G(60) = 150.0 + 29.3 = 179.3 mg/dL
        MetabolicPredictionEngine.EngineeredFeatures vigorousFeatures = createBaseFeatures(
                new BigDecimal("150.0"),
                new BigDecimal("1.00"),
                false,
                "VIGOROUS",
                BigDecimal.valueOf(0.98),
                Instant.now()
        );

        MetabolicPredictionEngine.GlucoseTrajectoryProjection proj =
                MetabolicPredictionEngine.calculateTrajectoryProjection(vigorousFeatures);

        assertNotNull(proj);
        List<MetabolicPredictionEngine.TrajectoryPoint> points = proj.trajectoryPoints();
        assertEquals(179.3, points.get(2).projectedGlucose().doubleValue(), 0.2,
                "Projected glucose at T=60 min must match Formulation A with v_activity = -0.12 and lambda = 0.022");
    }
}
