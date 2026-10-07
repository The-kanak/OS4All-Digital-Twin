package org.os4all.modules.baseline.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.os4all.core.common.ApiResponse;
import org.os4all.core.exception.UnauthorizedException;
import org.os4all.core.security.CustomUserDetails;
import org.os4all.modules.baseline.model.BaselineMetric;
import org.os4all.modules.baseline.model.BaselineProfile;
import org.os4all.modules.baseline.service.PersonalBaselineService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/health/baseline")
@Tag(name = "Personal Baseline Engine", description = "Calculates individualized statistical baselines: 'Normal for the population is not necessarily normal for the individual.'")
@SecurityRequirement(name = "BearerAuth")
public class PersonalBaselineController {

    private final PersonalBaselineService baselineService;

    public PersonalBaselineController(PersonalBaselineService baselineService) {
        this.baselineService = baselineService;
    }

    @GetMapping
    @Operation(
            summary = "Get personal baseline profile",
            description = "Calculates individualized baseline ranges, deviation, z-scores, and historical patterns across all supported continuous metrics"
    )
    public ResponseEntity<ApiResponse<BaselineProfile>> getBaselineProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        verifyAuthenticated(userDetails);
        BaselineProfile profile = baselineService.calculateBaselineProfile(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/{metric}")
    @Operation(
            summary = "Get personal baseline for specific metric",
            description = "Retrieves baseline metrics (resting_heart_rate, heart_rate, hrv, spo2, sleep_duration, steps, temperature)"
    )
    public ResponseEntity<ApiResponse<BaselineMetric>> getMetricBaseline(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Metric name: resting_heart_rate, heart_rate, hrv, spo2, sleep_duration, steps, temperature")
            @PathVariable String metric) {
        verifyAuthenticated(userDetails);
        BaselineMetric baselineMetric = baselineService.calculateSingleMetricBaseline(userDetails.getId(), metric);
        return ResponseEntity.ok(ApiResponse.success(baselineMetric));
    }

    private void verifyAuthenticated(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
    }
}
