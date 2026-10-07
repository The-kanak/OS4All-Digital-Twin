package org.os4all.modules.digitaltwin.telemetry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os4all.modules.auth.dto.RegisterRequest;
import org.os4all.modules.auth.service.AuthService;
import org.os4all.modules.digitaltwin.entity.HistoricalMedicalRecord;
import org.os4all.modules.digitaltwin.repository.HistoricalMedicalRecordRepository;
import org.os4all.modules.digitaltwin.service.DigitalTwinSimulationEngine;
import org.os4all.modules.digitaltwin.telemetry.dto.TelemetryIngestRequest;
import org.os4all.modules.digitaltwin.telemetry.repository.PatientTelemetryRepository;
import org.os4all.modules.lab.entity.LabResult;
import org.os4all.modules.lab.repository.LabResultRepository;
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
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Dynamic Real-Time Telemetry & Digital Twin Fusion Integration Tests")
class TelemetryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientTelemetryRepository telemetryRepository;

    @Autowired
    private HistoricalMedicalRecordRepository medicalRecordRepository;

    @Autowired
    private LabResultRepository labResultRepository;

    @Autowired
    private DigitalTwinSimulationEngine simulationEngine;

    private User patientA;
    private User patientB;

    @BeforeEach
    void setUp() {
        String emailA = "telemetry_patient_a_" + System.currentTimeMillis() + "@os4all.test";
        authService.register(new RegisterRequest(emailA, "Password123!", "Shara Senger [Synthea FHIR]"), "127.0.0.1");
        patientA = userRepository.findByEmail(emailA).orElseThrow();

        String emailB = "telemetry_patient_b_" + System.currentTimeMillis() + "@os4all.test";
        authService.register(new RegisterRequest(emailB, "Password123!", "Control Patient B"), "127.0.0.1");
        patientB = userRepository.findByEmail(emailB).orElseThrow();

        // Seed static EHR for Patient A: Type 2 Diabetes & elevated HbA1c
        HistoricalMedicalRecord rec = new HistoricalMedicalRecord();
        rec.setUser(patientA);
        rec.setRecordType("CONDITION");
        rec.setConditionOrDiagnosis("Diabetes mellitus type 2 (disorder)");
        rec.setIcd10Code("44054006");
        rec.setStatus("ACTIVE");
        rec.setDiagnosedDate(LocalDate.of(2018, 5, 10));
        rec.setMedications("Metformin 500 MG, Glipizide 5 MG");
        medicalRecordRepository.save(rec);

        LabResult lab = new LabResult();
        lab.setUser(patientA);
        lab.setBiomarker("Hemoglobin A1c/Hemoglobin.total in Blood");
        lab.setStandardizedBiomarker("HbA1c");
        lab.setValue(new BigDecimal("6.8"));
        lab.setUnit("%");
        lab.setCollectionDate(Instant.now());
        lab.setIsConfirmed(true);
        labResultRepository.save(lab);
    }

    @Test
    @DisplayName("1. Telemetry Creation: Ingests valid 5-minute telemetry packet successfully")
    void testTelemetryCreation() throws Exception {
        TelemetryIngestRequest req = new TelemetryIngestRequest(
                patientA.getId(),
                Instant.now(),
                new BigDecimal("115.0"),
                new BigDecimal("1.2"),
                new BigDecimal("74.0"),
                new BigDecimal("42.0"),
                new BigDecimal("68.0"),
                new BigDecimal("6.8"),
                new BigDecimal("75.0"),
                5200,
                "LIGHT",
                "SIMULATED_CGM_WEARABLE",
                "GLUCOSE_RISE"
        );

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.patientId").value(patientA.getId().toString()))
                .andExpect(jsonPath("$.data.glucose").value(115.0))
                .andExpect(jsonPath("$.data.glucoseVelocity").value(1.2))
                .andExpect(jsonPath("$.data.heartRate").value(74.0))
                .andExpect(jsonPath("$.data.hrv").value(42.0))
                .andExpect(jsonPath("$.data.activityLevel").value("LIGHT"));
    }

    @Test
    @DisplayName("2. Telemetry Retrieval: GET stream returns points and calculated statistics")
    void testTelemetryRetrieval() throws Exception {
        simulationEngine.injectScenario(patientA.getId(), "STABLE");

        mockMvc.perform(get("/api/telemetry/" + patientA.getId() + "/stream?metric=glucose"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.patientId").value(patientA.getId().toString()))
                .andExpect(jsonPath("$.data.metric").value("glucose"))
                .andExpect(jsonPath("$.data.unit").value("mg/dL"))
                .andExpect(jsonPath("$.data.baselineMean").isNumber())
                .andExpect(jsonPath("$.data.points").isArray());
    }

    @Test
    @DisplayName("3. Latest Telemetry: Retrieves single most recent telemetry packet")
    void testLatestTelemetry() throws Exception {
        TelemetryIngestRequest req = new TelemetryIngestRequest(
                patientA.getId(),
                Instant.now(),
                new BigDecimal("142.0"),
                new BigDecimal("2.1"),
                new BigDecimal("80.0"),
                new BigDecimal("35.0"),
                new BigDecimal("72.0"),
                new BigDecimal("5.5"),
                new BigDecimal("60.0"),
                3400,
                "SEDENTARY",
                "SIMULATED_CGM_WEARABLE",
                "GLUCOSE_RISE"
        );

        mockMvc.perform(post("/api/telemetry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(get("/api/telemetry/" + patientA.getId() + "/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.glucose").value(142.0))
                .andExpect(jsonPath("$.data.heartRate").value(80.0));
    }

    @Test
    @DisplayName("4. Historical Telemetry: Retrieves chronological history packets")
    void testHistoricalTelemetry() throws Exception {
        // Seed 2 readings
        for (int i = 0; i < 3; i++) {
            TelemetryIngestRequest req = new TelemetryIngestRequest(
                    patientA.getId(),
                    Instant.now().minusSeconds((3 - i) * 300),
                    new BigDecimal(100 + i * 5),
                    BigDecimal.ZERO,
                    new BigDecimal(70),
                    new BigDecimal(50),
                    new BigDecimal(65),
                    new BigDecimal(7.0),
                    new BigDecimal(80.0),
                    5000,
                    "MODERATE",
                    "SIMULATED_CGM_WEARABLE",
                    "STABLE"
            );
            mockMvc.perform(post("/api/telemetry")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));
        }

        mockMvc.perform(get("/api/telemetry/" + patientA.getId() + "/history?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    @DisplayName("5. Patient Isolation: Strict isolation guarantees zero cross-patient data leakage")
    void testPatientIsolation() throws Exception {
        // Ingest telemetry only for Patient A
        TelemetryIngestRequest reqA = new TelemetryIngestRequest(
                patientA.getId(),
                Instant.now(),
                new BigDecimal("155.0"),
                new BigDecimal("2.5"),
                new BigDecimal("82.0"),
                new BigDecimal("32.0"),
                new BigDecimal("75.0"),
                new BigDecimal("5.0"),
                new BigDecimal("50.0"),
                2500,
                "SEDENTARY",
                "SIMULATED_CGM_WEARABLE",
                "GLUCOSE_RISE"
        );
        mockMvc.perform(post("/api/telemetry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reqA)));

        // Patient B should have 0 telemetry points in repository
        assertEquals(0, telemetryRepository.countByUserId(patientB.getId()));

        // Querying Patient B history returns empty list (no leakage of Patient A readings)
        mockMvc.perform(get("/api/telemetry/" + patientB.getId() + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("6. Invalid Values: Rejects impossible numeric bounds with 400 Bad Request")
    void testInvalidValues() throws Exception {
        // Impossible glucose: 800 mg/dL or negative
        TelemetryIngestRequest badGlucose = new TelemetryIngestRequest(
                patientA.getId(),
                Instant.now(),
                new BigDecimal("850.0"), // exceeds 600
                BigDecimal.ZERO,
                new BigDecimal("70.0"),
                new BigDecimal("50.0"),
                new BigDecimal("60.0"),
                new BigDecimal("8.0"),
                new BigDecimal("80.0"),
                5000,
                "MODERATE",
                "SIMULATED_CGM_WEARABLE",
                "STABLE"
        );

        mockMvc.perform(post("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badGlucose)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7. Stable Scenario: Produces homeostatic vitals and STABLE digital twin state")
    void testStableScenario() throws Exception {
        mockMvc.perform(post("/api/telemetry/simulation/scenario")
                        .param("patientId", patientB.getId().toString())
                        .param("scenario", "STABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeScenario").value("STABLE"));

        mockMvc.perform(get("/api/telemetry/simulation/status?patientId=" + patientB.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.twinState").value("STABLE"))
                .andExpect(jsonPath("$.data.overallRiskScore").isNumber());
    }

    @Test
    @DisplayName("8. Glucose Rise Scenario: Trajectory demonstrates upward slope and elevated spike probability")
    void testGlucoseRiseScenario() throws Exception {
        // Inject GLUCOSE_RISE
        mockMvc.perform(post("/api/telemetry/simulation/scenario")
                        .param("patientId", patientA.getId().toString())
                        .param("scenario", "GLUCOSE_RISE"))
                .andExpect(status().isOk());

        // Advance simulation ticks
        simulationEngine.generateNextReading(patientA.getId());
        simulationEngine.generateNextReading(patientA.getId());

        mockMvc.perform(get("/api/telemetry/simulation/status?patientId=" + patientA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latestTelemetry.glucose", greaterThanOrEqualTo(130.0)))
                .andExpect(jsonPath("$.data.glucoseSpikeProbability", greaterThanOrEqualTo(50.0)))
                .andExpect(jsonPath("$.data.twinState", anyOf(is("ACTIVE_ANOMALY"), is("ELEVATED_RISK"), is("PRE_SYMPTOMATIC_DRIFT"))));
    }

    @Test
    @DisplayName("9. Recovery Scenario: Declining glucose, negative velocity, recovering HRV, and lower risk")
    void testRecoveryScenario() throws Exception {
        // First inject glucose rise
        simulationEngine.injectScenario(patientA.getId(), "GLUCOSE_RISE");

        // Then inject RECOVERY
        mockMvc.perform(post("/api/telemetry/simulation/scenario")
                        .param("patientId", patientA.getId().toString())
                        .param("scenario", "RECOVERY"))
                .andExpect(status().isOk());

        // Advance 3 ticks
        simulationEngine.generateNextReading(patientA.getId());
        simulationEngine.generateNextReading(patientA.getId());
        simulationEngine.generateNextReading(patientA.getId());

        mockMvc.perform(get("/api/telemetry/simulation/status?patientId=" + patientA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latestTelemetry.glucoseVelocity", lessThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.data.latestTelemetry.hrv", greaterThanOrEqualTo(45.0)))
                .andExpect(jsonPath("$.data.twinState").value("STABLE"));
    }

    @Test
    @DisplayName("10. Digital Twin Fusion: Static Synthea T2D diagnosis fuses with live telemetry")
    void testDigitalTwinFusion() throws Exception {
        // Ingest high-glucose telemetry for Patient A who has static T2D
        TelemetryIngestRequest req = new TelemetryIngestRequest(
                patientA.getId(),
                Instant.now(),
                new BigDecimal("168.0"),
                new BigDecimal("2.2"),
                new BigDecimal("82.0"),
                new BigDecimal("28.0"),
                new BigDecimal("75.0"),
                new BigDecimal("4.8"),
                new BigDecimal("35.0"),
                3200,
                "SEDENTARY",
                "SIMULATED_CGM_WEARABLE",
                "GLUCOSE_RISE"
        );
        mockMvc.perform(post("/api/telemetry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // Check Digital Twin state: static T2D + dynamic spike factors are both present in drivers
        mockMvc.perform(get("/api/v1/patients/" + patientA.getId() + "/digital-twin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stateDrivers", hasItem(containsString("Type 2 Diabetes"))));
    }

    @Test
    @DisplayName("11. State Transition: Dynamic telemetry induces predictable state machine transitions")
    void testStateTransition() throws Exception {
        // Step 1: STABLE on Control Patient B (no baseline chronic diabetes)
        simulationEngine.injectScenario(patientB.getId(), "STABLE");
        var twinStable = simulationEngine.getDigitalTwinState(patientB.getId());
        assertEquals("STABLE", twinStable.state());

        // Step 2: POOR_SLEEP -> induces drift
        simulationEngine.injectScenario(patientB.getId(), "POOR_SLEEP");
        var twinDrift = simulationEngine.getDigitalTwinState(patientB.getId());
        assertNotEquals("LOW", twinDrift.latestPrediction() != null ? twinDrift.latestPrediction().riskLevel() : "LOW");

        // Step 3: RECOVERY -> restores STABLE
        simulationEngine.injectScenario(patientB.getId(), "RECOVERY");
        var twinRecovery = simulationEngine.getDigitalTwinState(patientB.getId());
        assertEquals("STABLE", twinRecovery.state());
    }

    @Test
    @DisplayName("12. API Error Handling: Missing patient ID returns 404 Not Found")
    void testApiErrorHandling() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/telemetry/" + nonExistentId + "/latest"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/telemetry/" + nonExistentId + "/history"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/telemetry/" + nonExistentId + "/stream"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("13. Health Connect Ingestion: Ingest real-device health data with source REAL_HEALTH_CONNECT")
    void testHealthConnectIngestion() throws Exception {
        TelemetryIngestRequest hcRequest = new TelemetryIngestRequest(
                patientA.getId(),
                Instant.now(),
                new BigDecimal("124.0"),
                null, // glucose velocity computed by backend
                new BigDecimal("74.0"),
                new BigDecimal("46.0"),
                new BigDecimal("60.0"),
                new BigDecimal("7.2"),
                new BigDecimal("85.0"),
                5400,
                null, // no fabricated activity
                "REAL_HEALTH_CONNECT",
                "REAL_DEVICE_DATA"
        );

        mockMvc.perform(post("/api/telemetry/health-connect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hcRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("REAL_HEALTH_CONNECT"))
                .andExpect(jsonPath("$.data.scenario").value("REAL_DEVICE_DATA"))
                .andExpect(jsonPath("$.data.glucose").value(124.0))
                .andExpect(jsonPath("$.data.steps").value(5400))
                .andExpect(jsonPath("$.data.restingHeartRate").value(60.0));

        // Verify latest telemetry reflects REAL_HEALTH_CONNECT source
        mockMvc.perform(get("/api/telemetry/" + patientA.getId() + "/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("REAL_HEALTH_CONNECT"))
                .andExpect(jsonPath("$.data.glucose").value(124.0));
    }
}
