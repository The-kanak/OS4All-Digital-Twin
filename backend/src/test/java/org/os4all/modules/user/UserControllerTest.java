package org.os4all.modules.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os4all.modules.auth.dto.RegisterRequest;
import org.os4all.modules.user.dto.UpdateProfileRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = "profile_test_" + System.currentTimeMillis() + "@os4all.test";
        RegisterRequest registerRequest = new RegisterRequest(email, "Password123!", "Profile Test User");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        jwtToken = objectMapper.readTree(response).at("/data/token").asText();
    }

    @Test
    @DisplayName("GET /api/v1/users/me - should return profile for authenticated user")
    void shouldGetUserProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", notNullValue()))
                .andExpect(jsonPath("$.data.email", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/users/me - should return 401 without token")
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/users/me - should update profile data")
    void shouldUpdateProfile() throws Exception {
        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        updateRequest.setHeightCm(new BigDecimal("170.5"));
        updateRequest.setWeightKg(new BigDecimal("65.0"));
        updateRequest.setBiologicalSex("Female");
        updateRequest.setBloodType("O+");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.heightCm", is(170.5)))
                .andExpect(jsonPath("$.data.biologicalSex", is("Female")));
    }

    @Test
    @DisplayName("POST /api/v1/users/consent - should update consent")
    void shouldUpdateConsent() throws Exception {
        String requestBody = "{\"consentType\":\"AI_INFERENCE\",\"granted\":true}";

        mockMvc.perform(post("/api/v1/users/consent")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.consentType", is("AI_INFERENCE")))
                .andExpect(jsonPath("$.data.granted", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/users/consent - should return user consents list")
    void shouldGetUserConsents() throws Exception {
        mockMvc.perform(get("/api/v1/users/consent")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", isA(java.util.List.class)));
    }
}
