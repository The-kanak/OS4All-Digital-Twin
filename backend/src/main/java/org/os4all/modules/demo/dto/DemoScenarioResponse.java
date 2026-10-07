package org.os4all.modules.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.os4all.modules.ai.model.AiHealthInsightResponse;
import org.os4all.modules.anomaly.model.AnomalyEvent;
import org.os4all.modules.anomaly.model.HealthSignal;
import org.os4all.modules.baseline.model.BaselineMetric;
import org.os4all.modules.timeline.dto.TimelineItemResponse;

import java.util.List;

@Schema(description = "Complete end-to-end OS4All Hackathon Demonstration Result")
public record DemoScenarioResponse(
        @Schema(description = "Demonstration execution status banner")
        String statusMessage,

        @Schema(description = "Synthetic test user identifier")
        String demoUserEmail,

        @Schema(description = "Synthetic test user display name")
        String demoUserName,

        @Schema(description = "Overall aggregate state (e.g. STABLE, DRIFT, ANOMALY)")
        String aggregateState,

        @Schema(description = "Visual explanation of the complete end-to-end chain")
        DemoExecutionChainDto executionChain,

        @Schema(description = "Established individual baseline profile metrics")
        List<BaselineMetric> baselineMetrics,

        @Schema(description = "Evaluated physiological signals comparing recent observations to baseline")
        List<HealthSignal> evaluatedSignals,

        @Schema(description = "Detected anomaly events and multi-signal correlations")
        List<AnomalyEvent> detectedAnomalies,

        @Schema(description = "Flagship Nemotron-orchestrated AI health insight")
        AiHealthInsightResponse aiHealthInsight,

        @Schema(description = "Updated longitudinal timeline items")
        List<TimelineItemResponse> timelineEvents,

        @Schema(description = "Mandatory synthetic demonstration disclaimer")
        String syntheticDataNotice,

        @Schema(description = "Deterministic progression stages (STABLE -> DRIFT -> MULTI-SIGNAL ANOMALY)")
        List<DemoProgressionStageDto> progressionStages,

        @Schema(description = "Honest AI Provider Mode (e.g. 'NVIDIA Nemotron via Nebius' or 'AI DEMO MODE')")
        String aiProviderMode,

        @Schema(description = "Active Model Name")
        String aiModelName,

        @Schema(description = "Honest explanation of AI execution environment")
        String aiProviderNote
) {
    public record DemoExecutionChainDto(
            String step1Data,
            String step2Baseline,
            String step3Drift,
            String step4MultiSignalPattern,
            String step5NemotronReasoning,
            String step6TavilyEvidence,
            String step7Os4AllInsight,
            String step8RecommendedAction,
            String step9TimelineUpdate
    ) {}

    public record DemoProgressionStageDto(
            String stageName,
            String dayRange,
            String status,
            String description,
            List<String> keySignals
    ) {}

    public static final String SYNTHETIC_NOTICE =
            "DEMO DATA NOTICE: DEMO DATA — NOT A REAL PATIENT. All records, vital observations, baselines, and lab results in this scenario are " +
            "strictly synthetic demonstration data generated for the OS4All Hackathon prototype. " +
            "They do NOT represent a real patient and do NOT constitute clinical medical advice or diagnosis.";

    public DemoScenarioResponse(
            String statusMessage,
            String demoUserEmail,
            String aggregateState,
            DemoExecutionChainDto executionChain,
            List<BaselineMetric> baselineMetrics,
            List<HealthSignal> evaluatedSignals,
            List<AnomalyEvent> detectedAnomalies,
            AiHealthInsightResponse aiHealthInsight,
            List<TimelineItemResponse> timelineEvents,
            String syntheticDataNotice
    ) {
        this(statusMessage, demoUserEmail, "Demo User — Synthetic Data", aggregateState, executionChain,
                baselineMetrics, evaluatedSignals, detectedAnomalies, aiHealthInsight, timelineEvents,
                syntheticDataNotice, List.of(), "AI DEMO MODE", "mock-deterministic",
                "Deterministic mock provider calibrated for reproducible offline hackathon evaluation.");
    }
}
