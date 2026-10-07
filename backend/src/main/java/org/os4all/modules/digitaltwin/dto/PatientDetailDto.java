package org.os4all.modules.digitaltwin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PatientDetailDto(
        UUID id,
        String fullName,
        String email,
        LocalDate dateOfBirth,
        Integer age,
        String biologicalSex,
        BigDecimal heightCm,
        BigDecimal weightKg,
        BigDecimal bmi,
        String bloodType,
        String lifestyleNotes,
        String currentTwinState,
        BigDecimal overallRiskScore,
        BigDecimal metabolicRiskScore,
        BigDecimal glucoseSpikeProbability,
        String predictionHorizon,
        List<HistoricalRecordDto> historicalRecords,
        List<LatestLabBiomarkerDto> recentBiomarkers,
        String disclaimer
) {
    public record HistoricalRecordDto(
            UUID id,
            String recordType,
            String conditionOrDiagnosis,
            String icd10Code,
            String severity,
            String status,
            LocalDate diagnosedDate,
            String medications,
            String familyHistoryNotes,
            String clinicalNotes
    ) {}

    public record LatestLabBiomarkerDto(
            String biomarker,
            BigDecimal value,
            String unit,
            BigDecimal referenceLow,
            BigDecimal referenceHigh,
            String collectionDateIso
    ) {}
}
