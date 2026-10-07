package org.os4all.modules.observability.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.os4all.core.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Observability", description = "System health check and diagnostic telemetry")
public class HealthController {

    @Value("${spring.application.name:os4all-backend}")
    private String applicationName;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @GetMapping
    @Operation(summary = "System health check", description = "Verifies that the OS4All application layer is operational")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("status", "UP");
        healthData.put("application", applicationName);
        healthData.put("version", "1.0.0");
        healthData.put("environment", activeProfile);
        healthData.put("timestamp", Instant.now());
        healthData.put("framework", "Spring Boot 3.3.4 (Java 21/17 LTS)");

        return ResponseEntity.ok(ApiResponse.success("System operational", healthData));
    }
}
