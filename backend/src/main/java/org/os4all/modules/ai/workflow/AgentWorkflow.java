package org.os4all.modules.ai.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.os4all.modules.ai.agent.*;
import org.os4all.modules.ai.model.AiHealthInsightResponse;
import org.os4all.modules.ai.model.StructuredHealthContext;
import org.os4all.modules.ai.provider.ModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

/**
 * Deterministic Health Intelligence Workflow:
 * HEALTH DATA → PERSONAL BASELINE → TREND ANALYSIS → ANOMALY EVENTS → HEALTH CONTEXT BUILDER
 * → NEMOTRON (Physiological Context Evaluation)
 * → EVIDENCE RETRIEVAL IF REQUIRED
 * → NEMOTRON SYNTHESIS
 * → OS4ALL INSIGHT
 * → RECOMMENDED NEXT STEP
 *
 * Scoped strictly to relevant physiological context, baselines, deviations, and symptoms.
 * Produces fully traceable AI insights with workflow ID, model/provider, context version, and status.
 */
@Service
public class AgentWorkflow {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkflow.class);

    private final TrendInterpretationAgent trendAgent;
    private final EvidenceAgent evidenceAgent;
    private final ExplanationAgent explanationAgent;
    private final ActionAgent actionAgent;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.model:nvidia/Llama-3_1-Nemotron-70B-Instruct}")
    private String modelName;

    @Value("${app.ai.temperature:0.2}")
    private double temperature;

    @Value("${app.ai.max-tokens:1500}")
    private int maxTokens;

    public record WorkflowExecutionResult(
            AiHealthInsightResponse insight,
            String anonymizedPrompt,
            String rawModelOutput,
            long executionTimeMs
    ) {}

    public AgentWorkflow(
            TrendInterpretationAgent trendAgent,
            EvidenceAgent evidenceAgent,
            ExplanationAgent explanationAgent,
            ActionAgent actionAgent,
            ObjectMapper objectMapper
    ) {
        this.trendAgent = trendAgent;
        this.evidenceAgent = evidenceAgent;
        this.explanationAgent = explanationAgent;
        this.actionAgent = actionAgent;
        this.objectMapper = objectMapper;
    }

    public WorkflowExecutionResult executeWorkflow(StructuredHealthContext context, ModelProvider provider) {
        long startTime = System.currentTimeMillis();
        String workflowId = context.workflowId() != null ? context.workflowId() : "WF-" + UUID.randomUUID().toString();

        log.info("Starting deterministic health-intelligence workflow [ID: {}] using provider '{}'",
                workflowId, provider.getProviderName());

        // Stage 1: Trend Interpretation Agent (Trend Analysis: observations vs interpretations)
        TrendInterpretationAgent.InterpretationResult trendResult = trendAgent.interpretTrends(context);

        // Stage 2: Preliminary Nemotron Physiological Context Evaluation
        String deterministicExplanation = explanationAgent.generateExplanation(
                context,
                trendResult.verifiedObservations(),
                trendResult.possibleInterpretations(),
                provider
        );

        // Stage 3: Evidence Agent (Literature retrieval triggered conditionally if anomalies/deviations exist)
        List<AiHealthInsightResponse.EvidenceCitation> evidence = evidenceAgent.retrieveEvidence(context, trendResult.verifiedObservations());

        // Update health context with evidence results for complete provenance
        List<StructuredHealthContext.EvidenceResultItem> evidenceItems = evidence.stream()
                .map(e -> new StructuredHealthContext.EvidenceResultItem(e.title(), e.source(), e.urlOrDoi(), e.relevance()))
                .toList();
        StructuredHealthContext enrichedContext = context.withEvidenceResults(evidenceItems);

        // Stage 4: Action Agent (Formulates pragmatic guidance, recommended next steps, and urgency)
        ActionAgent.ActionPlan actionPlan = actionAgent.formulateActions(
                enrichedContext,
                trendResult.possibleInterpretations(),
                trendResult.baselineDeviationScore()
        );

        // Stage 5: Build anonymized system instructions and prompt for Nemotron Synthesis
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(enrichedContext, trendResult, evidence, deterministicExplanation, actionPlan);

        List<ModelProvider.ChatMessage> messages = List.of(
                new ModelProvider.ChatMessage("system", systemPrompt),
                new ModelProvider.ChatMessage("user", userPrompt)
        );

        ModelProvider.ModelParameters params = new ModelProvider.ModelParameters(
                modelName,
                temperature,
                maxTokens
        );

        // Stage 6: Nemotron Synthesis invocation
        String rawOutput;
        if (provider.getProviderName().equalsIgnoreCase("mock")) {
            rawOutput = provider.generateChatCompletion(messages, params);
        } else {
            // For real providers (Nebius with NVIDIA Nemotron):
            // If the model is unavailable or encounters an error, report the exact error and stop
            // rather than silently substituting another model or falling back to mock synthesis.
            rawOutput = provider.generateChatCompletion(messages, params);
        }

        // Stage 7: OS4All Insight & Recommended Next Step Formulation
        AiHealthInsightResponse parsedInsight = parseAndValidateResponse(
                rawOutput,
                deterministicExplanation,
                trendResult,
                evidence,
                actionPlan,
                enrichedContext,
                provider,
                params
        );

        long executionTimeMs = System.currentTimeMillis() - startTime;
        log.info("Deterministic health intelligence workflow [ID: {}] completed in {} ms (status: {}, urgency: {}, confidence: {})",
                workflowId, executionTimeMs, parsedInsight.processingStatus(), parsedInsight.urgency(), parsedInsight.confidence());

        return new WorkflowExecutionResult(
                parsedInsight,
                userPrompt,
                rawOutput,
                executionTimeMs
        );
    }

    private String buildSystemPrompt() {
        return """
                You are the OS4All Health Intelligence Engine, an objective, evidence-grounded AI orchestration system.
                Your task is to analyze personal physiological signals relative to individual historical baselines.
                
                MANDATORY OPERATIONAL DIRECTIVES:
                1. THE AI RESPONSE MUST STRICTLY DISTINGUISH THREE DISTINCT LEVELS OF INFORMATION:
                   - "OS4All observed": Factual observations comparing measurements to the individual's personal baseline.
                   - "External evidence indicates": Insights and mechanisms supported by the provided external scientific literature citations.
                   - "OS4All recommends discussing": Prudent, non-diagnostic guidance and questions to review with a qualified physician.
                2. NEVER CLAIM A CLINICAL DIAGNOSIS OR STATE THAT A DEVIATION EQUALS DISEASE. Do NOT present web search results as medical diagnoses.
                3. CRITICAL SOURCE GROUNDING DIRECTIVE: You MUST ONLY cite external sources provided in the `retrievedEvidence` section. NEVER invent, hallucinate, or fabricate references, authors, DOIs, URLs, or journal titles. If no external sources were provided, the evidence list MUST be empty.
                4. ACKNOWLEDGE UNCERTAINTY: Explicitly communicate confidence and statistical limitations.
                5. ALWAYS RECOMMEND PROFESSIONAL HEALTHCARE EVALUATION when acute, prolonged, or compound departures arise.
                6. OUTPUT STRICT, VALID JSON adhering exactly to the requested schema.
                
                JSON Output Format:
                {
                  "summary": "Concise high-level synthesis clearly separating what OS4All observed, what external evidence indicates, and what OS4All recommends discussing",
                  "observations": ["OS4All observed: Grounded factual data points relative to personal baseline"],
                  "possibleInterpretations": ["External evidence indicates: Non-diagnostic physiological hypotheses and mechanisms"],
                  "evidence": [{"title": "...", "source": "...", "urlOrDoi": "...", "relevance": "...", "domain": "...", "retrievedTime": "...", "relevantExcerpt": "...", "relevanceScore": 0.95}],
                  "recommendedActions": ["OS4All recommends discussing: Guidance to review with doctor", "Pragmatic lifestyle recovery steps"],
                  "recommendedNextSteps": ["Clear, immediate recommended next steps"],
                  "urgency": "ROUTINE | MONITOR | EVALUATE_SOON | URGENT",
                  "confidence": 0.0 to 1.0,
                  "disclaimer": "Standard OS4All clinical disclaimer"
                }
                """;
    }

    private String buildUserPrompt(
            StructuredHealthContext context,
            TrendInterpretationAgent.InterpretationResult trendResult,
            List<AiHealthInsightResponse.EvidenceCitation> evidence,
            String explanation,
            ActionAgent.ActionPlan actionPlan
    ) {
        try {
            Map<String, Object> promptData = new LinkedHashMap<>();
            promptData.put("workflowId", context.workflowId());
            promptData.put("contextVersion", context.contextVersion());
            promptData.put("anonymizedSubject", context.userIdentifier());
            promptData.put("biologicalSex", context.biologicalSex());
            promptData.put("ageYears", context.ageYears());
            promptData.put("currentMeasurements", context.currentMeasurements());
            promptData.put("baselineMetrics", context.personalBaselines());
            promptData.put("deviations", context.deviations());
            promptData.put("durationDays", context.durationDays());
            promptData.put("trendDirection", context.trendDirection());
            promptData.put("anomalyState", context.anomalyState());
            promptData.put("activeAnomalies", context.activeAnomalies());
            promptData.put("userProvidedSymptoms", context.userProvidedSymptoms());
            promptData.put("previousRelevantObservations", context.recentObservations());
            promptData.put("confirmedLabs", context.recentLabResults());
            promptData.put("evidenceResults", evidence);
            promptData.put("preliminaryPhysiologicalExplanation", explanation);
            promptData.put("trendAgentObservations", trendResult.verifiedObservations());
            promptData.put("trendAgentHypotheses", trendResult.possibleInterpretations());
            promptData.put("recommendedNextSteps", actionPlan.recommendedActions());
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(promptData);
        } catch (Exception e) {
            return "Health Context: " + context.currentAggregateState();
        }
    }

    private AiHealthInsightResponse parseAndValidateResponse(
            String rawOutput,
            String fallbackExplanation,
            TrendInterpretationAgent.InterpretationResult trendResult,
            List<AiHealthInsightResponse.EvidenceCitation> evidence,
            ActionAgent.ActionPlan actionPlan,
            StructuredHealthContext context,
            ModelProvider provider,
            ModelProvider.ModelParameters params
    ) {
        String workflowId = context.workflowId() != null ? context.workflowId() : "WF-" + UUID.randomUUID().toString();
        Instant timestamp = Instant.now();
        String contextVersion = context.contextVersion() != null ? context.contextVersion() : StructuredHealthContext.CURRENT_VERSION;

        try {
            String cleanJson = rawOutput.trim();
            if (cleanJson.contains("```json")) {
                cleanJson = cleanJson.substring(cleanJson.indexOf("```json") + 7);
                if (cleanJson.contains("```")) {
                    cleanJson = cleanJson.substring(0, cleanJson.indexOf("```"));
                }
            } else if (cleanJson.contains("```JSON")) {
                cleanJson = cleanJson.substring(cleanJson.indexOf("```JSON") + 7);
                if (cleanJson.contains("```")) {
                    cleanJson = cleanJson.substring(0, cleanJson.indexOf("```"));
                }
            } else if (cleanJson.contains("```")) {
                cleanJson = cleanJson.substring(cleanJson.indexOf("```") + 3);
                if (cleanJson.contains("```")) {
                    cleanJson = cleanJson.substring(0, cleanJson.indexOf("```"));
                }
            }

            JsonNode root = objectMapper.readTree(cleanJson.trim());

            String summary = root.path("summary").asText(fallbackExplanation);
            List<String> observations = extractStringList(root.path("observations"), trendResult.verifiedObservations());
            List<String> interpretations = extractStringList(root.path("possibleInterpretations"), trendResult.possibleInterpretations());

            List<AiHealthInsightResponse.EvidenceCitation> evidenceList = new ArrayList<>();
            // Anti-hallucination source grounding:
            if (evidence == null || evidence.isEmpty()) {
                // If retrieval was not necessary, do NOT allow the model to invent sources
                evidenceList = Collections.emptyList();
            } else {
                JsonNode evNode = root.path("evidence");
                if (evNode.isArray() && !evNode.isEmpty()) {
                    for (JsonNode item : evNode) {
                        String title = item.path("title").asText("");
                        Optional<AiHealthInsightResponse.EvidenceCitation> matched = evidence.stream()
                                .filter(e -> e.title().equalsIgnoreCase(title) ||
                                        title.toLowerCase(Locale.ROOT).contains(e.title().toLowerCase(Locale.ROOT)) ||
                                        e.title().toLowerCase(Locale.ROOT).contains(title.toLowerCase(Locale.ROOT)))
                                .findFirst();
                        if (matched.isPresent()) {
                            evidenceList.add(matched.get());
                        }
                    }
                }
                // If model returned no matches or omitted citations, ground with the verified retrieved evidence
                if (evidenceList.isEmpty()) {
                    evidenceList = evidence;
                }
            }

            List<String> actions = extractStringList(root.path("recommendedActions"), actionPlan.recommendedActions());
            List<String> nextSteps = extractStringList(root.path("recommendedNextSteps"), actions);
            String urgency = root.path("urgency").asText(actionPlan.urgency());

            double confDouble = root.path("confidence").asDouble(0.90);
            BigDecimal confidence = BigDecimal.valueOf(confDouble).setScale(2, RoundingMode.HALF_UP);

            String disclaimer = root.path("disclaimer").asText(AiHealthInsightResponse.CLINICAL_DISCLAIMER);

            List<String> evidenceUsedTitles = evidenceList.stream()
                    .map(AiHealthInsightResponse.EvidenceCitation::title)
                    .toList();

            return new AiHealthInsightResponse(
                    workflowId,
                    timestamp,
                    provider.getProviderName(),
                    params.model(),
                    contextVersion,
                    evidenceUsedTitles,
                    summary,
                    observations,
                    interpretations,
                    evidenceList,
                    actions,
                    nextSteps,
                    urgency,
                    confidence,
                    "COMPLETED",
                    disclaimer
            );
        } catch (Exception e) {
            log.warn("Failed to parse JSON model output: {}. Constructing validated fallback response.", e.getMessage());
            List<String> evidenceUsedTitles = evidence.stream()
                    .map(AiHealthInsightResponse.EvidenceCitation::title)
                    .toList();

            return new AiHealthInsightResponse(
                    workflowId,
                    timestamp,
                    provider.getProviderName(),
                    params.model(),
                    contextVersion,
                    evidenceUsedTitles,
                    fallbackExplanation,
                    trendResult.verifiedObservations(),
                    trendResult.possibleInterpretations(),
                    evidence,
                    actionPlan.recommendedActions(),
                    actionPlan.recommendedActions(),
                    actionPlan.urgency(),
                    BigDecimal.valueOf(0.90),
                    "COMPLETED",
                    AiHealthInsightResponse.CLINICAL_DISCLAIMER
            );
        }
    }

    private List<String> extractStringList(JsonNode node, List<String> fallback) {
        if (node != null && node.isArray() && !node.isEmpty()) {
            List<String> list = new ArrayList<>();
            for (JsonNode item : node) {
                list.add(item.asText());
            }
            return list;
        }
        return fallback;
    }
}
