package org.os4all.modules.digitaltwin.telemetry.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.os4all.core.common.ApiResponse;
import org.os4all.modules.digitaltwin.telemetry.dto.*;
import org.os4all.modules.digitaltwin.telemetry.service.TelemetryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/telemetry")
@Tag(name = "Dynamic Telemetry Stream", description = "Endpoints for simulated 5-minute CGM, wearable, and IoT time-series telemetry")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping
    @Operation(summary = "Ingest dynamic telemetry packet", description = "Accepts a 5-minute simulated IoT/CGM packet, validates bounds, computes velocity, and recomputes Digital Twin state.")
    public ResponseEntity<ApiResponse<TelemetryPacketDto>> ingestTelemetry(
            @Valid @RequestBody TelemetryIngestRequest request
    ) {
        TelemetryPacketDto dto = telemetryService.ingestTelemetry(request);
        return ResponseEntity.ok(ApiResponse.success("Telemetry packet ingested successfully.", dto));
    }

    @PostMapping("/health-connect")
    @Operation(summary = "Ingest real-device Android Health Connect telemetry", description = "Accepts real-device health data ingested from Android Health Connect with source REAL_HEALTH_CONNECT.")
    public ResponseEntity<ApiResponse<TelemetryPacketDto>> ingestHealthConnectTelemetry(
            @Valid @RequestBody TelemetryIngestRequest request
    ) {
        TelemetryIngestRequest enriched = new TelemetryIngestRequest(
                request.patientId(),
                request.timestamp() != null ? request.timestamp() : Instant.now(),
                request.glucose(),
                request.glucoseVelocity(),
                request.heartRate(),
                request.hrv(),
                request.restingHeartRate(),
                request.sleepDurationHours(),
                request.sleepQualityScore(),
                request.steps(),
                request.activityLevel() != null ? request.activityLevel() : "MODERATE",
                "REAL_HEALTH_CONNECT",
                "REAL_DEVICE_DATA"
        );
        TelemetryPacketDto dto = telemetryService.ingestTelemetry(enriched);
        return ResponseEntity.ok(ApiResponse.success("Health Connect telemetry packet ingested successfully.", dto));
    }

    @GetMapping("/{patientId}/latest")
    @Operation(summary = "Get latest telemetry packet", description = "Returns the single most recent 5-minute telemetry packet for a specific patient.")
    public ResponseEntity<ApiResponse<TelemetryPacketDto>> getLatestTelemetry(
            @PathVariable UUID patientId
    ) {
        TelemetryPacketDto dto = telemetryService.getLatestTelemetry(patientId);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/{patientId}/history")
    @Operation(summary = "Get telemetry history", description = "Returns chronological historical telemetry packets with strict patient isolation.")
    public ResponseEntity<ApiResponse<List<TelemetryPacketDto>>> getTelemetryHistory(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String since
    ) {
        Instant sinceInstant = null;
        if (since != null && !since.isBlank()) {
            try {
                sinceInstant = Instant.parse(since);
            } catch (Exception ignored) {}
        }
        List<TelemetryPacketDto> list = telemetryService.getTelemetryHistory(patientId, limit, sinceInstant);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{patientId}/stream")
    @Operation(summary = "Get dynamic telemetry stream", description = "Returns 5-minute time series stream with baseline mean/min/max and current status.")
    public ResponseEntity<ApiResponse<TelemetryStreamDto>> getTelemetryStream(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "glucose") String metric,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "100") int limit
    ) {
        TelemetryStreamDto stream = telemetryService.getTelemetryStream(patientId, metric, days, limit);
        return ResponseEntity.ok(ApiResponse.success(stream));
    }

    @PostMapping("/simulation/start")
    @Operation(summary = "Start telemetry simulation", description = "Starts the live dynamic telemetry simulation for a patient.")
    public ResponseEntity<ApiResponse<SimulationStatusDto>> startSimulation(
            @RequestParam UUID patientId,
            @RequestParam(required = false) String scenario
    ) {
        SimulationStatusDto status = telemetryService.startSimulation(patientId, scenario);
        return ResponseEntity.ok(ApiResponse.success("Simulation stream started.", status));
    }

    @PostMapping("/simulation/stop")
    @Operation(summary = "Stop telemetry simulation", description = "Pauses the dynamic telemetry simulation for a patient.")
    public ResponseEntity<ApiResponse<SimulationStatusDto>> stopSimulation(
            @RequestParam UUID patientId
    ) {
        SimulationStatusDto status = telemetryService.stopSimulation(patientId);
        return ResponseEntity.ok(ApiResponse.success("Simulation stream stopped.", status));
    }

    @PostMapping("/simulation/scenario")
    @Operation(summary = "Set simulation scenario", description = "Injects one of 5 deterministic scenarios: STABLE, POOR_SLEEP, HIGH_ACTIVITY, GLUCOSE_RISE, RECOVERY.")
    public ResponseEntity<ApiResponse<SimulationStatusDto>> setScenario(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) String scenario,
            @RequestBody(required = false) Map<String, String> body
    ) {
        UUID effectivePatientId = patientId;
        String effectiveScenario = scenario;

        if (body != null) {
            if (effectivePatientId == null && body.containsKey("patientId")) {
                effectivePatientId = UUID.fromString(body.get("patientId"));
            }
            if (effectiveScenario == null && body.containsKey("scenario")) {
                effectiveScenario = body.get("scenario");
            }
        }

        if (effectivePatientId == null) {
            throw new IllegalArgumentException("patientId must be provided via query param or body");
        }
        if (effectiveScenario == null || effectiveScenario.isBlank()) {
            effectiveScenario = "STABLE";
        }

        SimulationStatusDto status = telemetryService.injectScenario(effectivePatientId, effectiveScenario);
        return ResponseEntity.ok(ApiResponse.success("Injected scenario: " + effectiveScenario, status));
    }

    @GetMapping("/simulation/status")
    @Operation(summary = "Get simulation status", description = "Returns current simulation state and latest reading for a patient.")
    public ResponseEntity<ApiResponse<SimulationStatusDto>> getSimulationStatus(
            @RequestParam UUID patientId
    ) {
        SimulationStatusDto status = telemetryService.getSimulationStatus(patientId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
