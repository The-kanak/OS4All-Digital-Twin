package org.os4all.modules.ai.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Clean, structured output schema strictly enforced for all AI orchestration responses.
 * Provides complete traceability: workflow ID, timestamp, provider, model, context version,
 * grounded observations, hypotheses, evidence, actionable next steps, and status.
 */
public record AiHealthInsightResponse(
        String workflowId,
        Instant timestamp,
        String provider,
        String model,
        String inputContextVersion,
        List<String> evidenceUsed,
        String summary,
        List<String> observations,
        List<String> possibleInterpretations,
        List<EvidenceCitation> evidence,
        List<String> recommendedActions,
        List<String> recommendedNextSteps,
        String urgency, // "ROUTINE", "MONITOR", "EVALUATE_SOON", "URGENT"
        BigDecimal confidence,
        String processingStatus, // "COMPLETED", "FAILED"
        String disclaimer
) {
    public record EvidenceCitation(
            String title,
            String source,
            String urlOrDoi,
            String relevance,
            String domain,
            String retrievedTime,
            String relevantExcerpt,
            Double relevanceScore
    ) {
        public EvidenceCitation(String title, String source, String urlOrDoi, String relevance) {
            this(
                    title,
                    source,
                    urlOrDoi,
                    relevance,
                    extractDomainFromUrl(urlOrDoi, source),
                    Instant.now().toString(),
                    relevance,
                    0.90
            );
        }

        private static String extractDomainFromUrl(String url, String source) {
            if (url != null && url.startsWith("http")) {
                try {
                    String host = java.net.URI.create(url).getHost();
                    if (host != null && !host.isBlank()) return host;
                } catch (Exception ignored) {}
            }
            return source != null ? source : "ncbi.nlm.nih.gov";
        }
    }

    public static final String CLINICAL_DISCLAIMER =
            "OS4All AI insights are informational health interpretations based strictly on personal historical baselines " +
            "and physiological signals. They DO NOT constitute a medical diagnosis, clinical treatment plan, or doctor-patient relationship. " +
            "Always consult a qualified healthcare professional regarding medical symptoms or clinical decisions.";

    /**
     * Backward-compatible 8-parameter constructor.
     */
    public AiHealthInsightResponse(
            String summary,
            List<String> observations,
            List<String> possibleInterpretations,
            List<EvidenceCitation> evidence,
            List<String> recommendedActions,
            String urgency,
            BigDecimal confidence,
            String disclaimer
    ) {
        this(
                "WF-" + UUID.randomUUID().toString().substring(0, 8),
                Instant.now(),
                "nebius",
                "nvidia/Llama-3_1-Nemotron-70B-Instruct",
                "v1.0-deterministic",
                evidence != null ? evidence.stream().map(EvidenceCitation::title).toList() : List.of(),
                summary,
                observations != null ? observations : List.of(),
                possibleInterpretations != null ? possibleInterpretations : List.of(),
                evidence != null ? evidence : List.of(),
                recommendedActions != null ? recommendedActions : List.of(),
                recommendedActions != null ? recommendedActions : List.of(),
                urgency != null ? urgency : "ROUTINE",
                confidence != null ? confidence : BigDecimal.valueOf(0.90),
                "COMPLETED",
                disclaimer != null ? disclaimer : CLINICAL_DISCLAIMER
        );
    }
}
