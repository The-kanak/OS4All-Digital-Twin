package org.os4all.modules.digitaltwin.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PredictionResultDto(
        UUID id,
        UUID patientId,
        String patientName,
        String predictionType,
        String horizonWindow,
        String riskLevel,
        BigDecimal probability,
        String modelName,
        String headline,
        String clinicalExplanation,
        List<ContributingFactorDto> contributingFactors,
        String historicalEvidence,
        List<String> recommendedClinicalActions,
        BigDecimal confidence,
        boolean isSimulation,
        String disclaimer,
        String createdAtIso,
        BigDecimal currentGlucose,
        BigDecimal glucoseVelocity,
        BigDecimal projectedGlucose120Min,
        BigDecimal projectedDelta,
        String trajectoryDirection,
        List<TrajectoryPointDto> trajectoryPoints
) {
    public record ContributingFactorDto(
            String factorName,
            BigDecimal weight,
            String description,
            String direction
    ) {}

    public record TrajectoryPointDto(
            int minuteOffset,
            BigDecimal projectedGlucose,
            String trendDirection
    ) {}

    // Backward-compatible constructor (17 params)
    public PredictionResultDto(
            UUID id,
            UUID patientId,
            String patientName,
            String predictionType,
            String horizonWindow,
            String riskLevel,
            BigDecimal probability,
            String modelName,
            String headline,
            String clinicalExplanation,
            List<ContributingFactorDto> contributingFactors,
            String historicalEvidence,
            List<String> recommendedClinicalActions,
            BigDecimal confidence,
            boolean isSimulation,
            String disclaimer,
            String createdAtIso
    ) {
        this(id, patientId, patientName, predictionType, horizonWindow, riskLevel, probability,
                modelName, headline, clinicalExplanation, contributingFactors, historicalEvidence,
                recommendedClinicalActions, confidence, isSimulation, disclaimer, createdAtIso,
                null, null, null, null, "STABLE", List.of());
    }
}
