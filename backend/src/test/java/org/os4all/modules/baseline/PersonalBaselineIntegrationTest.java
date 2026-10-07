package org.os4all.modules.baseline;

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
class PersonalBaselineIntegrationTest {

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
        String testEmail = "baseline_user_" + System.currentTimeMillis() + "@os4all.test";
        authService.register(new RegisterRequest(testEmail, "SecurePass123!", "Baseline User"), "127.0.0.1");
        testUser = userRepository.findByEmail(testEmail).orElseThrow();
        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        token = "Bearer " + jwtService.generateToken(userDetails, testUser.getId());
    }

    @Test
    @DisplayName("GET /api/v1/health/baseline - retrieve baseline profile with insufficient data status")
    void shouldReturnProfileWithInsufficientStatusWhenNoReadings() throws Exception {
        mockMvc.perform(get("/api/v1/health/baseline")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.corePhilosophy", is("Normal for the population is not necessarily normal for the individual.")))
                .andExpect(jsonPath("$.data.metrics", hasSize(7)))
                .andExpect(jsonPath("$.data.metrics[0].baselineEstablished", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/health/baseline/resting_heart_rate - calculate established baseline when >= 5 observations exist")
    void shouldCalculateEstablishedBaselineWhenSufficientObservations() throws Exception {
        Instant now = Instant.now();

        // Ingest 6 resting heart rate readings
        double[] readings = {68.0, 70.0, 69.0, 71.0, 72.0, 70.0};
        for (int i = 0; i < readings.length; i++) {
            CreateObservationRequest req = new CreateObservationRequest();
            req.setType(ObservationType.HEART_RATE);
            req.setVitalName("Resting Heart Rate");
            req.setValueNumeric(BigDecimal.valueOf(readings[i]));
            req.setUnit("bpm");
            req.setTimestamp(now.minus(readings.length - i, ChronoUnit.DAYS));
            req.setSource("DEMO DATA: Sensor");

            mockMvc.perform(post("/api/v1/health/observations")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        // Now query specific metric baseline endpoint
        mockMvc.perform(get("/api/v1/health/baseline/resting_heart_rate")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.metric", is("resting_heart_rate")))
                .andExpect(jsonPath("$.data.baselineEstablished", is(true)))
                .andExpect(jsonPath("$.data.observationCount", is(6)))
                .andExpect(jsonPath("$.data.mean", notNullValue()))
                .andExpect(jsonPath("$.data.median", notNullValue()))
                .andExpect(jsonPath("$.data.standardDeviation", notNullValue()))
                .andExpect(jsonPath("$.data.baselineRangeLow", notNullValue()))
                .andExpect(jsonPath("$.data.baselineRangeHigh", notNullValue()))
                .andExpect(jsonPath("$.data.recentAverage", notNullValue()))
                .andExpect(jsonPath("$.data.interpretation", containsString("not a medical diagnosis")));
    }
}
