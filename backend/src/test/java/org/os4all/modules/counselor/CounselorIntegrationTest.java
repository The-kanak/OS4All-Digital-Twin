package org.os4all.modules.counselor;

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
import org.os4all.modules.counselor.dto.CounselorChatRequest;
import org.os4all.modules.counselor.entity.CounselorConversation;
import org.os4all.modules.counselor.repository.CounselorConversationRepository;
import org.os4all.modules.ingestion.dto.CreateObservationRequest;
import org.os4all.modules.ingestion.entity.ObservationType;
import org.os4all.modules.ingestion.service.HealthObservationService;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CounselorIntegrationTest {

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
    private HealthObservationService observationService;

    @Autowired
    private CounselorConversationRepository conversationRepository;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        String email = "counselor_test_" + System.currentTimeMillis() + "@os4all.test";
        RegisterRequest registerRequest = new RegisterRequest(
                email,
                "SecureP@ssw0rd123!",
                "Counselor Test User"
        );
        authService.register(registerRequest, "127.0.0.1");

        testUser = userRepository.findByEmail(email).orElseThrow();
        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        jwtToken = "Bearer " + jwtService.generateToken(userDetails, testUser.getId());

        // Grant required AI_INFERENCE and DATA_STORAGE consent
        ConsentRequest dataStorageConsent = new ConsentRequest();
        dataStorageConsent.setConsentType(ConsentType.DATA_STORAGE);
        dataStorageConsent.setGranted(true);
        consentService.updateConsent(testUser.getId(), dataStorageConsent, "127.0.0.1");

        ConsentRequest aiConsent = new ConsentRequest();
        aiConsent.setConsentType(ConsentType.AI_INFERENCE);
        aiConsent.setGranted(true);
        consentService.updateConsent(testUser.getId(), aiConsent, "127.0.0.1");
    }

    @Test
    @DisplayName("Counselor chat requires authentication")
    void testChatRequiresAuthentication() throws Exception {
        CounselorChatRequest request = new CounselorChatRequest(null, "Why did my health status change?");

        mockMvc.perform(post("/api/v1/counselor/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Counselor chat requires AI_INFERENCE consent")
    void testChatRequiresConsent() throws Exception {
        // Revoke AI_INFERENCE consent
        ConsentRequest revokeConsent = new ConsentRequest();
        revokeConsent.setConsentType(ConsentType.AI_INFERENCE);
        revokeConsent.setGranted(false);
        consentService.updateConsent(testUser.getId(), revokeConsent, "127.0.0.1");

        CounselorChatRequest request = new CounselorChatRequest(null, "Why did my health status change?");

        mockMvc.perform(post("/api/v1/counselor/chat")
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("AI Inference consent is required")));
    }

    @Test
    @DisplayName("Counselor explains health status shift using structured observations, baseline comparison, and evidence")
    void testChatExplainsHealthStatus() throws Exception {
        // Seed baseline observations
        Instant now = Instant.now();
        for (int i = 25; i >= 6; i--) {
            CreateObservationRequest req = new CreateObservationRequest();
            req.setType(ObservationType.HEART_RATE);
            req.setValueNumeric(new BigDecimal("60.0"));
            req.setUnit("bpm");
            req.setTimestamp(now.minus(i, ChronoUnit.DAYS));
            req.setSource("wearable_sensor");
            req.setConfidence(new BigDecimal("0.98"));
            observationService.recordObservation(testUser.getId(), req, "127.0.0.1");
        }

        // Seed recent anomalous observations
        for (int i = 5; i >= 1; i--) {
            CreateObservationRequest req = new CreateObservationRequest();
            req.setType(ObservationType.HEART_RATE);
            req.setValueNumeric(new BigDecimal("74.0"));
            req.setUnit("bpm");
            req.setTimestamp(now.minus(i, ChronoUnit.DAYS));
            req.setSource("wearable_sensor");
            req.setConfidence(new BigDecimal("0.95"));
            observationService.recordObservation(testUser.getId(), req, "127.0.0.1");
        }

        CounselorChatRequest chatRequest = new CounselorChatRequest(null, "Why did my health status change?");

        mockMvc.perform(post("/api/v1/counselor/chat")
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").isNotEmpty())
                .andExpect(jsonPath("$.data.os4allObservations").isArray())
                .andExpect(jsonPath("$.data.aiInterpretations").isArray())
                .andExpect(jsonPath("$.data.externalEvidence").isArray())
                .andExpect(jsonPath("$.data.recommendedActions").isArray())
                .andExpect(jsonPath("$.data.professionalEvaluationGuidance").isNotEmpty())
                .andExpect(jsonPath("$.data.urgency").value(anyOf(equalTo("MONITOR"), equalTo("ROUTINE"))))
                .andExpect(jsonPath("$.data.emergencyFlagged").value(false))
                .andExpect(jsonPath("$.data.conversationId").isNotEmpty())
                .andExpect(jsonPath("$.data.disclaimer", containsString("DO NOT constitute a medical diagnosis")));
    }

    @Test
    @DisplayName("Emergency classifier intercepts critical acute symptoms and advises emergency care without attempting diagnosis")
    void testEmergencyHandling() throws Exception {
        CounselorChatRequest emergencyRequest = new CounselorChatRequest(null, "I have crushing chest pain radiating to my left arm and I can't breathe!");

        mockMvc.perform(post("/api/v1/counselor/chat")
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emergencyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emergencyFlagged").value(true))
                .andExpect(jsonPath("$.data.urgency").value("EMERGENCY"))
                .andExpect(jsonPath("$.data.message", containsString("emergency")))
                .andExpect(jsonPath("$.data.disclaimer", containsString("EMERGENCY ADVISORY")))
                .andExpect(jsonPath("$.data.recommendedActions", hasItem(containsString("Call emergency services"))));
    }

    @Test
    @DisplayName("Conversation management: list, retrieve detail, and delete")
    void testConversationManagement() throws Exception {
        // Step 1: Send initial message to create conversation
        CounselorChatRequest req1 = new CounselorChatRequest(null, "What is my current resting heart rate baseline?");

        String responseJson = mockMvc.perform(post("/api/v1/counselor/chat")
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String convIdStr = objectMapper.readTree(responseJson).path("data").path("conversationId").asText();
        UUID conversationId = UUID.fromString(convIdStr);

        // Step 2: Send second message in same conversation
        CounselorChatRequest req2 = new CounselorChatRequest(conversationId, "How does that compare to population average?");
        mockMvc.perform(post("/api/v1/counselor/chat")
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value(convIdStr));

        // Step 3: List conversations
        mockMvc.perform(get("/api/v1/counselor/conversations")
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].messageCount").value(4)); // 2 user + 2 assistant messages

        // Step 4: Get conversation details with full message history
        mockMvc.perform(get("/api/v1/counselor/conversations/" + convIdStr)
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(convIdStr))
                .andExpect(jsonPath("$.data.messages", hasSize(4)))
                .andExpect(jsonPath("$.data.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.data.messages[1].role").value("ASSISTANT"));

        // Step 5: Delete conversation
        mockMvc.perform(delete("/api/v1/counselor/conversations/" + convIdStr)
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Step 6: Verify deletion
        mockMvc.perform(get("/api/v1/counselor/conversations/" + convIdStr)
                        .header("Authorization", jwtToken))
                .andExpect(status().isNotFound());
    }
}
