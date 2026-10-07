package org.os4all.modules.digitaltwin.telemetry.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SimulationStatusDto(
        UUID patientId,
        String patientName,
        boolean isRunning,
        String activeScenario,
        int tickCount,
        Instant lastTickAt,
        TelemetryPacketDto latestTelemetry,
        String twinState,
        BigDecimal overallRiskScore,
        BigDecimal glucoseSpikeProbability,
        List<String> stateDrivers,
        String message,
        String disclaimer
) {
    public static final String DEFAULT_DISCLAIMER =
            "SYNTHETIC / SIMULATED TELEMETRY — Research / Hackathon Prototype — Not a Medical Diagnosis.";
}
