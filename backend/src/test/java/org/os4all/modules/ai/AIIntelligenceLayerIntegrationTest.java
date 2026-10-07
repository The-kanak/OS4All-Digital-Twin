package org.os4all.modules.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os4all.core.security.CustomUserDetails;
import org.os4all.core.security.JwtService;
import org.os4all.modules.ai.entity.AiInferenceLog;
import org.os4all.modules.ai.repository.AiInferenceLogRepository;
import org.os4all.modules.auth.dto.RegisterRequest;
import org.os4all.modules.auth.service.AuthService;
import org.os4all.modules.consent.dto.ConsentRequest;
import org.os4all.modules.consent.entity.ConsentType;
import org.os4all.modules.consent.service.ConsentService;
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
class AIIntelligenceLayerIntegrationTest {

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
    private AiInferenceLogRepository inferenceLogRepository;

    @Autowired
    private JwtService jwtService;

    private String token;
    private User testUser;

    @BeforeEach
    void setUp() {
        String testEmail = "ai_test_" + System.currentTimeMillis() + "@os4all.test";
        authService.register(new RegisterRequest(testEmail, "SecurePass123!", "AI Test User"), "127.0.0.1");
        testUser = userRepository.findByEmail(testEmail).orElseThrow();
        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        token = "Bearer " + jwtService.generateToken(userDetails, testUser.getId());
    }

    @Test
    @DisplayName("Evaluate AI insight fails with 400 when AI_INFERENCE consent is not granted")
    void shouldRejectEvaluationWithoutConsent() throws Exception {
        mockMvc.perform(post("/api/v1/ai/insights/evaluate")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("AI Inference consent is required")));
    }

    @Test
    @DisplayName("Evaluate AI insight successfully with Mock Provider and AI_INFERENCE consent")
    void shouldEvaluateAiInsightWithMockProvider() throws Exception {
        // 1. Grant consent for AI_INFERENCE
        ConsentRequest consentReq = new ConsentRequest();
        consentReq.setConsentType(ConsentType.AI_INFERENCE);
        consentReq.setGranted(true);
        consentService.updateConsent(testUser.getId(), consentReq, "127.0.0.1");

        // 2. Perform AI evaluation
        mockMvc.perform(post("/api/v1/ai/insights/evaluate")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.summary", notNullValue()))
                .andExpect(jsonPath("$.data.observations", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.possibleInterpretations", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.evidence", notNullValue()))
                .andExpect(jsonPath("$.data.recommendedActions", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.urgency", isOneOf("ROUTINE", "MONITOR", "EVALUATE_SOON", "URGENT")))
                .andExpect(jsonPath("$.data.confidence", notNullValue()))
                .andExpect(jsonPath("$.data.disclaimer", containsString("DO NOT constitute a medical diagnosis")));

        // 3. Verify AI request/response was logged in tamper-evident audit / inference logs
        List<AiInferenceLog> logs = inferenceLogRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId(), null).getContent();
        assertFalse(logs.isEmpty());
        AiInferenceLog log = logs.get(0);
        assertEquals("mock", log.getProvider());
        assertNotNull(log.getAnonymizedPrompt());
        assertNotNull(log.getRawResponse());
        assertNotNull(log.getStructuredSummary());
        assertEquals("SUCCESS", log.getStatus());

        // 4. Verify GET /api/v1/ai/insights/logs endpoint
        mockMvc.perform(get("/api/v1/ai/insights/logs")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].provider", is("mock")));
    }

    @Test
    @DisplayName("Evaluate AI insight on detected multi-signal autonomic strain pattern")
    void shouldEvaluateMultiSignalStrainPatternWithAppropriateUrgency() throws Exception {
        // 1. Grant AI_INFERENCE consent
        ConsentRequest consentReq = new ConsentRequest();
        consentReq.setConsentType(ConsentType.AI_INFERENCE);
        consentReq.setGranted(true);
        consentService.updateConsent(testUser.getId(), consentReq, "127.0.0.1");

        // 2. Ingest baseline readings (6 days)
        Instant now = Instant.now();
        for (int i = 8; i >= 3; i--) {
            ingestObservation(ObservationType.HEART_RATE, "Resting Heart Rate", 60.0, "bpm", now.minus(i, ChronoUnit.DAYS));
            ingestObservation(ObservationType.HRV, "HRV", 55.0, "ms", now.minus(i, ChronoUnit.DAYS));
            ingestObservation(ObservationType.SLEEP, "Sleep", 8.0, "hours", now.minus(i, ChronoUnit.DAYS));
        }

        // 3. Ingest acute strain pattern for last 2 days: Sleep ↓ (5 hrs), RHR ↑ (72 bpm), HRV ↓ (38 ms)
        for (int i = 2; i >= 1; i--) {
            ingestObservation(ObservationType.HEART_RATE, "Resting Heart Rate", 72.0, "bpm", now.minus(i, ChronoUnit.DAYS));
            ingestObservation(ObservationType.HRV, "HRV", 38.0, "ms", now.minus(i, ChronoUnit.DAYS));
            ingestObservation(ObservationType.SLEEP, "Sleep", 5.0, "hours", now.minus(i, ChronoUnit.DAYS));
        }

        // 4. Trigger AI evaluation
        mockMvc.perform(post("/api/v1/ai/insights/evaluate")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.urgency", is("MONITOR")))
                .andExpect(jsonPath("$.data.summary", containsString("autonomic strain pattern")))
                .andExpect(jsonPath("$.data.possibleInterpretations", hasItem(containsString("autonomic"))))
                .andExpect(jsonPath("$.data.evidence", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.disclaimer", notNullValue()));
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
        req.setSource("DEMO DATA: Biosensor");

        mockMvc.perform(post("/api/v1/health/observations")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}
