package org.os4all.modules.digitaltwin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os4all.modules.digitaltwin.dto.DigitalTwinStateDto;
import org.os4all.modules.digitaltwin.dto.PatientDetailDto;
import org.os4all.modules.digitaltwin.dto.PredictionResultDto;
import org.os4all.modules.digitaltwin.entity.DigitalTwinPrediction;
import org.os4all.modules.digitaltwin.entity.DigitalTwinState;
import org.os4all.modules.digitaltwin.entity.HistoricalMedicalRecord;
import org.os4all.modules.digitaltwin.fhir.service.FhirImportService;
import org.os4all.modules.digitaltwin.repository.DigitalTwinPredictionRepository;
import org.os4all.modules.digitaltwin.repository.DigitalTwinStateRepository;
import org.os4all.modules.digitaltwin.repository.HistoricalMedicalRecordRepository;
import org.os4all.modules.digitaltwin.service.DigitalTwinService;
import org.os4all.modules.digitaltwin.service.DigitalTwinSimulationEngine;
import org.os4all.modules.digitaltwin.engine.MetabolicPredictionEngine;
import org.os4all.modules.digitaltwin.telemetry.dto.TelemetryIngestRequest;
import org.os4all.modules.digitaltwin.telemetry.dto.TelemetryPacketDto;
import org.os4all.modules.digitaltwin.telemetry.repository.PatientTelemetryRepository;
import org.os4all.modules.digitaltwin.telemetry.service.TelemetryService;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Judge Demonstration End-to-End Validation Suite")
class JudgeEndToEndValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FhirImportService fhirImportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HistoricalMedicalRecordRepository medicalRecordRepository;

    @Autowired
    private LabResultRepository labResultRepository;

    @Autowired
    private PatientTelemetryRepository telemetryRepository;

    @Autowired
    private DigitalTwinStateRepository twinStateRepository;

    @Autowired
    private DigitalTwinPredictionRepository predictionRepository;

    @Autowired
    private DigitalTwinService digitalTwinService;

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private DigitalTwinSimulationEngine simulationEngine;

    private User sharaSenger;
    private UUID sharaId;

    @BeforeEach
    void setUp() {
        if (userRepository.findAll().stream().noneMatch(u -> u.getFullName().contains("Senger"))) {
            fhirImportService.importSyntheaCohort("D:/Projects/synthea-master/output/fhir", true);
        }

        sharaSenger = userRepository.findAll().stream()
                .filter(u -> u.getFullName().contains("Shara") || u.getFullName().contains("Senger"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Shara Senger not imported from Synthea bundle"));

        sharaId = sharaSenger.getId();
    }

    @Test
    @DisplayName("Stage 1 & 2: Primary Patient & Static EHR Verification")
    void testPrimaryPatientAndStaticEhr() {
        assertNotNull(sharaSenger);
        assertNotNull(sharaId);
        assertEquals(sharaSenger.getId(), sharaId);
        assertTrue(sharaSenger.getFullName().contains("Shara"));
        assertTrue(sharaSenger.getFullName().contains("Senger"));

        // Verify Static EHR Conditions
        List<HistoricalMedicalRecord> records = medicalRecordRepository.findByUserIdOrderByDiagnosedDateDesc(sharaId);
        assertFalse(records.isEmpty(), "Shara Senger must have historical medical records");

        boolean hasT2D = records.stream().anyMatch(r ->
                r.getConditionOrDiagnosis() != null &&
                (r.getConditionOrDiagnosis().toLowerCase().contains("diabetes") || "44054006".equals(r.getIcd10Code()))
        );
        assertTrue(hasT2D, "Shara Senger must have documented Type 2 Diabetes (SNOMED 44054006)");

        // Verify Static EHR Labs (HbA1c & Fasting Glucose)
        List<LabResult> labs = labResultRepository.findByUserIdOrderByCollectionDateDesc(sharaId);
        assertFalse(labs.isEmpty(), "Shara Senger must have longitudinal lab observations");

        boolean hasHbA1c = labs.stream().anyMatch(l ->
                l.getStandardizedBiomarker() != null && l.getStandardizedBiomarker().toLowerCase().contains("hba1c")
        );
        assertTrue(hasHbA1c, "Shara Senger must have documented historical HbA1c observations");

        // Verify Patient Detail DTO
        PatientDetailDto detail = digitalTwinService.getPatientDetail(sharaId);
        assertNotNull(detail);
        assertEquals(sharaId, detail.id());
        assertTrue(detail.fullName().contains("Shara"));
        assertNotNull(detail.bmi());
    }

    @Test
    @DisplayName("Stage 3, 4, 5, 6: Simulation Scenarios (STABLE -> GLUCOSE_RISE -> RECOVERY) with Trajectory Validation")
    void testSimulationScenariosAndTrajectory() {
        // --- 1. Scenario A: STABLE ---
        telemetryService.injectScenario(sharaId, "STABLE_PATIENT");
        simulationEngine.generateNextReading(sharaId);

        TelemetryPacketDto stablePkt = telemetryService.getLatestTelemetry(sharaId);
        DigitalTwinStateDto stableState = digitalTwinService.getDigitalTwinState(sharaId);
        PredictionResultDto stablePred = digitalTwinService.getLatestPrediction(sharaId);

        System.out.println("=== SCENARIO A: STABLE ===");
        System.out.println("Glucose: " + stablePkt.glucose() + " mg/dL");
        System.out.println("Velocity: " + stablePkt.glucoseVelocity() + " mg/dL/min");
        System.out.println("Projected @ 120m: " + stablePred.projectedGlucose120Min() + " mg/dL");
        System.out.println("Projected Delta: " + stablePred.projectedDelta() + " mg/dL");
        System.out.println("Trajectory Direction: " + stablePred.trajectoryDirection());
        System.out.println("Risk Score: " + stableState.overallRiskScore() + " / 100");
        System.out.println("Twin State: " + stableState.state());

        assertEquals(5, stablePred.trajectoryPoints().size(), "Must have 5 trajectory milestone points");

        // --- 2. Scenario B: GLUCOSE_RISE ---
        telemetryService.injectScenario(sharaId, "GLUCOSE_SPIKE");
        for (int i = 0; i < 3; i++) {
            simulationEngine.generateNextReading(sharaId);
        }

        DigitalTwinStateDto riseState = digitalTwinService.getDigitalTwinState(sharaId);
        PredictionResultDto risePred = digitalTwinService.getLatestPrediction(sharaId);
        TelemetryPacketDto risePkt = telemetryService.getLatestTelemetry(sharaId);

        System.out.println("\n=== SCENARIO B: GLUCOSE_RISE ===");
        System.out.println("Glucose: " + risePkt.glucose() + " mg/dL");
        System.out.println("Velocity: " + risePkt.glucoseVelocity() + " mg/dL/min");
        System.out.println("Projected @ 120m: " + risePred.projectedGlucose120Min() + " mg/dL");
        System.out.println("Projected Delta: " + risePred.projectedDelta() + " mg/dL");
        System.out.println("Trajectory Direction: " + risePred.trajectoryDirection());
        System.out.println("Risk Score: " + riseState.overallRiskScore() + " / 100");
        System.out.println("Twin State: " + riseState.state());

        assertTrue(risePkt.glucose().doubleValue() > stablePkt.glucose().doubleValue(),
                "Glucose must rise in GLUCOSE_RISE scenario");
        assertTrue(risePkt.glucoseVelocity().doubleValue() > 0.0,
                "Glucose velocity must be positive in GLUCOSE_RISE scenario");
        assertEquals("RISING", risePred.trajectoryDirection(),
                "Trajectory direction must be RISING");
        assertTrue(risePred.projectedGlucose120Min().doubleValue() > risePkt.glucose().doubleValue(),
                "Projected glucose must be higher than current glucose");
        assertTrue(risePred.projectedDelta().doubleValue() > 10.0,
                "Projected delta must be strongly positive");

        // --- 3. Scenario C: RECOVERY ---
        telemetryService.injectScenario(sharaId, "RECOVERY");
        for (int i = 0; i < 3; i++) {
            simulationEngine.generateNextReading(sharaId);
        }

        DigitalTwinStateDto recState = digitalTwinService.getDigitalTwinState(sharaId);
        PredictionResultDto recPred = digitalTwinService.getLatestPrediction(sharaId);
        TelemetryPacketDto recPkt = telemetryService.getLatestTelemetry(sharaId);

        System.out.println("\n=== SCENARIO C: RECOVERY ===");
        System.out.println("Glucose: " + recPkt.glucose() + " mg/dL");
        System.out.println("Velocity: " + recPkt.glucoseVelocity() + " mg/dL/min");
        System.out.println("Projected @ 120m: " + recPred.projectedGlucose120Min() + " mg/dL");
        System.out.println("Projected Delta: " + recPred.projectedDelta() + " mg/dL");
        System.out.println("Trajectory Direction: " + recPred.trajectoryDirection());
        System.out.println("Risk Score: " + recState.overallRiskScore() + " / 100");
        System.out.println("Twin State: " + recState.state());

        assertTrue(recPkt.glucoseVelocity().doubleValue() <= 0.0,
                "Glucose velocity must become non-positive/negative in RECOVERY scenario");
        assertTrue(recPred.projectedDelta().doubleValue() <= 0.0,
                "Projected delta must be downward in RECOVERY scenario");
        assertTrue(recPred.trajectoryDirection().equals("FALLING") || recPred.trajectoryDirection().equals("STABLE"),
                "Trajectory direction must shift to FALLING or STABLE in RECOVERY");
    }

    @Test
    @DisplayName("Stage 7: Independent Mathematical Verification of Trajectory")
    void testMathematicalFormulaAgreement() {
        // Ingest a controlled telemetry packet
        Instant now = Instant.now();
        BigDecimal g0 = new BigDecimal("125.0");
        BigDecimal v0 = new BigDecimal("1.10"); // mg/dL/min
        String activity = "MODERATE";

        TelemetryIngestRequest req = new TelemetryIngestRequest(
                sharaId,
                now,
                g0,
                v0,
                new BigDecimal("76.0"),
                new BigDecimal("48.0"),
                new BigDecimal("64.0"),
                new BigDecimal("7.0"),
                new BigDecimal("80.0"),
                6500,
                activity,
                "TEST_HARNESS",
                "CONTROLLED_RISE"
        );
        telemetryService.ingestTelemetry(req);

        PredictionResultDto pred = digitalTwinService.getLatestPrediction(sharaId);

        // Theoretical calculation (Formulation A):
        // Shara Senger has confirmed T2D -> lambda = 0.010 min^-1 (prototype clearance parameter)
        // Activity is MODERATE -> v_activity = 0.0 mg/dL/min (activity velocity modifier)
        // Formula: G(t) = G0 + ((v0 + v_activity) / lambda) * (1 - e^(-lambda * t))
        double lambda = 0.010;
        double vActivity = 0.0;
        double effV = v0.doubleValue() + vActivity;

        for (PredictionResultDto.TrajectoryPointDto pt : pred.trajectoryPoints()) {
            int t = pt.minuteOffset();
            double expectedDelta = (effV / lambda) * (1.0 - Math.exp(-lambda * t));
            double expectedG = Math.min(400.0, Math.max(40.0, g0.doubleValue() + expectedDelta));

            BigDecimal expectedRounded = BigDecimal.valueOf(expectedG).setScale(1, RoundingMode.HALF_UP);
            BigDecimal actualRounded = pt.projectedGlucose().setScale(1, RoundingMode.HALF_UP);

            System.out.printf("T=+%d min: Expected=%.1f mg/dL, Actual=%.1f mg/dL\n", t, expectedRounded.doubleValue(), actualRounded.doubleValue());
            assertEquals(expectedRounded.doubleValue(), actualRounded.doubleValue(), 0.15,
                    "Projected glucose at t=" + t + " must mathematically match damped velocity decay model");
        }
    }

    @Test
    @DisplayName("Stage 8: Persistence Verification in DB Entities")
    void testPersistenceInDatabase() {
        DigitalTwinState state = twinStateRepository.findTopByUserIdOrderByLastUpdatedAtDesc(sharaId).orElseThrow();
        assertNotNull(state.getProjectedGlucose120Min(), "DigitalTwinState must persist projectedGlucose120Min");
        assertNotNull(state.getGlucoseVelocity(), "DigitalTwinState must persist glucoseVelocity");
        assertNotNull(state.getTrajectoryDirection(), "DigitalTwinState must persist trajectoryDirection");

        DigitalTwinPrediction pred = predictionRepository.findTopByUserIdOrderByCreatedAtDesc(sharaId).orElseThrow();
        assertNotNull(pred.getCurrentGlucose(), "DigitalTwinPrediction must persist currentGlucose");
        assertNotNull(pred.getProjectedGlucose120Min(), "DigitalTwinPrediction must persist projectedGlucose120Min");
        assertNotNull(pred.getProjectedDelta(), "DigitalTwinPrediction must persist projectedDelta");
        assertNotNull(pred.getTrajectoryDirection(), "DigitalTwinPrediction must persist trajectoryDirection");
        assertNotNull(pred.getTrajectoryPointsJson(), "DigitalTwinPrediction must persist trajectoryPointsJson");
        assertTrue(pred.getTrajectoryPointsJson().contains("minuteOffset"), "JSON must contain serialized trajectory points");
    }

    @Test
    @DisplayName("Stage 9: REST Endpoints Return Valid Trajectory Data")
    void testRestEndpointExposure() throws Exception {
        mockMvc.perform(get("/api/v1/patients/{patientId}/prediction", sharaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectedGlucose120Min").isNumber())
                .andExpect(jsonPath("$.data.glucoseVelocity").isNumber())
                .andExpect(jsonPath("$.data.trajectoryDirection").isString())
                .andExpect(jsonPath("$.data.trajectoryPoints").isArray())
                .andExpect(jsonPath("$.data.trajectoryPoints[0].minuteOffset").value(0))
                .andExpect(jsonPath("$.data.trajectoryPoints[4].minuteOffset").value(120))
                .andExpect(jsonPath("$.data.disclaimer").value(org.hamcrest.Matchers.containsString("Prototype")));

        mockMvc.perform(get("/api/v1/patients/{patientId}/digital-twin", sharaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectedGlucose120Min").isNumber())
                .andExpect(jsonPath("$.data.glucoseVelocity").isNumber())
                .andExpect(jsonPath("$.data.trajectoryDirection").isString());
    }

    @Test
    @DisplayName("Stage 10: Grounded AI Explanation Generation")
    void testGroundedExplanationEndpoint() throws Exception {
        var reqBody = java.util.Map.of("question", "Why is this patient's glucose trajectory changing?");
        mockMvc.perform(post("/api/v1/patients/{patientId}/interact", sharaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answer").isString())
                .andExpect(jsonPath("$.data.groundedEvidence").isArray())
                .andExpect(jsonPath("$.data.disclaimer").isString());
    }

    @Test
    @DisplayName("Stage 11: Safety, Bounds Clamping, and Patient Isolation")
    void testSafetyAndPatientIsolation() {
        // Unknown patient returns 404
        UUID fakeId = UUID.randomUUID();
        assertThrows(Exception.class, () -> digitalTwinService.getDigitalTwinState(fakeId));

        // Extreme values beyond physiological limits are rejected by TelemetryService
        TelemetryIngestRequest extremeReq = new TelemetryIngestRequest(
                sharaId,
                Instant.now(),
                new BigDecimal("850.0"), // extreme
                new BigDecimal("15.0"),  // extreme velocity
                new BigDecimal("80.0"),
                new BigDecimal("50.0"),
                new BigDecimal("60.0"),
                new BigDecimal("7.0"),
                new BigDecimal("80.0"),
                5000,
                "MODERATE",
                "TEST",
                "EXTREME"
        );
        assertThrows(IllegalArgumentException.class, () -> telemetryService.ingestTelemetry(extremeReq),
                "Telemetry bounds validation must reject extreme glucose values > 600 mg/dL");

        // Mathematical trajectory projection engine safety clamping
        MetabolicPredictionEngine.EngineeredFeatures extremeFeat = new MetabolicPredictionEngine.EngineeredFeatures(
                new BigDecimal("550.0"), // Extreme glucose input
                new BigDecimal("25.0"),  // Extreme slope (25/5 = 5.0 mg/dL/min > 3.0 clamp)
                new BigDecimal("10.0"),
                new BigDecimal("10.0"),
                0.0,
                new BigDecimal("10.0"),
                0.0,
                new BigDecimal("7.0"),
                new BigDecimal("0.0"),
                new BigDecimal("30.0"),
                new BigDecimal("8.0"),
                new BigDecimal("180.0"),
                0,
                true
        );
        MetabolicPredictionEngine.PredictionOutput clampedOut = MetabolicPredictionEngine.predict(extremeFeat);
        assertTrue(clampedOut.trajectoryProjection().projectedGlucose120Min().doubleValue() <= 400.0, "Prediction must be clamped to max 400.0");
        assertTrue(clampedOut.trajectoryProjection().projectedGlucose120Min().doubleValue() >= 40.0, "Prediction must be clamped to min 40.0");
        assertEquals(new BigDecimal("3.00"), clampedOut.trajectoryProjection().glucoseVelocityMgDlPerMin(), "Velocity must be clamped to max 3.00 mg/dL/min");

        // Patient isolation: Verify another patient exists and has independent state
        User controlPatient = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(sharaId) && !u.getFullName().equals(sharaSenger.getFullName()))
                .findFirst()
                .orElse(null);

        if (controlPatient != null) {
            DigitalTwinStateDto sharaState = digitalTwinService.getDigitalTwinState(sharaId);
            DigitalTwinStateDto controlState = digitalTwinService.getDigitalTwinState(controlPatient.getId());

            assertNotEquals(sharaState.patientId(), controlState.patientId());
            assertNotEquals(sharaState.patientName(), controlState.patientName());
        }
    }
}
