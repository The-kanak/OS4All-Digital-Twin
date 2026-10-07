package org.os4all.modules.digitaltwin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.os4all.core.common.ApiResponse;
import org.os4all.modules.baseline.model.BaselineProfile;
import org.os4all.modules.baseline.service.PersonalBaselineService;
import org.os4all.modules.digitaltwin.dto.*;
import org.os4all.modules.digitaltwin.service.DigitalTwinInteractionService;
import org.os4all.modules.digitaltwin.service.DigitalTwinService;
import org.os4all.modules.digitaltwin.service.DigitalTwinSimulationEngine;
import org.os4all.modules.timeline.dto.TimelineItemResponse;
import org.os4all.modules.timeline.service.TimelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Digital Twin Engine", description = "Endpoints for virtual patient model, live wearable telemetry, algorithmic predictions, and simulation engine")
public class DigitalTwinController {

    private final DigitalTwinService digitalTwinService;
    private final DigitalTwinSimulationEngine simulationEngine;
    private final DigitalTwinInteractionService interactionService;
    private final PersonalBaselineService baselineService;
    private final TimelineService timelineService;

    public DigitalTwinController(
            DigitalTwinService digitalTwinService,
            DigitalTwinSimulationEngine simulationEngine,
            DigitalTwinInteractionService interactionService,
            PersonalBaselineService baselineService,
            TimelineService timelineService
    ) {
        this.digitalTwinService = digitalTwinService;
        this.simulationEngine = simulationEngine;
        this.interactionService = interactionService;
        this.baselineService = baselineService;
        this.timelineService = timelineService;
    }

    @GetMapping("/patients")
    @Operation(summary = "Get all synthetic patients", description = "Returns longitudinal synthetic patients with current digital twin states and risk scores.")
    public ResponseEntity<ApiResponse<List<PatientSummaryDto>>> getAllPatients() {
        List<PatientSummaryDto> patients = digitalTwinService.getAllPatients();
        return ResponseEntity.ok(ApiResponse.success(patients));
    }

    @GetMapping("/patients/{id}")
    @Operation(summary = "Get synthetic patient details", description = "Returns demographics, historical records, previous diagnoses, and lab results.")
    public ResponseEntity<ApiResponse<PatientDetailDto>> getPatientDetail(@PathVariable UUID id) {
        PatientDetailDto patient = digitalTwinService.getPatientDetail(id);
        return ResponseEntity.ok(ApiResponse.success(patient));
    }

    @GetMapping("/patients/{id}/history")
    @Operation(summary = "Get patient historical medical records", description = "Returns medical history, existing conditions, medications, and previous diagnoses.")
    public ResponseEntity<ApiResponse<List<PatientDetailDto.HistoricalRecordDto>>> getPatientHistory(@PathVariable UUID id) {
        PatientDetailDto patient = digitalTwinService.getPatientDetail(id);
        return ResponseEntity.ok(ApiResponse.success(patient.historicalRecords()));
    }

