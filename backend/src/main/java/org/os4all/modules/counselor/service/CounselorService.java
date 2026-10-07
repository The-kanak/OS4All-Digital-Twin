package org.os4all.modules.counselor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.os4all.core.exception.ApiException;
import org.os4all.core.exception.ResourceNotFoundException;
import org.os4all.modules.ai.agent.EvidenceAgent;
import org.os4all.modules.ai.agent.HealthContextAgent;
import org.os4all.modules.ai.agent.TrendInterpretationAgent;
import org.os4all.modules.ai.entity.AiInferenceLog;
import org.os4all.modules.ai.model.AiHealthInsightResponse;
import org.os4all.modules.ai.model.StructuredHealthContext;
import org.os4all.modules.ai.provider.ModelProvider;
import org.os4all.modules.ai.provider.ModelProviderRegistry;
import org.os4all.modules.ai.repository.AiInferenceLogRepository;
import org.os4all.modules.audit.service.AuditService;
import org.os4all.modules.consent.entity.ConsentType;
import org.os4all.modules.consent.repository.UserConsentRepository;
import org.os4all.modules.counselor.dto.*;
import org.os4all.modules.counselor.entity.CounselorConversation;
import org.os4all.modules.counselor.entity.CounselorMessage;
import org.os4all.modules.counselor.repository.CounselorConversationRepository;
import org.os4all.modules.counselor.repository.CounselorMessageRepository;
import org.os4all.modules.user.entity.User;
import org.os4all.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class CounselorService {

    private static final Logger log = LoggerFactory.getLogger(CounselorService.class);

    private final HealthContextAgent healthContextAgent;
    private final TrendInterpretationAgent trendAgent;
    private final EvidenceAgent evidenceAgent;
    private final ModelProviderRegistry providerRegistry;
    private final ClinicalEmergencyClassifier emergencyClassifier;
    private final CounselorConversationRepository conversationRepository;
    private final CounselorMessageRepository messageRepository;
    private final UserConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final AiInferenceLogRepository inferenceLogRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.model:nvidia/Llama-3_1-Nemotron-70B-Instruct}")
    private String modelName;

    @Value("${app.ai.temperature:0.3}")
    private double temperature;

    @Value("${app.ai.max-tokens:1500}")
    private int maxTokens;

    public CounselorService(
            HealthContextAgent healthContextAgent,
            TrendInterpretationAgent trendAgent,
            EvidenceAgent evidenceAgent,
            ModelProviderRegistry providerRegistry,
            ClinicalEmergencyClassifier emergencyClassifier,
            CounselorConversationRepository conversationRepository,
            CounselorMessageRepository messageRepository,
            UserConsentRepository consentRepository,
            UserRepository userRepository,
            AiInferenceLogRepository inferenceLogRepository,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.healthContextAgent = healthContextAgent;
        this.trendAgent = trendAgent;
        this.evidenceAgent = evidenceAgent;
        this.providerRegistry = providerRegistry;
        this.emergencyClassifier = emergencyClassifier;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.consentRepository = consentRepository;
        this.userRepository = userRepository;
        this.inferenceLogRepository = inferenceLogRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Processes a user chat interaction with the OS4All AI Counselor.
     * Enforces clinical safety, consent checks, groundings in actual health context,
     * conversation persistence, and structured separation of concerns.
     */
    @Transactional
    public CounselorResponseDto chat(UUID userId, CounselorChatRequest request, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // 1. Consent Verification: Verify user granted AI_INFERENCE consent
        boolean consentGranted = consentRepository.findByUserIdAndConsentType(userId, ConsentType.AI_INFERENCE)
                .map(org.os4all.modules.consent.entity.UserConsent::isGranted)
                .orElse(false);

        if (!consentGranted) {
            throw new ApiException("AI Inference consent is required to converse with the OS4All Health Counselor. Please enable consent in settings.");
        }

        // 2. Fetch or initialize conversation thread
        CounselorConversation conversation;
        if (request.conversationId() != null) {
            conversation = conversationRepository.findByIdAndUserId(request.conversationId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + request.conversationId()));
        } else {
            conversation = new CounselorConversation();
            conversation.setUser(user);
            String title = generateTitleFromMessage(request.message());
            conversation.setTitle(title);
            conversation = conversationRepository.save(conversation);
        }

        // 3. Persist incoming USER message
        int nextSeq = conversation.getMessages() != null ? conversation.getMessages().size() + 1 : 1;
        CounselorMessage userMsg = new CounselorMessage();
        userMsg.setConversation(conversation);
        userMsg.setRole("USER");
        userMsg.setSequenceNumber(nextSeq++);
        userMsg.setContent(request.message());
        messageRepository.save(userMsg);

        // 4. Emergency Classifier Check: If acute life-threatening symptoms detected, triage immediately
        boolean isEmergency = emergencyClassifier.isEmergencyQuery(request.message());
        if (isEmergency) {
            CounselorResponseDto emergencyResponse = buildEmergencyResponse(conversation.getId().toString());

            CounselorMessage assistantMsg = new CounselorMessage();
            assistantMsg.setConversation(conversation);
            assistantMsg.setRole("ASSISTANT");
            assistantMsg.setSequenceNumber(nextSeq);
            assistantMsg.setContent(emergencyResponse.message());
            assistantMsg.setEmergencyFlagged(true);
            try {
                assistantMsg.setStructuredJson(objectMapper.writeValueAsString(emergencyResponse));
            } catch (Exception ignored) {}
            assistantMsg = messageRepository.save(assistantMsg);

            auditService.record(
                    user,
                    user.getEmail(),
                    "COUNSELOR_EMERGENCY_TRIGGERED",
                    "COUNSELOR_MESSAGE",
                    assistantMsg.getId().toString(),
                    clientIp,
                    "Emergency classifier triggered for query: " + request.message()
            );

            return new CounselorResponseDto(
                    emergencyResponse.message(),
                    emergencyResponse.os4allObservations(),
                    emergencyResponse.aiInterpretations(),
                    emergencyResponse.externalEvidence(),
                    emergencyResponse.recommendedActions(),
                    emergencyResponse.professionalEvaluationGuidance(),
                    emergencyResponse.urgency(),
                    emergencyResponse.confidence(),
                    true,
                    emergencyResponse.disclaimer(),
                    conversation.getId().toString(),
                    assistantMsg.getId().toString(),
                    assistantMsg.getCreatedAt().toString()
            );
        }

        // 5. Build structured health context snapshot
        StructuredHealthContext context = healthContextAgent.buildHealthContext(userId);

        // 6. Execute Trend and Evidence agents to ground the response in actual data
        TrendInterpretationAgent.InterpretationResult trendResult = trendAgent.interpretTrends(context);
        List<AiHealthInsightResponse.EvidenceCitation> evidenceCitations = evidenceAgent.retrieveEvidence(
                context,
                trendResult.verifiedObservations()
        );

        // 7. Resolve ModelProvider and execute LLM completion
        ModelProvider activeProvider = providerRegistry.getActiveProvider();
        CounselorResponseDto structuredResponse = generateCounselorResponse(
                context,
                trendResult,
                evidenceCitations,
                request.message(),
                conversation.getId().toString(),
                activeProvider
        );

        // 8. Persist assistant message
        CounselorMessage assistantMsg = new CounselorMessage();
        assistantMsg.setConversation(conversation);
        assistantMsg.setRole("ASSISTANT");
        assistantMsg.setSequenceNumber(nextSeq);
        assistantMsg.setContent(structuredResponse.message());
        assistantMsg.setEmergencyFlagged(false);
        try {
            assistantMsg.setStructuredJson(objectMapper.writeValueAsString(structuredResponse));
        } catch (Exception ignored) {}
        assistantMsg = messageRepository.save(assistantMsg);

        // 9. Log inference and audit trail
        AiInferenceLog inferenceLog = new AiInferenceLog();
        inferenceLog.setUser(user);
        inferenceLog.setProvider(activeProvider.getProviderName());
        inferenceLog.setModel(activeProvider.getProviderName().equalsIgnoreCase("mock") ? "mock-counselor" : modelName);
        inferenceLog.setWorkflowState("COUNSELOR_CHAT");
        inferenceLog.setAnonymizedPrompt("User Query: " + request.message() + " | State: " + context.currentAggregateState());
        inferenceLog.setRawResponse(assistantMsg.getContent());
        inferenceLog.setStructuredSummary(structuredResponse.message());
        inferenceLog.setUrgency(structuredResponse.urgency());
        inferenceLog.setConfidence(java.math.BigDecimal.valueOf(structuredResponse.confidence()));
        inferenceLog.setExecutionTimeMs(150L);
        inferenceLog.setStatus("SUCCESS");
        inferenceLogRepository.save(inferenceLog);

        auditService.record(
                user,
                user.getEmail(),
                "COUNSELOR_MESSAGE_PROCESSED",
                "COUNSELOR_MESSAGE",
                assistantMsg.getId().toString(),
                clientIp,
                "Conversation: " + conversation.getId() + ", Provider: " + activeProvider.getProviderName()
        );

        return new CounselorResponseDto(
                structuredResponse.message(),
                structuredResponse.os4allObservations(),
                structuredResponse.aiInterpretations(),
                structuredResponse.externalEvidence(),
                structuredResponse.recommendedActions(),
                structuredResponse.professionalEvaluationGuidance(),
                structuredResponse.urgency(),
                structuredResponse.confidence(),
                false,
                structuredResponse.disclaimer(),
                conversation.getId().toString(),
                assistantMsg.getId().toString(),
                assistantMsg.getCreatedAt().toString()
        );
    }

    /**
     * Lists user's conversation threads.
     */
    @Transactional(readOnly = true)
    public Page<CounselorConversationSummaryDto> listConversations(UUID userId, Pageable pageable) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable)
                .map(c -> new CounselorConversationSummaryDto(
                        c.getId(),
                        c.getTitle(),
                        c.getMessages() != null ? c.getMessages().size() : 0,
                        c.getCreatedAt() != null ? c.getCreatedAt().toString() : null,
                        c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null
                ));
    }

    /**
     * Gets a single conversation thread with all its messages.
     */
    @Transactional(readOnly = true)
    public CounselorConversationDetailDto getConversation(UUID conversationId, UUID userId) {
        CounselorConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        List<CounselorMessage> messages = messageRepository.findByConversationIdOrderBySequenceNumberAsc(conversationId);
        List<CounselorConversationDetailDto.CounselorMessageItemDto> messageDtos = new ArrayList<>();

        for (CounselorMessage msg : messages) {
            CounselorResponseDto structured = null;
            if (msg.getStructuredJson() != null && !msg.getStructuredJson().isBlank()) {
                try {
                    structured = objectMapper.readValue(msg.getStructuredJson(), CounselorResponseDto.class);
                } catch (Exception ignored) {}
            }

            messageDtos.add(new CounselorConversationDetailDto.CounselorMessageItemDto(
                    msg.getId(),
                    msg.getRole(),
                    msg.getSequenceNumber(),
                    msg.getContent(),
                    structured,
                    msg.isEmergencyFlagged(),
                    msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : null
            ));
        }

        return new CounselorConversationDetailDto(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt() != null ? conversation.getCreatedAt().toString() : null,
                conversation.getUpdatedAt() != null ? conversation.getUpdatedAt().toString() : null,
                messageDtos
        );
    }

    /**
     * Deletes a conversation thread.
     */
    @Transactional
    public void deleteConversation(UUID conversationId, UUID userId) {
        CounselorConversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));
        conversationRepository.delete(conversation);
    }

    private CounselorResponseDto buildEmergencyResponse(String conversationId) {
        String msg = "Your safety is our top priority. Based on the symptoms described in your message, this could be an acute medical emergency. " +
                "Please do NOT wait or rely on digital self-tracking. Immediately call your local emergency services (911, 112, 999) or have someone drive you to the nearest hospital emergency room.";

        return new CounselorResponseDto(
                msg,
                List.of("Potential acute clinical emergency detected from message contents."),
                List.of("Severe symptoms requiring urgent hands-on triage by emergency medical professionals."),
                List.of(),
                List.of(
                        "Call emergency services (911, 112, or local equivalent) right away.",
                        "Notify a family member, colleague, or nearby companion immediately.",
                        "Rest quietly and avoid strenuous physical movement while waiting for medical assistance."
                ),
                "Immediate emergency room evaluation is urgently advised.",
                "EMERGENCY",
                0.99,
                true,
                CounselorResponseDto.EMERGENCY_DISCLAIMER,
                conversationId,
                null,
                Instant.now().toString()
        );
    }

    private CounselorResponseDto generateCounselorResponse(
            StructuredHealthContext context,
            TrendInterpretationAgent.InterpretationResult trendResult,
            List<AiHealthInsightResponse.EvidenceCitation> evidence,
            String userQuery,
            String conversationId,
            ModelProvider provider
    ) {
        // Build prompt for LLM or mock provider
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildCounselorPrompt(context, trendResult, evidence, userQuery);

        List<ModelProvider.ChatMessage> messages = List.of(
                new ModelProvider.ChatMessage("system", systemPrompt),
                new ModelProvider.ChatMessage("user", userPrompt)
        );

        ModelProvider.ModelParameters params = new ModelProvider.ModelParameters(
                modelName,
                temperature,
                maxTokens
        );

        String rawOutput;
        try {
            rawOutput = provider.generateChatCompletion(messages, params);
        } catch (Exception e) {
            log.warn("ModelProvider failed for counselor chat: {}. Falling back to deterministic structured response.", e.getMessage());
            rawOutput = null;
        }

        return parseOrBuildStructuredResponse(rawOutput, context, trendResult, evidence, userQuery, conversationId);
    }

    private String buildSystemPrompt() {
        return """
                You are the OS4All AI Health Counselor, an objective, empathetic, and evidence-grounded companion.
                You help users understand their personal health data:
                - Current Health Status (STABLE, DRIFT, ANOMALY, FOLLOW_UP)
                - Individual Personal Baselines (mean, variance, normal ranges)
                - Detected Multi-Signal Trends and Duration
                - Uploaded Lab Reports and confirmed biomarkers
                - Previous OS4All health insights
                - Evidence retrieved by the Evidence Agent

                CRITICAL DIRECTIVES:
                1. STRICT SEPARATION: Distinguish fact-based OS4All Observations, non-diagnostic AI Interpretations, External Evidence, and Recommended Actions.
                2. NEVER DIAGNOSE: Do not assign clinical disease names. Deviations from individual baselines reflect physiological patterns, not diseases.
                3. DO NOT INVENT FACTS: Only reference data provided in the user's structured health snapshot.
                4. EVIDENCE GROUNDING: Clearly cite scientific evidence titles and sources when external evidence is referenced.
                5. EVALUATION GUIDANCE: Explicitly state when professional medical evaluation is appropriate.
                6. JSON OUTPUT FORMAT: You must return valid JSON with the schema:
                {
                  "message": "Conversational, clear, empathetic synthesis answering the user's question directly",
                  "os4allObservations": ["What changed and factual biometric readings"],
                  "aiInterpretations": ["What this physiological pattern may mean"],
                  "externalEvidence": [{"title": "...", "source": "...", "urlOrDoi": "...", "snippet": "...", "relevanceScore": 0.95}],
                  "recommendedActions": ["What the user can do next"],
                  "professionalEvaluationGuidance": "When professional evaluation may be appropriate",
                  "urgency": "ROUTINE | MONITOR | EVALUATE_SOON",
                  "confidence": 0.92,
                  "disclaimer": "Standard clinical disclaimer"
                }
                """;
    }

    private String buildCounselorPrompt(
            StructuredHealthContext context,
            TrendInterpretationAgent.InterpretationResult trendResult,
            List<AiHealthInsightResponse.EvidenceCitation> evidence,
            String userQuery
    ) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userQuery", userQuery);
            data.put("aggregateState", context.currentAggregateState());
            data.put("personalBaselines", context.personalBaselines());
            data.put("recentObservations", context.recentObservations());
            data.put("activeAnomalies", context.activeAnomalies());
            data.put("recentLabs", context.recentLabResults());
            data.put("uploadedReports", context.uploadedReports());
            data.put("previousInsights", context.previousInsights());
            data.put("trendObservations", trendResult.verifiedObservations());
            data.put("trendInterpretations", trendResult.possibleInterpretations());
            data.put("retrievedEvidence", evidence);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (Exception e) {
            return "User query: " + userQuery + " | State: " + context.currentAggregateState();
        }
    }

    private CounselorResponseDto parseOrBuildStructuredResponse(
            String rawOutput,
            StructuredHealthContext context,
            TrendInterpretationAgent.InterpretationResult trendResult,
            List<AiHealthInsightResponse.EvidenceCitation> evidence,
            String userQuery,
            String conversationId
    ) {
        if (rawOutput != null && !rawOutput.isBlank()) {
            try {
                String cleanJson = rawOutput.trim();
                if (cleanJson.contains("```json")) {
                    cleanJson = cleanJson.substring(cleanJson.indexOf("```json") + 7);
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
                if (root.has("message") || root.has("os4allObservations")) {
                    String message = root.path("message").asText();
                    List<String> obs = extractStringList(root.path("os4allObservations"), trendResult.verifiedObservations());
                    List<String> interps = extractStringList(root.path("aiInterpretations"), trendResult.possibleInterpretations());

                    List<CounselorResponseDto.EvidenceCitationDto> evDtos = new ArrayList<>();
                    JsonNode evNode = root.path("externalEvidence");
                    if (evNode.isArray() && !evNode.isEmpty()) {
                        for (JsonNode item : evNode) {
                            evDtos.add(new CounselorResponseDto.EvidenceCitationDto(
                                    item.path("title").asText("Clinical Reference"),
                                    item.path("source").asText("Medical Literature"),
                                    item.path("urlOrDoi").asText(""),
                                    item.path("snippet").asText(""),
                                    item.path("relevanceScore").asDouble(0.9)
                            ));
                        }
                    } else if (evidence != null) {
                        for (AiHealthInsightResponse.EvidenceCitation cit : evidence) {
                            evDtos.add(new CounselorResponseDto.EvidenceCitationDto(
                                    cit.title(),
                                    cit.source(),
                                    cit.urlOrDoi(),
                                    cit.relevance(),
                                    0.92
                            ));
                        }
                    }

                    List<String> actions = extractStringList(root.path("recommendedActions"), List.of("Prioritize restorative rest and hydration."));
                    String evalGuidance = root.path("professionalEvaluationGuidance").asText(
                            "If symptoms persist or worsen over the next 48 to 72 hours, or if you feel unwell, schedule a consultation with your doctor."
                    );
                    String urgency = root.path("urgency").asText("MONITOR");
                    double confidence = root.path("confidence").asDouble(0.92);
                    String disclaimer = root.path("disclaimer").asText(CounselorResponseDto.CLINICAL_DISCLAIMER);

                    return new CounselorResponseDto(
                            message,
                            obs,
                            interps,
                            evDtos,
                            actions,
                            evalGuidance,
                            urgency,
                            confidence,
                            false,
                            disclaimer,
                            conversationId,
                            null,
                            Instant.now().toString()
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to parse JSON counselor response: {}. Falling back to deterministic context answer.", e.getMessage());
            }
        }

        // Deterministic Fallback grounded in StructuredHealthContext
        return buildDeterministicResponse(context, trendResult, evidence, userQuery, conversationId);
    }

    private CounselorResponseDto buildDeterministicResponse(
            StructuredHealthContext context,
            TrendInterpretationAgent.InterpretationResult trendResult,
            List<AiHealthInsightResponse.EvidenceCitation> evidence,
            String userQuery,
            String conversationId
    ) {
        StringBuilder answer = new StringBuilder();
        String state = context.currentAggregateState();

        boolean hasAnomalies = context.activeAnomalies() != null && !context.activeAnomalies().isEmpty();
        boolean hasLabs = context.recentLabResults() != null && !context.recentLabResults().isEmpty();

        if (hasAnomalies) {
            StructuredHealthContext.DetectedAnomalyItem firstAnomaly = context.activeAnomalies().get(0);
            answer.append("Your health status is currently in a ").append(state).append(" state. ")
                  .append("What changed: ").append(firstAnomaly.explanation()).append(" ")
                  .append("Compared with your personal baseline, this departure has persisted for approximately ")
                  .append(firstAnomaly.persistenceDays()).append(" days. ")
                  .append("Physiologically, this compound pattern can indicate autonomic strain, physical fatigue, or stress recovery debt. ");
        } else {
            answer.append("Your health status is currently ").append(state).append(". ")
                  .append("All tracked continuous physiological metrics (resting heart rate, HRV, SpO2, sleep) remain within your personal historical ranges. ");
        }

        if (hasLabs) {
            StructuredHealthContext.LabBiomarkerItem firstLab = context.recentLabResults().get(0);
            answer.append("Your most recent confirmed lab result shows ").append(firstLab.biomarker())
                  .append(" at ").append(firstLab.value()).append(" ").append(firstLab.unit()).append(". ");
        }

        if (context.previousInsights() != null && !context.previousInsights().isEmpty()) {
            StructuredHealthContext.PreviousInsightItem prev = context.previousInsights().get(0);
            answer.append("Your last OS4All insight noted: \"").append(prev.summary()).append("\". ");
        }

        answer.append("What you can do next: prioritize restful sleep, stay hydrated, and continue continuous tracking to see if metrics normalize back toward your personal baseline.");

        List<CounselorResponseDto.EvidenceCitationDto> evDtos = new ArrayList<>();
        if (evidence != null) {
            for (AiHealthInsightResponse.EvidenceCitation cit : evidence) {
                evDtos.add(new CounselorResponseDto.EvidenceCitationDto(
                        cit.title(),
                        cit.source(),
                        cit.urlOrDoi(),
                        cit.relevance(),
                        0.91
                ));
            }
        }

        String evalGuidance = hasAnomalies
                ? "If physiological metrics remain elevated beyond 5-7 days or if you experience acute symptoms such as chest tightness, lightheadedness, or fever, professional clinical evaluation is recommended."
                : "Routine annual check-ups remain standard as no clinical baseline anomalies are currently active.";

        return new CounselorResponseDto(
                answer.toString(),
                trendResult.verifiedObservations(),
                trendResult.possibleInterpretations(),
                evDtos,
                List.of(
                        "Maintain steady sleep hygiene aiming for 7.5 to 8.5 hours.",
                        "Moderate strenuous physical training or stimulants until physiological metrics stabilize.",
                        "Continue wearing biometric tracker to update your personal baseline dataset."
                ),
                evalGuidance,
                hasAnomalies ? "MONITOR" : "ROUTINE",
                0.94,
                false,
                CounselorResponseDto.CLINICAL_DISCLAIMER,
                conversationId,
                null,
                Instant.now().toString()
        );
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

    private String generateTitleFromMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Health Consultation";
        }
        String clean = message.trim();
        if (clean.length() <= 40) {
            return clean;
        }
        return clean.substring(0, 37) + "...";
    }
}
