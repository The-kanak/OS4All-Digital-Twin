package org.os4all.modules.digitaltwin.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VirtualPatientInteractionDto(
        UUID patientId,
        String question,
        String answer,
        String digitalTwinState,
        BigDecimal overallRiskScore,
        BigDecimal glucoseSpikeProbability,
        List<String> keyContributingFactors,
        List<String> groundedEvidence,
        List<String> recommendedNextSteps,
        String confidence,
        String disclaimer,
        String timestampIso
) {}
