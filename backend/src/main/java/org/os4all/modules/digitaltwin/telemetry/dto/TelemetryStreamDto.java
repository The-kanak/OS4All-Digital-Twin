package org.os4all.modules.digitaltwin.telemetry.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TelemetryStreamDto(
        UUID patientId,
        String patientName,
        String metric,
        String unit,
        List<TelemetryPointDto> points,
        BigDecimal baselineMean,
        BigDecimal baselineMin,
        BigDecimal baselineMax,
        TelemetryPacketDto latestTelemetry,
        String predictionHorizon,
        boolean isSimulating,
        String activeScenario,
        String disclaimer
) {
    public record TelemetryPointDto(
            BigDecimal value,
            BigDecimal velocity,
            BigDecimal heartRate,
            BigDecimal hrv,
            BigDecimal restingHeartRate,
            BigDecimal sleepDurationHours,
            Integer steps,
            String activityLevel,
            String timestampIso,
            String source,
            BigDecimal confidence
    ) {}
}
