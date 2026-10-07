package org.os4all.modules.ai.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.os4all.core.exception.ApiException;
import org.os4all.modules.ai.model.AiHealthInsightResponse;
import org.os4all.modules.ai.model.StructuredHealthContext;
import org.os4all.modules.ai.provider.NebiusModelProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * End-to-end integration test for the sequential Agent Workflow with Nebius AI Studio & NVIDIA Nemotron:
 * Health Context
 *   → Trend Interpretation Agent
 *   → Evidence Agent (when needed)
 *   → Explanation Agent
 *   → Action Agent
 *   → NebiusModelProvider (NVIDIA Nemotron model: nvidia/Llama-3_1-Nemotron-70B-Instruct)
 */
@SpringBootTest
@ActiveProfiles("test")
class AgentWorkflowNebiusIntegrationTest {

    private static final String CONFIGURED_NEMOTRON_MODEL = "nvidia/Llama-3_1-Nemotron-70B-Instruct";
    private static final String NEBIUS_ENDPOINT = "https://api.studio.nebius.ai/v1";

    @Autowired
    private AgentWorkflow agentWorkflow;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private HttpResponse mockHttpResponse;

    private NebiusModelProvider nebiusProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Instantiate Nebius provider with test key and mocked HTTP client
        nebiusProvider = new NebiusModelProvider(
                NEBIUS_ENDPOINT,
                "nebius-test-key-sk-demo-12345",
                30000L,
                objectMapper,
                mockHttpClient
        );
    }

    @Test
    @DisplayName("Complete Workflow: Synthetic health context -> Trend -> Evidence -> Explanation -> Action -> Nebius Nemotron -> Structured Response")
    void shouldExecuteCompleteWorkflowWithNebiusNemotron() throws Exception {
        // 1. Build deterministic synthetic OS4All health scenario
        StructuredHealthContext syntheticScenario = buildDeterministicHealthScenario();

        // 2. Prepare Nemotron structured response payload
        String nemotronResponseJson = """
                {
                  "id": "chatcmpl-nemotron-sync-001",
                  "object": "chat.completion",
                  "created": 1759654800,
                  "model": "nvidia/Llama-3_1-Nemotron-70B-Instruct",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "{\\"summary\\": \\"Persistent autonomic strain pattern identified across 4 days (elevated resting heart rate and depressed nocturnal HRV relative to personal baseline).\\", \\"observations\\": [\\"Resting heart rate increased to 74 bpm (+14 bpm above personal baseline of 60 bpm)\\", \\"Nocturnal HRV decreased to 36 ms (below personal baseline of 55 ms)\\", \\"Sleep duration averaged 5.2 hours across the last 4 days\\"], \\"possibleInterpretations\\": [\\"Compounded autonomic recovery deficit secondary to cumulative sleep restriction and elevated sympathetic drive.\\"], \\"evidence\\": [{\\"title\\": \\"Heart rate variability as a marker of autonomic recovery and physical stress\\", \\"source\\": \\"ncbi.nlm.nih.gov\\", \\"urlOrDoi\\": \\"https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5900352/\\", \\"relevance\\": \\"Short-term sleep restriction significantly suppresses high-frequency HRV and elevates morning resting heart rate.\\"}], \\"recommendedActions\\": [\\"Prioritize 8+ hours of recovery sleep over the next 48 hours.\\", \\"Temporarily reduce intense cardiovascular training load until HRV returns to baseline.\\"], \\"urgency\\": \\"MONITOR\\", \\"confidence\\": 0.94, \\"disclaimer\\": \\"OS4All insights are non-diagnostic and do not constitute a medical diagnosis. Consult a physician for persistent health concerns.\\"}"
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 420,
                    "completion_tokens": 195,
                    "total_tokens": 615
                  }
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(nemotronResponseJson);
        when(mockHttpClient.send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);

        // 3. Execute workflow
        long startTime = System.currentTimeMillis();
        AgentWorkflow.WorkflowExecutionResult result = agentWorkflow.executeWorkflow(syntheticScenario, nebiusProvider);
        long latencyMs = System.currentTimeMillis() - startTime;

        // 4. Verify request reached Nebius
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient, times(1)).send(requestCaptor.capture(), any());
        HttpRequest sentRequest = requestCaptor.getValue();

        assertEquals("POST", sentRequest.method());
        assertEquals("https://api.studio.nebius.ai/v1/chat/completions", sentRequest.uri().toString());
        assertTrue(sentRequest.headers().firstValue("Authorization").orElse("").startsWith("Bearer nebius-test-key-sk-demo-"));
        assertEquals("application/json", sentRequest.headers().firstValue("Content-Type").orElse(""));

        // 5. Verify Nemotron response was received and parsed correctly
        AiHealthInsightResponse insight = result.insight();
        assertNotNull(insight);
        assertNotNull(insight.summary());
        assertTrue(insight.summary().contains("Persistent autonomic strain pattern"));
        assertEquals("MONITOR", insight.urgency());
        assertTrue(insight.confidence().compareTo(BigDecimal.valueOf(0.90)) >= 0);

        // Verify observations
        assertFalse(insight.observations().isEmpty());
        assertTrue(insight.observations().stream().anyMatch(o -> o.contains("Resting heart rate") || o.contains("74")));

        // Verify interpretations
        assertFalse(insight.possibleInterpretations().isEmpty());
        assertTrue(insight.possibleInterpretations().stream().anyMatch(i -> i.contains("autonomic")));

        // Verify evidence
        assertFalse(insight.evidence().isEmpty());
        assertEquals("Heart rate variability as a marker of autonomic recovery and physical stress", insight.evidence().get(0).title());

        // Verify recommended actions and recommended next steps
        assertFalse(insight.recommendedActions().isEmpty());
        assertTrue(insight.recommendedActions().stream().anyMatch(a -> a.contains("sleep") || a.contains("recovery")));
        assertFalse(insight.recommendedNextSteps().isEmpty());

        // Verify traceability: workflow ID, timestamp, model, provider, context version, evidence used, processing status
        assertNotNull(insight.workflowId());
        assertTrue(insight.workflowId().startsWith("WF-"));
        assertNotNull(insight.timestamp());
        assertEquals("nebius", insight.provider());
        assertEquals(CONFIGURED_NEMOTRON_MODEL, insight.model());
        assertEquals("v1.0-deterministic", insight.inputContextVersion());
        assertFalse(insight.evidenceUsed().isEmpty());
        assertEquals("COMPLETED", insight.processingStatus());

        // Verify disclaimer
        assertNotNull(insight.disclaimer());
        assertTrue(insight.disclaimer().contains("non-diagnostic"));

        // Verify execution time and latency tracking
        assertTrue(result.executionTimeMs() >= 0);
        assertTrue(latencyMs >= 0);
    }

    @Test
    @DisplayName("Deterministic Health Context Builder: strictly scopes measurements, baselines, deviations, and symptoms")
    void shouldScopeHealthContextDeterministically() {
        StructuredHealthContext context = buildDeterministicHealthScenario();

        // 1. Current measurements present
        assertFalse(context.currentMeasurements().isEmpty());
        assertTrue(context.currentMeasurements().stream().anyMatch(m -> m.metric().contains("HEART_RATE") && m.value().intValue() == 74));

        // 2. Personal baselines present
        assertFalse(context.personalBaselines().isEmpty());
        assertTrue(context.personalBaselines().containsKey("Resting Heart Rate"));

        // 3. Deviations present with direction and z-scores
        assertFalse(context.deviations().isEmpty());
        StructuredHealthContext.SignalDeviationItem rhrDev = context.deviations().stream()
                .filter(d -> d.metric().contains("Resting Heart Rate"))
                .findFirst().orElseThrow();
        assertEquals("INCREASE", rhrDev.trendDirection());
        assertTrue(rhrDev.deviation().intValue() > 0);

        // 4. Anomaly state and duration
        assertEquals("ANOMALY", context.anomalyState());
        assertEquals(4, context.durationDays());

        // 5. Context version and workflow ID
        assertEquals("v1.0-deterministic", context.contextVersion());
        assertTrue(context.workflowId().startsWith("WF-"));

        // 6. User Identifier is an anonymized pseudonym (no raw PII)
        assertEquals("ANON_ALEX_MORGAN", context.userIdentifier());
    }

    @Test
    @DisplayName("Security: AiInferenceLog must NOT expose raw sensitive prompts in frontend serialization")
    void shouldNotExposeRawPromptInFrontendSerialization() throws Exception {
        org.os4all.modules.ai.entity.AiInferenceLog logEntity = new org.os4all.modules.ai.entity.AiInferenceLog();
        logEntity.setWorkflowId("WF-SEC-001");
        logEntity.setProvider("nebius");
        logEntity.setModel("nvidia/Llama-3_1-Nemotron-70B-Instruct");
        logEntity.setWorkflowState("ANOMALY");
        logEntity.setAnonymizedPrompt("INTERNAL_SENSITIVE_PROMPT_WITH_SYSTEM_INSTRUCTIONS");
        logEntity.setRawResponse("{\"raw\": \"model_response\"}");
        logEntity.setStructuredSummary("Autonomic strain detected.");
        logEntity.setUrgency("MONITOR");
        logEntity.setConfidence(BigDecimal.valueOf(0.94));
        logEntity.setInputContextVersion("v1.0-deterministic");
        logEntity.setEvidenceUsed("Journal of Sleep Research");

        String serializedJson = objectMapper.writeValueAsString(logEntity);

        // Verify anonymizedPrompt and rawResponse are excluded (@JsonIgnore)
        assertFalse(serializedJson.contains("INTERNAL_SENSITIVE_PROMPT_WITH_SYSTEM_INSTRUCTIONS"));
        assertFalse(serializedJson.contains("rawResponse"));
        assertFalse(serializedJson.contains("anonymizedPrompt"));

        // Verify traceable workflow metadata IS present
        assertTrue(serializedJson.contains("WF-SEC-001"));
        assertTrue(serializedJson.contains("v1.0-deterministic"));
        assertTrue(serializedJson.contains("Autonomic strain detected."));
    }

    @Test
    @DisplayName("If configured Nemotron model is unavailable (HTTP 404), reports exact error and stops without substituting")
    void shouldReportExactErrorAndStopWhenModelUnavailable() throws Exception {
        StructuredHealthContext syntheticScenario = buildDeterministicHealthScenario();

        // Simulate Nebius returning HTTP 404 when model is not available
        String errorPayload = """
                {
                  "error": {
                    "message": "The model 'nvidia/Llama-3_1-Nemotron-70B-Instruct' is not found or has been deprecated on Nebius Token Factory",
                    "type": "invalid_request_error",
                    "code": "model_not_found"
                  }
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(404);
        when(mockHttpResponse.body()).thenReturn(errorPayload);
        when(mockHttpClient.send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);

        // Verify AgentWorkflow does NOT swallow the error or fall back to mock
        ApiException exception = assertThrows(ApiException.class, () ->
                agentWorkflow.executeWorkflow(syntheticScenario, nebiusProvider)
        );

        assertTrue(exception.getMessage().contains("is unavailable or not found (HTTP 404)"));
        assertTrue(exception.getMessage().contains(CONFIGURED_NEMOTRON_MODEL));
        assertTrue(exception.getMessage().contains("not found or has been deprecated"));
    }

    @Test
    @DisplayName("If Nebius authentication fails (HTTP 401), reports exact error and stops without substituting")
    void shouldReportExactErrorAndStopWhenAuthenticationFails() throws Exception {
        StructuredHealthContext syntheticScenario = buildDeterministicHealthScenario();

        String errorPayload = """
                {
                  "detail": "Couldn't authenticate. Reason: token is invalid or expired"
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(401);
        when(mockHttpResponse.body()).thenReturn(errorPayload);
        when(mockHttpClient.send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);

        ApiException exception = assertThrows(ApiException.class, () ->
                agentWorkflow.executeWorkflow(syntheticScenario, nebiusProvider)
        );

        assertTrue(exception.getMessage().contains("authentication failed (HTTP 401)"));
        assertTrue(exception.getMessage().contains("token is invalid or expired"));
    }

    /**
     * Builds the deterministic synthetic OS4All 35-day health perturbation scenario:
     * - Days 1-30: Homeostatic baseline (Resting HR 60 bpm, HRV 55 ms, Sleep 8.0h, Temp 36.6°C)
     * - Days 31-35: Controlled departure / perturbation (Sleep 5.2h, Resting HR 74 bpm, HRV 36 ms, Temp 37.2°C)
     */
    private StructuredHealthContext buildDeterministicHealthScenario() {
        Map<String, StructuredHealthContext.BaselineContextItem> baselines = Map.of(
                "Resting Heart Rate", new StructuredHealthContext.BaselineContextItem(
                        "Resting Heart Rate", BigDecimal.valueOf(60.0), BigDecimal.valueOf(2.5),
                        BigDecimal.valueOf(57.5), BigDecimal.valueOf(62.5), 30, true
                ),
                "HRV", new StructuredHealthContext.BaselineContextItem(
                        "HRV", BigDecimal.valueOf(55.0), BigDecimal.valueOf(3.8),
                        BigDecimal.valueOf(51.2), BigDecimal.valueOf(58.8), 30, true
                ),
                "Sleep Duration", new StructuredHealthContext.BaselineContextItem(
                        "Sleep Duration", BigDecimal.valueOf(8.0), BigDecimal.valueOf(0.5),
                        BigDecimal.valueOf(7.5), BigDecimal.valueOf(8.5), 30, true
                )
        );

        List<StructuredHealthContext.RecentObservationItem> recentObservations = List.of(
                new StructuredHealthContext.RecentObservationItem("HEART_RATE", BigDecimal.valueOf(74.0), "bpm", Instant.now().toString(), BigDecimal.valueOf(14.0)),
                new StructuredHealthContext.RecentObservationItem("HRV", BigDecimal.valueOf(36.0), "ms", Instant.now().toString(), BigDecimal.valueOf(-19.0)),
                new StructuredHealthContext.RecentObservationItem("SLEEP", BigDecimal.valueOf(5.2), "hours", Instant.now().toString(), BigDecimal.valueOf(-2.8))
        );

        List<StructuredHealthContext.DetectedAnomalyItem> activeAnomalies = List.of(
                new StructuredHealthContext.DetectedAnomalyItem(
                        "MULTI_SIGNAL",
                        "ANOMALY",
                        "Resting HR increase (+14 bpm) coupled with nocturnal HRV decrease (-19 ms) and restricted sleep",
                        BigDecimal.valueOf(4),
                        BigDecimal.valueOf(0.95),
                        true
                )
        );

        return new StructuredHealthContext(
                "ANON_ALEX_MORGAN",
                "MALE",
                34,
                baselines,
                recentObservations,
                activeAnomalies,
                List.of(),
                "ANOMALY",
                List.of(),
                List.of()
        );
    }
}
