package org.os4all.modules.digitaltwin.telemetry.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TelemetryIngestRequest(
        @NotNull(message = "patientId is required")
        UUID patientId,

        Instant timestamp,

        BigDecimal glucose, // validated 20.0 to 600.0 mg/dL

        BigDecimal glucoseVelocity, // calculated if null

        BigDecimal heartRate, // validated 30.0 to 250.0 bpm

        BigDecimal hrv, // validated 0.0 to 300.0 ms

        BigDecimal restingHeartRate, // validated 30.0 to 200.0 bpm

        BigDecimal sleepDurationHours, // validated 0.0 to 24.0 hours

        BigDecimal sleepQualityScore, // validated 0.0 to 100.0

        Integer steps, // validated 0 to 100,000

        String activityLevel, // SEDENTARY, LIGHT, MODERATE, VIGOROUS

        String source, // e.g. "SIMULATED_CGM_WEARABLE"

        String scenario // STABLE, POOR_SLEEP, HIGH_ACTIVITY, GLUCOSE_RISE, RECOVERY
) {}
