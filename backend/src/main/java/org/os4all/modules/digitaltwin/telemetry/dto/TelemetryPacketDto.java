package org.os4all.modules.digitaltwin.telemetry.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TelemetryPacketDto(
        UUID id,
        UUID patientId,
        String patientName,
        Instant timestamp,
        BigDecimal glucose,
        BigDecimal glucoseVelocity,
        BigDecimal heartRate,
        BigDecimal hrv,
        BigDecimal restingHeartRate,
        BigDecimal sleepDurationHours,
        BigDecimal sleepQualityScore,
        Integer steps,
        String activityLevel,
        String source,
        String scenario,
        BigDecimal confidence,
        String disclaimer
) {
    public static final String DEFAULT_DISCLAIMER =
            "SYNTHETIC / SIMULATED TELEMETRY — Research / Hackathon Prototype — Not a Medical Diagnosis.";
}
