package org.os4all.modules.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os4all.core.security.CustomUserDetails;
import org.os4all.core.security.JwtService;
import org.os4all.modules.ai.agent.DefaultEvidenceAgent;
import org.os4all.modules.ai.model.AiHealthInsightResponse;
import org.os4all.modules.ai.model.StructuredHealthContext;
import org.os4all.modules.auth.dto.RegisterRequest;
import org.os4all.modules.auth.service.AuthService;
import org.os4all.modules.consent.dto.ConsentRequest;
import org.os4all.modules.consent.entity.ConsentType;
import org.os4all.modules.consent.service.ConsentService;
import org.os4all.modules.evidence.model.EvidenceQuery;
import org.os4all.modules.evidence.model.EvidenceResult;
import org.os4all.modules.evidence.service.EvidenceQueryBuilder;
import org.os4all.modules.evidence.service.MockEvidenceService;
import org.os4all.modules.evidence.service.TavilyEvidenceService;
import org.os4all.modules.ingestion.dto.CreateObservationRequest;
import org.os4all.modules.ingestion.entity.ObservationType;
import org.os4all.modules.user.entity.User;
import org.os4all.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvidenceAgentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConsentService consentService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private DefaultEvidenceAgent evidenceAgent;

    @Autowired
    private EvidenceQueryBuilder queryBuilder;

    @Autowired
    private MockEvidenceService mockEvidenceService;

    private String token;
    private User testUser;

    @BeforeEach
    void setUp() {
        String testEmail = "evidence_agent_test_" + System.currentTimeMillis() + "@os4all.test";
        authService.register(new RegisterRequest(testEmail, "SecurePass123!", "Evidence Agent Tester"), "127.0.0.1");
        testUser = userRepository.findByEmail(testEmail).orElseThrow();
        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        token = "Bearer " + jwtService.generateToken(userDetails, testUser.getId());
    }

    @Test
    @DisplayName("Evidence necessity gate: Bypasses web searches for routine STABLE health baseline")
    void shouldBypassEvidenceWhenContextIsStable() {
        StructuredHealthContext stableContext = new StructuredHealthContext(
                "WF-TEST-STABLE",
                StructuredHealthContext.CURRENT_VERSION,
                "USER_ANON_1",
                "MALE",
                35,
                "STABLE",
                List.of(
                        new StructuredHealthContext.CurrentMeasurementItem("HEART_RATE", BigDecimal.valueOf(60.0), "bpm", Instant.now().toString())
                ),
                java.util.Map.of(),
                List.of(), // no deviations
                "STABLE",
                0,
                List.of(),
                List.of(), // no anomalies
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        List<String> stableObservations = List.of(
                "Resting heart rate remains within typical baseline bounds."
        );

        assertFalse(evidenceAgent.isEvidenceNecessary(stableContext, stableObservations),
                "Evidence search MUST be skipped when health signals are STABLE.");

        List<AiHealthInsightResponse.EvidenceCitation> citations = evidenceAgent.retrieveEvidence(stableContext, stableObservations);
        assertTrue(citations.isEmpty(), "No external queries should execute for routine stable baseline.");
    }

    @Test
    @DisplayName("Evidence Query Builder: Formulates targeted biomedical query from multi-signal deviation without diagnosing disease")
    void shouldBuildTargetedQueryFromDeviations() {
        StructuredHealthContext anomalousContext = new StructuredHealthContext(
                "WF-TEST-ANOMALY",
                StructuredHealthContext.CURRENT_VERSION,
                "USER_ANON_1",
                "MALE",
                35,
                "MULTI_SIGNAL_ANOMALY",
                List.of(
                        new StructuredHealthContext.CurrentMeasurementItem("HEART_RATE", BigDecimal.valueOf(72.0), "bpm", Instant.now().toString()),
                        new StructuredHealthContext.CurrentMeasurementItem("HRV", BigDecimal.valueOf(36.0), "ms", Instant.now().toString())
                ),
                java.util.Map.of(),
                List.of(
                        new StructuredHealthContext.SignalDeviationItem("HEART_RATE", BigDecimal.valueOf(72.0), BigDecimal.valueOf(60.0), BigDecimal.valueOf(12.0), 2.4, "ELEVATED", 3),
                        new StructuredHealthContext.SignalDeviationItem("HRV", BigDecimal.valueOf(36.0), BigDecimal.valueOf(55.0), BigDecimal.valueOf(-19.0), -2.1, "SUPPRESSED", 3)
                ),
                "ELEVATED",
                3,
                List.of(),
                List.of(
                        new StructuredHealthContext.DetectedAnomalyItem("HEART_RATE", "PERSISTENT", "Elevated resting pulse with suppressed vagal tone", BigDecimal.valueOf(3), BigDecimal.valueOf(0.95), true)
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        List<String> observations = List.of(
                "Resting heart rate elevated +2.4 standard deviations above baseline for 3 consecutive days.",
                "HRV rMSSD suppressed -2.1 standard deviations below personal baseline."
        );

        assertTrue(evidenceAgent.isEvidenceNecessary(anomalousContext, observations));

        EvidenceQuery query = queryBuilder.buildQuery(anomalousContext, observations);
        assertNotNull(query);
        assertNotNull(query.queryText());
        assertFalse(query.queryText().isBlank());
        assertTrue(query.includeMedicalDomainsOnly(), "Must restrict queries strictly to medical domains");

        // Verify query text does not diagnose disease but queries physiological mechanisms
        String queryLower = query.queryText().toLowerCase();
        assertTrue(queryLower.contains("heart rate") || queryLower.contains("hrv") || queryLower.contains("recovery"));
        assertFalse(queryLower.contains("diagnose"), "Must never formulate diagnostic queries");
    }

    @Test
    @DisplayName("Evidence Agent: Preserves all provenance metadata (title, url, domain, retrievedTime, excerpt, score)")
    void shouldPreserveAllEvidenceProvenanceFields() {
        StructuredHealthContext anomalousContext = new StructuredHealthContext(
                "WF-TEST-PROVENANCE",
                StructuredHealthContext.CURRENT_VERSION,
                "USER_ANON_1",
                "MALE",
                35,
                "MULTI_SIGNAL_ANOMALY",
                List.of(
                        new StructuredHealthContext.CurrentMeasurementItem("HEART_RATE", BigDecimal.valueOf(75.0), "bpm", Instant.now().toString())
                ),
                java.util.Map.of(),
                List.of(
                        new StructuredHealthContext.SignalDeviationItem("HEART_RATE", BigDecimal.valueOf(75.0), BigDecimal.valueOf(60.0), BigDecimal.valueOf(15.0), 2.8, "ELEVATED", 4)
                ),
                "ELEVATED",
                4,
                List.of(),
                List.of(
                        new StructuredHealthContext.DetectedAnomalyItem("HEART_RATE", "PERSISTENT", "Elevated pulse", BigDecimal.valueOf(4), BigDecimal.valueOf(0.92), false)
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        List<AiHealthInsightResponse.EvidenceCitation> citations = evidenceAgent.retrieveEvidence(
                anomalousContext,
                List.of("Resting pulse departure +2.8 SD from baseline")
        );

        assertFalse(citations.isEmpty(), "Should retrieve evidence for anomalous context");
        for (AiHealthInsightResponse.EvidenceCitation cit : citations) {
            assertNotNull(cit.title(), "Citation must contain title");
            assertNotNull(cit.urlOrDoi(), "Citation must contain URL or DOI");
            assertNotNull(cit.domain(), "Citation must contain domain");
            assertNotNull(cit.retrievedTime(), "Citation must contain retrieved timestamp");
            assertNotNull(cit.relevantExcerpt(), "Citation must contain excerpt");
            assertTrue(cit.relevanceScore() >= 0.60, "Citation must meet relevance threshold");
        }
    }

    @Test
    @DisplayName("AI Workflow Integration: Produces 3-tier distinction and grounded evidence citations")
    void shouldEnforceThreeTierDistinctionInEndToEndEvaluation() throws Exception {
        // 1. Consent
        ConsentRequest consentReq = new ConsentRequest();
        consentReq.setConsentType(ConsentType.AI_INFERENCE);
        consentReq.setGranted(true);
        consentService.updateConsent(testUser.getId(), consentReq, "127.0.0.1");

        // 2. Ingest multi-signal drift
        Instant now = Instant.now();
        for (int i = 7; i >= 3; i--) {
            ingestObservation(ObservationType.HEART_RATE, "Resting Heart Rate", 60.0, "bpm", now.minus(i, ChronoUnit.DAYS));
            ingestObservation(ObservationType.HRV, "HRV", 55.0, "ms", now.minus(i, ChronoUnit.DAYS));
        }
        for (int i = 2; i >= 1; i--) {
            ingestObservation(ObservationType.HEART_RATE, "Resting Heart Rate", 74.0, "bpm", now.minus(i, ChronoUnit.DAYS));
            ingestObservation(ObservationType.HRV, "HRV", 36.0, "ms", now.minus(i, ChronoUnit.DAYS));
        }

        // 3. Trigger evaluation
        mockMvc.perform(post("/api/v1/ai/insights/evaluate")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.evidence", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.evidence[0].domain", notNullValue()))
                .andExpect(jsonPath("$.data.evidence[0].relevantExcerpt", notNullValue()))
                .andExpect(jsonPath("$.data.evidence[0].relevanceScore", greaterThanOrEqualTo(0.60)))
                .andExpect(jsonPath("$.data.summary", containsString("OS4All observed:")))
                .andExpect(jsonPath("$.data.summary", containsString("External evidence indicates:")))
                .andExpect(jsonPath("$.data.summary", containsString("OS4All recommends discussing:")));
    }

    @Test
    @DisplayName("Tavily service provider availability check without API key")
    void shouldReportUnavailableWhenTavilyKeyMissing() {
        TavilyEvidenceService unconfiguredTavily = new TavilyEvidenceService(
                "https://api.tavily.com/search",
                "", // empty API key
                true,
                0.65,
                10000,
                objectMapper
        );

        assertFalse(unconfiguredTavily.isAvailable(), "Tavily should report unavailable when API key is empty");
        assertEquals("tavily", unconfiguredTavily.getProviderName());
    }

    private void ingestObservation(ObservationType type, String name, double value, String unit, Instant timestamp) throws Exception {
        CreateObservationRequest req = new CreateObservationRequest();
        req.setType(type);
        if (type == ObservationType.HEART_RATE) {
            req.setVitalName(name);
        }
        req.setValueNumeric(BigDecimal.valueOf(value));
        req.setUnit(unit);
        req.setTimestamp(timestamp);
        req.setSource("DEMO DATA: Sensor");

        mockMvc.perform(post("/api/v1/health/observations")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}
