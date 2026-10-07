package org.os4all.modules.digitaltwin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PatientSummaryDto(
        UUID id,
        String fullName,
        String email,
        Integer age,
        String biologicalSex,
        BigDecimal heightCm,
        BigDecimal weightKg,
        BigDecimal bmi,
        String bloodType,
        String currentTwinState,
        BigDecimal overallRiskScore,
        BigDecimal glucoseSpikeProbability,
        String primaryCondition,
        List<String> keyMedications,
        String lastUpdatedIso
) {}
