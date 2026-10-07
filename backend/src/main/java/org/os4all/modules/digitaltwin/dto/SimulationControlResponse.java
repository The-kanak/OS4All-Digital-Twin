package org.os4all.modules.digitaltwin.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SimulationControlResponse(
        UUID patientId,
        String scenario,
        String status, // RUNNING, PAUSED, RESET, SCENARIO_INJECTED
        String description,
        String currentTwinState,
        BigDecimal overallRiskScore,
        BigDecimal glucoseSpikeProbability,
        List<String> immediateDrivers,
        DigitalTwinStateDto.CurrentVitalsSnapshot currentVitals,
        Map<String, Object> simulationMetadata,
        String timestampIso
) {}
