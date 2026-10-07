package org.os4all.modules.anomaly;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os4all.core.security.CustomUserDetails;
import org.os4all.core.security.JwtService;
import org.os4all.modules.auth.dto.RegisterRequest;
import org.os4all.modules.auth.service.AuthService;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthAnomalyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String token;
    private User testUser;

    @BeforeEach
    void setUp() {
        String testEmail = "anomaly_user_" + System.currentTimeMillis() + "@os4all.test";
        authService.register(new RegisterRequest(testEmail, "SecurePass123!", "Anomaly Test User"), "127.0.0.1");
        testUser = userRepository.findByEmail(testEmail).orElseThrow();
        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        token = "Bearer " + jwtService.generateToken(userDetails, testUser.getId());
    }

    @Test
    @DisplayName("GET /api/v1/health/signals - retrieve evaluated health signals")
    void shouldRetrieveEvaluatedSignals() throws Exception {
        mockMvc.perform(get("/api/v1/health/signals")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(7))))
                .andExpect(jsonPath("$.data[0].metric", notNullValue()))
                .andExpect(jsonPath("$.data[0].state", notNullValue()))
                .andExpect(jsonPath("$.data[0].explanation", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/health/status - retrieve overall health state summary")
    void shouldRetrieveHealthStatus() throws Exception {
        mockMvc.perform(get("/api/v1/health/status")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.aggregateState", is("STABLE")))
                .andExpect(jsonPath("$.data.statusHeadline", notNullValue()))
                .andExpect(jsonPath("$.data.disclaimer", containsString("DO NOT equal a clinical disease diagnosis")));
    }

    @Test
    @DisplayName("GET /api/v1/health/anomalies - detect multi-signal compound anomaly pattern")
    void shouldDetectMultiSignalCompoundAnomaly() throws Exception {
        Instant now = Instant.now();

        // 1. Ingest historical baseline for RHR, Sleep, HRV (6 days of normal readings)
        for (int i = 8; i >= 3; i--) {
            ingestObs(ObservationType.HEART_RATE, "Resting Heart Rate", 60.0, "bpm", now.minus(i, ChronoUnit.DAYS));
            ingestObs(ObservationType.HRV, "HRV", 55.0, "ms", now.minus(i, ChronoUnit.DAYS));
            ingestObs(ObservationType.SLEEP, "Sleep", 8.0, "hours", now.minus(i, ChronoUnit.DAYS));
        }

        // 2. Ingest persistent strain pattern for last 2 days: Sleep ↓ (5 hrs), RHR ↑ (72 bpm), HRV ↓ (38 ms)
        for (int i = 2; i >= 1; i--) {
            ingestObs(ObservationType.HEART_RATE, "Resting Heart Rate", 72.0, "bpm", now.minus(i, ChronoUnit.DAYS));
            ingestObs(ObservationType.HRV, "HRV", 38.0, "ms", now.minus(i, ChronoUnit.DAYS));
            ingestObs(ObservationType.SLEEP, "Sleep", 5.0, "hours", now.minus(i, ChronoUnit.DAYS));
        }

        // 3. Query anomalies endpoint
        mockMvc.perform(get("/api/v1/health/anomalies")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[?(@.multiSignalCorrelationDetected == true)].patternName",
                        hasItem("Multi-Signal Autonomic Recovery Strain Pattern")))
                .andExpect(jsonPath("$.data[?(@.multiSignalCorrelationDetected == true)].explanation",
                        hasItem(containsString("personal historical baseline"))));

        // 4. Query status endpoint to verify aggregate status reflects the pattern
        mockMvc.perform(get("/api/v1/health/status")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.multiSignalPatternActive", is(true)))
                .andExpect(jsonPath("$.data.aggregateState", is("ANOMALY")));
    }

    private void ingestObs(ObservationType type, String name, double value, String unit, Instant timestamp) throws Exception {
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
