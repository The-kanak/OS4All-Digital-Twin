package org.os4all.modules.digitaltwin.fhir.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FhirParsedPatient(
        UUID id,
        String fhirId,
        String sourceFile,
        String fullName,
        String email,
        String gender,
        LocalDate birthDate,
        Integer age,
        BigDecimal heightCm,
        BigDecimal weightKg,
        BigDecimal bmi,
        BigDecimal bloodPressureSystolic,
        BigDecimal bloodPressureDiastolic,
        boolean hasType2Diabetes,
        boolean hasPrediabetes,
        boolean hasHypertension,
        List<FhirParsedCondition> conditions,
        List<FhirParsedLab> labs,
        List<String> medications,
        List<FhirParsedEncounter> encounters
) {
    public record FhirParsedCondition(
            String code,
            String name,
            String clinicalStatus,
            LocalDate onsetDate
    ) {}

    public record FhirParsedLab(
            String code,
            String name,
            BigDecimal value,
            String unit,
            String dateIso,
            BigDecimal refLow,
            BigDecimal refHigh
    ) {}

    public record FhirParsedEncounter(
            String type,
            String dateIso
    ) {}
}
