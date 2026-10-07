package org.os4all.modules.digitaltwin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os4all.modules.digitaltwin.engine.MetabolicPredictionEngine;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Metabolic Prediction Engine & Digital Twin Logic Tests")
class MetabolicPredictionEngineTest {

    @Test
    @DisplayName("Stable features produce LOW risk and STABLE twin state")
    void testStablePrediction() {
        MetabolicPredictionEngine.EngineeredFeatures stableFeatures = new MetabolicPredictionEngine.EngineeredFeatures(
                new BigDecimal("92.0"),
                new BigDecimal("0.5"),   // flat slope
                new BigDecimal("16.0"),  // normal CV
                new BigDecimal("2.5"),   // normal HRV
                0.2,
                new BigDecimal("0.8"),   // normal RHR
                0.1,
                new BigDecimal("-0.2"),  // good sleep
                new BigDecimal("5.0"),   // active
                new BigDecimal("23.4"),  // normal BMI
                new BigDecimal("5.2"),   // normal HbA1c
                new BigDecimal("88.0"),  // normal fasting
                10,
                false
        );

        MetabolicPredictionEngine.PredictionOutput result = MetabolicPredictionEngine.predict(stableFeatures);

        assertNotNull(result);
        assertEquals("STABLE", result.twinState());
        assertEquals("LOW", result.riskLevel());
        assertTrue(result.spikeProbability().doubleValue() < 35.0);
        assertTrue(result.contributions().stream().anyMatch(c -> c.impactDirection().equals("PROTECTIVE")));
    }

    @Test
    @DisplayName("Poor sleep, suppressed HRV, and upward glucose slope trigger ELEVATED_RISK / ACTIVE_ANOMALY")
    void testElevatedSpikePrediction() {
        MetabolicPredictionEngine.EngineeredFeatures driftFeatures = new MetabolicPredictionEngine.EngineeredFeatures(
                new BigDecimal("148.0"),
                new BigDecimal("22.0"),  // rapid upward slope (+22 mg/dL/hr)
                new BigDecimal("42.0"),  // high variability
                new BigDecimal("-26.0"), // severe HRV drop (-26%)
                -2.4,
                new BigDecimal("14.5"),  // +14.5 bpm RHR elevation
                2.6,
                new BigDecimal("2.8"),   // 2.8 hrs sleep deficit
                new BigDecimal("-45.0"), // sedentary
                new BigDecimal("28.2"),
                new BigDecimal("5.9"),   // prediabetic HbA1c
                new BigDecimal("104.0"),
                14,
                true                     // post-meal window
        );

        MetabolicPredictionEngine.PredictionOutput result = MetabolicPredictionEngine.predict(driftFeatures);

        assertNotNull(result);
        assertTrue(result.spikeProbability().doubleValue() >= 65.0, "Spike probability should be high under compound stress");
        assertTrue(result.twinState().equals("ELEVATED_RISK") || result.twinState().equals("ACTIVE_ANOMALY"));
        assertTrue(result.stateDrivers().size() >= 3);
        assertTrue(result.contributions().stream().anyMatch(c -> c.featureName().contains("Sleep")));
        assertTrue(result.contributions().stream().anyMatch(c -> c.featureName().contains("HRV")));
    }
}
