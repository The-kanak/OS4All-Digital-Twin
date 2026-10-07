package org.os4all.modules.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os4all.core.security.CustomUserDetails;
import org.os4all.core.security.JwtService;
import org.os4all.modules.auth.dto.RegisterRequest;
import org.os4all.modules.auth.service.AuthService;
import org.os4all.modules.consent.dto.ConsentRequest;
import org.os4all.modules.consent.entity.ConsentType;
import org.os4all.modules.consent.service.ConsentService;
import org.os4all.modules.evidence.model.EvidenceQuery;
import org.os4all.modules.evidence.model.EvidenceResult;
import org.os4all.modules.evidence.service.EvidenceService;
import org.os4all.modules.evidence.service.EvidenceServiceRegistry;
import org.os4all.modules.evidence.service.MockEvidenceService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvidenceRetrievalIntegrationTest {

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
    private EvidenceServiceRegistry evidenceRegistry;

    @Autowired
    private MockEvidenceService mockEvidenceService;

    @Autowired
    private JwtService jwtService;

    private String token;
    private User testUser;

    @BeforeEach
    void setUp() {
        String testEmail = "evidence_test_" + System.currentTimeMillis() + "@os4all.test";
        authService.register(new RegisterRequest(testEmail, "SecurePass123!", "Evidence Test User"), "127.0.0.1");
        testUser = userRepository.findByEmail(testEmail).orElseThrow();
        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        token = "Bearer " + jwtService.generateToken(userDetails, testUser.getId());
    }

    @Test
    @DisplayName("MockEvidenceService retrieves ranked biomedical sources for constrained queries")
    void shouldRetrieveRankedBiomedicalSources() {
        EvidenceQuery query = new EvidenceQuery(
                "heart rate variability and resting heart rate elevation during acute sleep restriction recovery",
                List.of("HRV", "resting heart rate"),
                "Context: State ANOMALY",
                true,
                3
        );

        EvidenceResult result = mockEvidenceService.retrieveEvidence(query);

        assertNotNull(result);
        assertEquals("mock", result.providerName());
        assertFalse(result.sources().isEmpty());
        assertTrue(result.sources().size() <= 3);

        // Verify that sources contain peer-reviewed/gov domains and high relevance scores
        var first = result.sources().get(0);
        assertNotNull(first.title());
        assertNotNull(first.url());
        assertTrue(first.relevanceScore() >= 0.65);
        assertTrue(first.isPeerReviewedOrGov());
    }

    @Test
    @DisplayName("GET /api/v1/evidence/search - returns constrained biomedical literature results")
    void shouldSearchEvidenceViaController() throws Exception {
        mockMvc.perform(get("/api/v1/evidence/search")
                        .param("query", "fasting blood glucose variation clinical reference range")
                        .param("maxResults", "2")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.providerName", is("mock")))
                .andExpect(jsonPath("$.data.sources", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.sources[0].domain", notNullValue()))
                .andExpect(jsonPath("$.data.sources[0].relevanceScore", greaterThanOrEqualTo(0.65)));
    }

    @Test
    @DisplayName("AI Workflow invokes Evidence Retrieval only when anomalies or deviations are present")
    void shouldSelectivelyRetrieveEvidenceInAiWorkflow() throws Exception {
        // 1. Grant AI_INFERENCE consent
        ConsentRequest consentReq = new ConsentRequest();
        consentReq.setConsentType(ConsentType.AI_INFERENCE);
        consentReq.setGranted(true);
        consentService.updateConsent(testUser.getId(), consentReq, "127.0.0.1");

        // 2. Ingest persistent anomaly (6 days baseline + 2 days strain)
        Instant now = Instant.now();
        for (int i = 8; i >= 3; i--) {
            ingestObservation(ObservationType.HEART_RATE, "Resting Heart Rate", 60.0, "bpm", now.minus(i, ChronoUnit.DAYS));
            ingestObservation(ObservationType.HRV, "HRV", 55.0, "ms", now.minus(i, ChronoUnit.DAYS));
            ingestObservation(ObservationType.SLEEP, "Sleep", 8.0, "hours", now.minus(i, ChronoUnit.DAYS));
        }
        for (int i = 2; i >= 1; i--) {
            ingestObservation(ObservationType.HEART_RATE, "Resting Heart Rate", 72.0, "bpm", now.minus(i, ChronoUnit.DAYS));
            ingestObservation(ObservationType.HRV, "HRV", 38.0, "ms", now.minus(i, ChronoUnit.DAYS));
            ingestObservation(ObservationType.SLEEP, "Sleep", 5.0, "hours", now.minus(i, ChronoUnit.DAYS));
        }

        // 3. Trigger AI evaluation - evidence should be actively retrieved and cited
        mockMvc.perform(post("/api/v1/ai/insights/evaluate")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.evidence", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.evidence[0].title", notNullValue()))
                .andExpect(jsonPath("$.data.evidence[0].urlOrDoi", notNullValue()));
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
