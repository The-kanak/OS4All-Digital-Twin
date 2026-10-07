package org.os4all.modules.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os4all.core.security.JwtService;
import org.os4all.modules.auth.dto.RegisterRequest;
import org.os4all.modules.auth.service.AuthService;
import org.os4all.modules.ingestion.dto.CreateObservationRequest;
import org.os4all.modules.ingestion.entity.ObservationType;
import org.os4all.modules.lab.dto.CreateLabReportRequest;
import org.os4all.modules.lab.dto.CreateLabResultItemRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthDataEngineIntegrationTest {

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
        String testEmail = "health_test_" + System.currentTimeMillis() + "@os4all.test";
        authService.register(new RegisterRequest(testEmail, "SecurePass123!", "Health Engine User"), "127.0.0.1");
        testUser = userRepository.findByEmail(testEmail).orElseThrow();
        org.os4all.core.security.CustomUserDetails userDetails = new org.os4all.core.security.CustomUserDetails(testUser);
        token = "Bearer " + jwtService.generateToken(userDetails, testUser.getId());
    }

    @Test
    @DisplayName("POST /api/v1/health/observations - record VitalMeasurement and normalize units")
    void shouldRecordVitalObservationWithNormalization() throws Exception {
        CreateObservationRequest req = new CreateObservationRequest();
        req.setType(ObservationType.BODY_TEMPERATURE);
        req.setValueNumeric(new BigDecimal("98.6"));
        req.setUnit("degF");
        req.setVitalName("Core Temperature");
        req.setDeviceModel("Withings Thermo");
        req.setTimestamp(Instant.now());
        req.setSource("DEMO DATA: Sensor");
        req.setConfidence(new BigDecimal("0.98"));

        mockMvc.perform(post("/api/v1/health/observations")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.observationClass", is("VitalMeasurement")))
                .andExpect(jsonPath("$.data.vitalName", is("Core Temperature")))
                .andExpect(jsonPath("$.data.deviceModel", is("Withings Thermo")))
                .andExpect(jsonPath("$.data.valueNumeric", is(98.6)))
                .andExpect(jsonPath("$.data.unit", is("degF")))
                .andExpect(jsonPath("$.data.standardValueNumeric", is(37.0)))
                .andExpect(jsonPath("$.data.standardUnit", is("°C")));
    }

    @Test
    @DisplayName("POST /api/v1/health/observations - record LifestyleObservation and SymptomObservation")
    void shouldRecordLifestyleAndSymptomObservations() throws Exception {
        // 1. Lifestyle
        CreateObservationRequest lifestyleReq = new CreateObservationRequest();
        lifestyleReq.setType(ObservationType.SLEEP);
        lifestyleReq.setValueText("Restful sleep, 2 deep cycles");
        lifestyleReq.setLifestyleCategory("Sleep Hygiene");
        lifestyleReq.setDurationMinutes(480);
        lifestyleReq.setTimestamp(Instant.now());
        lifestyleReq.setSource("DEMO DATA: Sleep Tracker");

        mockMvc.perform(post("/api/v1/health/observations")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lifestyleReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.observationClass", is("LifestyleObservation")))
                .andExpect(jsonPath("$.data.durationMinutes", is(480)));

        // 2. Symptom
        CreateObservationRequest symptomReq = new CreateObservationRequest();
        symptomReq.setType(ObservationType.SYMPTOM);
        symptomReq.setSymptomName("Mild localized headache");
        symptomReq.setSeverity("MILD");
        symptomReq.setBodySite("Frontal lobe");
        symptomReq.setTimestamp(Instant.now());
        symptomReq.setSource("DEMO DATA: Self Check");

        mockMvc.perform(post("/api/v1/health/observations")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(symptomReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.observationClass", is("SymptomObservation")))
                .andExpect(jsonPath("$.data.symptomName", is("Mild localized headache")))
                .andExpect(jsonPath("$.data.severity", is("MILD")));
    }

    @Test
    @DisplayName("GET /api/v1/health/observations - filter by type and date")
    void shouldFilterObservationsByTypeAndDate() throws Exception {
        Instant now = Instant.now();

        CreateObservationRequest hr = new CreateObservationRequest();
        hr.setType(ObservationType.HEART_RATE);
        hr.setValueNumeric(new BigDecimal("72"));
        hr.setUnit("bpm");
        hr.setTimestamp(now.minus(2, ChronoUnit.DAYS));
        hr.setSource("DEMO DATA");

        CreateObservationRequest bp = new CreateObservationRequest();
        bp.setType(ObservationType.BLOOD_PRESSURE_SYSTOLIC);
        bp.setValueNumeric(new BigDecimal("120"));
        bp.setUnit("mmHg");
        bp.setTimestamp(now);
        bp.setSource("DEMO DATA");

        mockMvc.perform(post("/api/v1/health/observations").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(hr))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/health/observations").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(bp))).andExpect(status().isCreated());

        // Filter by HEART_RATE
        mockMvc.perform(get("/api/v1/health/observations?type=HEART_RATE")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].type", is("HEART_RATE")));
    }

    @Test
    @DisplayName("POST /api/v1/health/labs & GET /api/v1/health/labs - record and query lab biomarkers")
    void shouldRecordAndQueryLabBiomarkers() throws Exception {
        CreateLabReportRequest reportReq = new CreateLabReportRequest();
        reportReq.setReportTitle("Lipid & Metabolic Profile (DEMO DATA)");
        reportReq.setLaboratoryName("LabCorp (DEMO DATA)");
        reportReq.setCollectionDate(Instant.now().minus(1, ChronoUnit.DAYS));
        reportReq.setSource("DEMO DATA: Hospital Portal");

        CreateLabResultItemRequest alt = new CreateLabResultItemRequest();
        alt.setBiomarker("ALT/SGPT");
        alt.setValue(new BigDecimal("28.0"));
        alt.setUnit("U/L");

        CreateLabResultItemRequest glucose = new CreateLabResultItemRequest();
        glucose.setBiomarker("glucose");
        glucose.setValue(new BigDecimal("5.5")); // in mmol/L
        glucose.setUnit("mmol/L");

        CreateLabResultItemRequest hba1c = new CreateLabResultItemRequest();
        hba1c.setBiomarker("HbA1c");
        hba1c.setValue(new BigDecimal("5.3"));
        hba1c.setUnit("%");

        CreateLabResultItemRequest wbc = new CreateLabResultItemRequest();
        wbc.setBiomarker("WBC");
        wbc.setValue(new BigDecimal("6800")); // raw count
        wbc.setUnit("/uL");

        reportReq.setResults(List.of(alt, glucose, hba1c, wbc));

        mockMvc.perform(post("/api/v1/health/labs")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.results", hasSize(4)))
                .andExpect(jsonPath("$.data.results[1].standardizedBiomarker", is("glucose")))
                .andExpect(jsonPath("$.data.results[1].standardValue", is(99.1))) // converted from 5.5 mmol/L
                .andExpect(jsonPath("$.data.results[1].standardUnit", is("mg/dL")))
                .andExpect(jsonPath("$.data.results[3].standardizedBiomarker", is("WBC")))
                .andExpect(jsonPath("$.data.results[3].standardValue", is(6.8)))
                .andExpect(jsonPath("$.data.results[3].standardUnit", is("10^3/uL")));

        // Query labs endpoint for specific biomarker
        mockMvc.perform(get("/api/v1/health/labs?biomarker=glucose")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].standardizedBiomarker", is("glucose")))
                .andExpect(jsonPath("$.data.content[0].standardValue", is(99.1)));
    }

    @Test
    @DisplayName("GET /api/v1/health/timeline - retrieve unified chronological health timeline")
    void shouldRetrieveUnifiedTimeline() throws Exception {
        Instant now = Instant.now();

        // 1. Add observation
        CreateObservationRequest obs = new CreateObservationRequest();
        obs.setType(ObservationType.HEART_RATE);
        obs.setValueNumeric(new BigDecimal("75"));
        obs.setUnit("bpm");
        obs.setTimestamp(now.minus(2, ChronoUnit.HOURS));
        obs.setSource("DEMO DATA: Smartwatch");
        mockMvc.perform(post("/api/v1/health/observations").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(obs))).andExpect(status().isCreated());

        // 2. Add lab report
        CreateLabReportRequest report = new CreateLabReportRequest();
        report.setReportTitle("Renal Profile (DEMO DATA)");
        report.setCollectionDate(now.minus(1, ChronoUnit.HOURS));
        report.setSource("DEMO DATA: Clinic");
        CreateLabResultItemRequest creat = new CreateLabResultItemRequest();
        creat.setBiomarker("creatinine");
        creat.setValue(new BigDecimal("0.9"));
        creat.setUnit("mg/dL");
        report.setResults(List.of(creat));
        mockMvc.perform(post("/api/v1/health/labs").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(report))).andExpect(status().isCreated());

        // 3. Query unified timeline
        mockMvc.perform(get("/api/v1/health/timeline")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))))
                // Timeline sorted chronologically descending: most recent (1 hr ago) first
                .andExpect(jsonPath("$.data[0].itemType", is("LAB_REPORT")))
                .andExpect(jsonPath("$.data[1].itemType", is("OBSERVATION")));
    }
}
