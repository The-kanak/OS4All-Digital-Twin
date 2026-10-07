package org.os4all.modules.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.os4all.core.common.ApiResponse;
import org.os4all.modules.demo.dto.DemoScenarioResponse;
import org.os4all.modules.demo.service.DemoScenarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo")
@Tag(name = "Hackathon Flagship Demo", description = "Deterministic reproducible demonstration scenario of the entire OS4All architecture")
public class DemoScenarioController {

    private final DemoScenarioService demoScenarioService;

    public DemoScenarioController(DemoScenarioService demoScenarioService) {
        this.demoScenarioService = demoScenarioService;
    }

    @PostMapping("/reset")
    @Operation(
            summary = "Reset Demo Scenario to Baseline (STABLE)",
            description = "Seeds 45 days of 100% stable homeostatic baseline observations for 'Demo User — Synthetic Data'. Returns STABLE aggregate state."
    )
    public ResponseEntity<ApiResponse<DemoScenarioResponse>> resetDemo(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        DemoScenarioResponse response = demoScenarioService.resetDemoScenario(clientIp);
        return ResponseEntity.ok(ApiResponse.success("Demo environment reset to stable personal baseline.", response));
    }

    @PostMapping("/run-health-drift")
    @Operation(
            summary = "Run Multi-Signal Health Drift Demo Scenario",
            description = "Executes the deterministic progression: STABLE -> DRIFT -> MULTI-SIGNAL ANOMALY over 45 days of synthetic data (Sleep, Resting HR, HRV, SpO2, Activity, Temp)."
    )
    public ResponseEntity<ApiResponse<DemoScenarioResponse>> runHealthDrift(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        DemoScenarioResponse response = demoScenarioService.runHealthDriftScenario(clientIp);
        return ResponseEntity.ok(ApiResponse.success("Deterministic health drift demo scenario executed successfully.", response));
    }

    @PostMapping("/run-flagship-scenario")
    @Operation(
            summary = "Run OS4All Flagship Hackathon Demonstration",
            description = "Executes the complete multi-signal chain: 45 days of synthetic observations -> Personal Baseline Engine -> Anomaly Engine -> Nemotron Reasoning -> Tavily Evidence -> Structured Insight -> Timeline Update."
    )
    public ResponseEntity<ApiResponse<DemoScenarioResponse>> runDemonstration(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        DemoScenarioResponse response = demoScenarioService.runDemonstrationScenario(clientIp);
        return ResponseEntity.ok(ApiResponse.success("Flagship OS4All demonstration scenario executed successfully.", response));
    }
}
