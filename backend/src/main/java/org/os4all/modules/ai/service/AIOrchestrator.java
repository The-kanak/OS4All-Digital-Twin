package org.os4all.modules.ai.service;

import org.os4all.core.exception.ApiException;
import org.os4all.core.exception.ResourceNotFoundException;
import org.os4all.modules.ai.agent.HealthContextAgent;
import org.os4all.modules.ai.entity.AiInferenceLog;
import org.os4all.modules.ai.model.AiHealthInsightResponse;
import org.os4all.modules.ai.model.StructuredHealthContext;
import org.os4all.modules.ai.provider.ModelProvider;
import org.os4all.modules.ai.provider.ModelProviderRegistry;
import org.os4all.modules.ai.repository.AiInferenceLogRepository;
import org.os4all.modules.ai.workflow.AgentWorkflow;
import org.os4all.modules.audit.service.AuditService;
import org.os4all.modules.consent.entity.ConsentType;
import org.os4all.modules.consent.repository.UserConsentRepository;
import org.os4all.modules.user.entity.User;
import org.os4all.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * High-level AI Orchestration Service.
 * Verifies user consent, enforces read-only access (never modifies health records),
 * orchestrates the AgentWorkflow, sanitizes sensitive data, and logs inferences to tamper-evident audit trails.
 */
@Service
public class AIOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AIOrchestrator.class);

    private final HealthContextAgent healthContextAgent;
    private final AgentWorkflow agentWorkflow;
    private final ModelProviderRegistry providerRegistry;
    private final UserConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final AiInferenceLogRepository inferenceLogRepository;
    private final AuditService auditService;

    @org.springframework.beans.factory.annotation.Value("${app.ai.model:nvidia/Llama-3_1-Nemotron-70B-Instruct}")
    private String modelName;

    public AIOrchestrator(
            HealthContextAgent healthContextAgent,
            AgentWorkflow agentWorkflow,
            ModelProviderRegistry providerRegistry,
            UserConsentRepository consentRepository,
            UserRepository userRepository,
            AiInferenceLogRepository inferenceLogRepository,
            AuditService auditService
    ) {
        this.healthContextAgent = healthContextAgent;
        this.agentWorkflow = agentWorkflow;
        this.providerRegistry = providerRegistry;
        this.consentRepository = consentRepository;
        this.userRepository = userRepository;
        this.inferenceLogRepository = inferenceLogRepository;
        this.auditService = auditService;
    }

    /**
     * Orchestrates an AI health evaluation workflow for the user.
     * Enforces that the LLM is strictly non-destructive and cannot modify records directly.
     */
    @Transactional
    public AiHealthInsightResponse generateHealthInsight(UUID userId, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // 1. Consent Verification: Verify user granted AI_INFERENCE consent
        boolean consentGranted = consentRepository.findByUserIdAndConsentType(userId, ConsentType.AI_INFERENCE)
                .map(org.os4all.modules.consent.entity.UserConsent::isGranted)
                .orElse(false);

        if (!consentGranted) {
            throw new ApiException("AI Inference consent is required before generating automated health insights. Please grant consent in settings.");
        }

        // 2. Health Context Agent: Curate strictly read-only, anonymized health context
        StructuredHealthContext healthContext = healthContextAgent.buildHealthContext(userId);

        // 3. Resolve active model provider (mock, nebius, or configured provider)
        ModelProvider activeProvider = providerRegistry.getActiveProvider();

        // 4. Execute Agent Workflow
        AgentWorkflow.WorkflowExecutionResult result = agentWorkflow.executeWorkflow(healthContext, activeProvider);

        // 5. Audit & Inference Logging with sensitive-data protection
        AiInferenceLog inferenceLog = new AiInferenceLog();
        inferenceLog.setUser(user);
        inferenceLog.setWorkflowId(result.insight().workflowId());
        inferenceLog.setInputContextVersion(result.insight().inputContextVersion());
        inferenceLog.setEvidenceUsed(result.insight().evidenceUsed() != null ? String.join("; ", result.insight().evidenceUsed()) : "");
        inferenceLog.setProvider(activeProvider.getProviderName());
        inferenceLog.setModel(activeProvider.getProviderName().equalsIgnoreCase("mock") ? "mock-engine" : modelName);
        inferenceLog.setWorkflowState(healthContext.currentAggregateState());
        inferenceLog.setAnonymizedPrompt(result.anonymizedPrompt());
        inferenceLog.setRawResponse(result.rawModelOutput());
        inferenceLog.setStructuredSummary(result.insight().summary());
        inferenceLog.setUrgency(result.insight().urgency());
        inferenceLog.setConfidence(result.insight().confidence());
        inferenceLog.setExecutionTimeMs(result.executionTimeMs());
        inferenceLog.setStatus("SUCCESS");
        inferenceLogRepository.save(inferenceLog);

        auditService.record(
                user,
                user.getEmail(),
                "AI_INFERENCE_EXECUTED",
                "AI_INSIGHT",
                inferenceLog.getId().toString(),
                clientIp,
                "WorkflowId: " + result.insight().workflowId() +
                        ", Provider: " + activeProvider.getProviderName() +
                        ", Model: " + (activeProvider.getProviderName().equalsIgnoreCase("mock") ? "mock-engine" : modelName) +
                        ", ContextVersion: " + result.insight().inputContextVersion() +
                        ", Urgency: " + result.insight().urgency() +
                        ", Confidence: " + result.insight().confidence() +
                        ", Latency: " + result.executionTimeMs() + "ms"
        );

        log.info("AI Orchestrator produced health insight for user {} using provider {} in {} ms",
                userId, activeProvider.getProviderName(), result.executionTimeMs());

        return result.insight();
    }

    /**
     * Lists recent AI inference logs for the user with sensitive-data protections.
     */
    @Transactional(readOnly = true)
    public Page<AiInferenceLog> getUserInferenceLogs(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return inferenceLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