    @GetMapping("/patients/{id}/wearables")
    @Operation(summary = "Get dynamic wearable time-series stream", description = "Returns simulated IoT wearable stream (glucose, heart rate, HRV, sleep, steps, SpO2).")
    public ResponseEntity<ApiResponse<WearableStreamDto>> getWearablesStream(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "glucose") String metric,
            @RequestParam(defaultValue = "7") int days
    ) {
        WearableStreamDto stream = digitalTwinService.getWearablesStream(id, metric, days);
        return ResponseEntity.ok(ApiResponse.success(stream));
    }

    @GetMapping("/patients/{id}/baseline")
    @Operation(summary = "Get personalized baseline profile", description = "Returns patient-specific baseline means, ranges, standard deviations, and recent deviations.")
    public ResponseEntity<ApiResponse<BaselineProfile>> getPatientBaseline(@PathVariable UUID id) {
        BaselineProfile profile = baselineService.calculateBaselineProfile(id);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/patients/{id}/digital-twin")
    @Operation(summary = "Get current living Digital Twin state", description = "Returns unified virtual patient state combining history, dynamic wearables, baselines, and risk scores.")
    public ResponseEntity<ApiResponse<DigitalTwinStateDto>> getDigitalTwinState(@PathVariable UUID id) {
        DigitalTwinStateDto state = digitalTwinService.getDigitalTwinState(id);
        return ResponseEntity.ok(ApiResponse.success(state));
    }

    @GetMapping("/patients/{id}/prediction")
    @Operation(summary = "Get primary metabolic risk prediction", description = "Returns transparent, explainable prediction of glucose spike / metabolic drift horizon.")
    public ResponseEntity<ApiResponse<PredictionResultDto>> getPrediction(@PathVariable UUID id) {
        PredictionResultDto pred = digitalTwinService.getLatestPrediction(id);
        return ResponseEntity.ok(ApiResponse.success(pred));
    }

    @GetMapping("/patients/{id}/timeline")
    @Operation(summary = "Get patient event timeline", description = "Returns unified chronological timeline of clinical events, observations, and anomalies.")
    public ResponseEntity<ApiResponse<List<TimelineItemResponse>>> getPatientTimeline(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "50") int limit
    ) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        List<TimelineItemResponse> items = timelineService.getUnifiedTimeline(id, from, Instant.now(), "ALL", limit);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @PostMapping("/patients/{id}/interact")
    @Operation(summary = "Interact with Virtual Patient Digital Twin", description = "Ask natural-language clinical questions strictly grounded in the patient's verified Digital Twin telemetry.")
    public ResponseEntity<ApiResponse<VirtualPatientInteractionDto>> interactWithPatient(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        String question = body.getOrDefault("question", "What is the patient's current Digital Twin state?");
        VirtualPatientInteractionDto response = interactionService.interact(id, question);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // --- Simulation Controls ---

    @PostMapping("/simulation/start")
    @Operation(summary = "Start continuous simulation stream", description = "Initiates wearable stream generation for the patient.")
    public ResponseEntity<ApiResponse<SimulationControlResponse>> startSimulation(@RequestParam UUID patientId) {
        SimulationControlResponse resp = simulationEngine.startSimulation(patientId);
        return ResponseEntity.ok(ApiResponse.success("Simulation started successfully.", resp));
    }

    @PostMapping("/simulation/pause")
    @Operation(summary = "Pause simulation", description = "Pauses continuous stream generation.")
    public ResponseEntity<ApiResponse<SimulationControlResponse>> pauseSimulation(@RequestParam UUID patientId) {
        SimulationControlResponse resp = simulationEngine.pauseSimulation(patientId);
        return ResponseEntity.ok(ApiResponse.success("Simulation paused.", resp));
    }

    @PostMapping("/simulation/reset")
    @Operation(summary = "Reset simulation to stable baseline", description = "Reverts patient to 45 days of stable homeostatic baseline.")
    public ResponseEntity<ApiResponse<SimulationControlResponse>> resetSimulation(@RequestParam UUID patientId) {
        SimulationControlResponse resp = simulationEngine.resetSimulation(patientId);
        return ResponseEntity.ok(ApiResponse.success("Simulation reset to stable baseline.", resp));
    }

    @PostMapping("/simulation/next-reading")
    @Operation(summary = "Generate next simulation reading", description = "Advances time-series by one reading tick.")
    public ResponseEntity<ApiResponse<SimulationControlResponse>> nextReading(@RequestParam UUID patientId) {
        SimulationControlResponse resp = simulationEngine.generateNextReading(patientId);
        return ResponseEntity.ok(ApiResponse.success("Next reading generated.", resp));
    }

    @PostMapping("/simulation/scenario/{scenario}")
    @Operation(summary = "Inject simulation scenario", description = "Injects one of 5 scenarios: STABLE_PATIENT, POOR_SLEEP, HIGH_ACTIVITY, GLUCOSE_SPIKE, RECOVERY.")
    public ResponseEntity<ApiResponse<SimulationControlResponse>> injectScenario(
            @PathVariable String scenario,
            @RequestParam UUID patientId
    ) {
        SimulationControlResponse resp = simulationEngine.injectScenario(patientId, scenario);
        return ResponseEntity.ok(ApiResponse.success("Scenario injected: " + scenario, resp));
    }

    @PostMapping("/digital-twin/update")
    @Operation(summary = "Trigger Digital Twin recomputation", description = "Recalculates baselines, deviations, and predictions from latest observations.")
    public ResponseEntity<ApiResponse<DigitalTwinStateDto>> updateDigitalTwin(@RequestParam UUID patientId) {
        digitalTwinService.updateDigitalTwinState(patientId, "MANUAL_TRIGGER");
        DigitalTwinStateDto state = digitalTwinService.getDigitalTwinState(patientId);
        return ResponseEntity.ok(ApiResponse.success("Digital Twin state recomputed.", state));
    }
}
