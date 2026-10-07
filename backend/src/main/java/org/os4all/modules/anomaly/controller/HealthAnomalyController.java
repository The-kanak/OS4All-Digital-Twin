package org.os4all.modules.anomaly.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.os4all.core.common.ApiResponse;
import org.os4all.core.exception.UnauthorizedException;
import org.os4all.core.security.CustomUserDetails;
import org.os4all.modules.anomaly.model.AnomalyEvent;
import org.os4all.modules.anomaly.model.HealthSignal;
import org.os4all.modules.anomaly.model.HealthStatusSummary;
import org.os4all.modules.anomaly.service.RuleBasedAnomalyEngine;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Trend & Anomaly Engine", description = "Transparent, rule-based biometric signal evaluation, multi-signal correlation, and state detection (STABLE, DRIFT, ANOMALY, FOLLOW_UP)")
@SecurityRequirement(name = "BearerAuth")
public class HealthAnomalyController {

    private final RuleBasedAnomalyEngine anomalyEngine;

    public HealthAnomalyController(RuleBasedAnomalyEngine anomalyEngine) {
        this.anomalyEngine = anomalyEngine;
    }

    @GetMapping("/signals")
    @Operation(
            summary = "Get evaluated health signals",
            description = "Retrieves all continuous health signals evaluating deviation from personal baseline, persistence days, confidence, and direction"
    )
    public ResponseEntity<ApiResponse<List<HealthSignal>>> getSignals(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        verifyAuthenticated(userDetails);
        List<HealthSignal> signals = anomalyEngine.evaluateHealthSignals(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(signals));
    }

    @GetMapping("/anomalies")
    @Operation(
            summary = "Get active health anomaly events",
            description = "Retrieves active anomaly events with structured explanations, baseline comparisons, persistence duration, confidence, and multi-signal correlations"
    )
    public ResponseEntity<ApiResponse<List<AnomalyEvent>>> getAnomalies(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        verifyAuthenticated(userDetails);
        List<AnomalyEvent> anomalies = anomalyEngine.detectAnomalies(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(anomalies));
    }

    @GetMapping("/status")
    @Operation(
            summary = "Get overall health state status",
            description = "Retrieves aggregate state (STABLE, DRIFT, ANOMALY, FOLLOW_UP), active anomaly counts, multi-signal pattern summaries, and clinical non-diagnostic disclaimer"
    )
    public ResponseEntity<ApiResponse<HealthStatusSummary>> getStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        verifyAuthenticated(userDetails);
        HealthStatusSummary summary = anomalyEngine.evaluateStatus(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    private void verifyAuthenticated(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
    }
}
