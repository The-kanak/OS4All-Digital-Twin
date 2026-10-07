package org.os4all.modules.counselor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Structured response from the OS4All AI Health Counselor")
public record CounselorResponseDto(
        @Schema(description = "Overall conversational text message")
        String message,

        @Schema(description = "Factual, verified health observations detected from individual data")
        List<String> os4allObservations,

        @Schema(description = "Grounded, non-diagnostic AI interpretations and physiological hypotheses")
        List<String> aiInterpretations,

        @Schema(description = "External scientific and medical literature citations retrieved by the Evidence Agent")
        List<EvidenceCitationDto> externalEvidence,

        @Schema(description = "Actionable, non-invasive lifestyle and health actions")
        List<String> recommendedActions,

        @Schema(description = "Clinical evaluation criteria: when to seek medical evaluation")
        String professionalEvaluationGuidance,

        @Schema(description = "Urgency tier: ROUTINE, MONITOR, EVALUATE_SOON, EMERGENCY")
        String urgency,

        @Schema(description = "Confidence score between 0.0 and 1.0")
        Double confidence,

        @Schema(description = "Whether the inquiry triggered clinical safety emergency protocols")
        boolean emergencyFlagged,

        @Schema(description = "Mandatory clinical disclaimer")
        String disclaimer,

        @Schema(description = "Conversation ID")
        String conversationId,

        @Schema(description = "Message ID")
        String messageId,

        @Schema(description = "Message timestamp in ISO-8601")
        String timestamp
) {
    public record EvidenceCitationDto(
            String title,
            String source,
            String urlOrDoi,
            String snippet,
            Double relevanceScore
    ) {}

    public static final String CLINICAL_DISCLAIMER =
            "OS4All AI insights and Counselor conversations are informational health interpretations grounded in individual baselines " +
            "and physiological signals. They DO NOT constitute a medical diagnosis, clinical treatment plan, or doctor-patient relationship. " +
            "Always consult a qualified healthcare provider for clinical medical concerns.";

    public static final String EMERGENCY_DISCLAIMER =
            "EMERGENCY ADVISORY: The symptoms or query described may require immediate medical attention. " +
            "If you or someone nearby is experiencing acute symptoms such as severe chest pressure, sudden numbness/paralysis, " +
            "difficulty breathing, sudden loss of vision, or suicidal distress, please contact emergency services (e.g., 911, 112, 999) " +
            "or proceed to the nearest emergency department immediately. Do not rely on AI analysis for acute medical emergencies.";
}
