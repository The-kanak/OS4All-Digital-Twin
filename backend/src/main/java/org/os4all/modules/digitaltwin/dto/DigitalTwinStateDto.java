package org.os4all.modules.digitaltwin.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DigitalTwinStateDto(
        UUID patientId,
        String patientName,
        String state, // STABLE, PRE_SYMPTOMATIC_DRIFT, ELEVATED_RISK, ACTIVE_ANOMALY
        BigDecimal overallRiskScore,
        BigDecimal metabolicRiskScore,
        BigDecimal glucoseSpikeProbability,
        String predictionHorizon,
        BigDecimal confidence,
        List<String> stateDrivers,
        String physiologicalSummary,
        String activeScenario,
        CurrentVitalsSnapshot vitalsSnapshot,
        List<BaselineDeviationDto> baselineDeviations,
        PredictionSnapshotDto latestPrediction,
        String lastUpdatedIso,
        String disclaimer,
        BigDecimal projectedGlucose120Min,
        BigDecimal glucoseVelocity,
        BigDecimal projectedDelta,
        String trajectoryDirection
) {
    public record CurrentVitalsSnapshot(
            BigDecimal heartRate,
            BigDecimal restingHeartRate,
            BigDecimal hrv,
            BigDecimal spo2,
            BigDecimal glucose,
            BigDecimal sleepHours,
            BigDecimal steps,
            BigDecimal temperature
    ) {}

    public record BaselineDeviationDto(
            String metric,
            BigDecimal baselineMean,
            BigDecimal currentValue,
            BigDecimal deviation,
            BigDecimal percentageDeviation,
            Double zScore,
            String unit,
            String trendDirection,
            String interpretation
    ) {}

    public record PredictionSnapshotDto(
            UUID id,
            String predictionType,
            String horizonWindow,
            String riskLevel,
            BigDecimal probability,
            String headline,
            String clinicalExplanation,
            List<String> topContributingFactors,
            String historicalEvidence,
            List<String> recommendedClinicalActions,
            BigDecimal confidence,
            String disclaimer,
            BigDecimal projectedGlucose120Min,
            BigDecimal glucoseVelocity,
            BigDecimal projectedDelta,
            String trajectoryDirection
    ) {
        public PredictionSnapshotDto(
                UUID id,
                String predictionType,
                String horizonWindow,
                String riskLevel,
                BigDecimal probability,
                String headline,
                String clinicalExplanation,
                List<String> topContributingFactors,
                String historicalEvidence,
                List<String> recommendedClinicalActions,
                BigDecimal confidence,
                String disclaimer
        ) {
            this(id, predictionType, horizonWindow, riskLevel, probability, headline,
                    clinicalExplanation, topContributingFactors, historicalEvidence,
                    recommendedClinicalActions, confidence, disclaimer,
                    null, null, null, "STABLE");
        }
    }

    // Backward-compatible constructor (16 params)
    public DigitalTwinStateDto(
            UUID patientId,
            String patientName,
            String state,
            BigDecimal overallRiskScore,
            BigDecimal metabolicRiskScore,
            BigDecimal glucoseSpikeProbability,
            String predictionHorizon,
            BigDecimal confidence,
            List<String> stateDrivers,
            String physiologicalSummary,
            String activeScenario,
            CurrentVitalsSnapshot vitalsSnapshot,
            List<BaselineDeviationDto> baselineDeviations,
            PredictionSnapshotDto latestPrediction,
            String lastUpdatedIso,
            String disclaimer
    ) {
        this(patientId, patientName, state, overallRiskScore, metabolicRiskScore,
                glucoseSpikeProbability, predictionHorizon, confidence, stateDrivers,
                physiologicalSummary, activeScenario, vitalsSnapshot, baselineDeviations,
                latestPrediction, lastUpdatedIso, disclaimer,
                null, null, null, "STABLE");
    }
}
