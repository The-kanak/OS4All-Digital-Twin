package org.os4all.modules.ingestion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.os4all.core.common.ApiResponse;
import org.os4all.core.exception.UnauthorizedException;
import org.os4all.core.security.CustomUserDetails;
import org.os4all.modules.ingestion.dto.CreateObservationRequest;
import org.os4all.modules.ingestion.dto.ObservationResponse;
import org.os4all.modules.ingestion.entity.ObservationType;
import org.os4all.modules.ingestion.service.HealthObservationService;
import org.os4all.modules.lab.dto.CreateLabReportRequest;
import org.os4all.modules.lab.dto.LabReportResponse;
import org.os4all.modules.lab.dto.LabResultResponse;
import org.os4all.modules.lab.service.LabReportService;
import org.os4all.modules.timeline.dto.TimelineItemResponse;
import org.os4all.modules.timeline.service.TimelineService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Data Engine", description = "Endpoints for normalized health observations, lab results, and unified health timeline")
@SecurityRequirement(name = "BearerAuth")
public class HealthDataController {

    private final HealthObservationService observationService;
    private final LabReportService labReportService;
    private final TimelineService timelineService;

    public HealthDataController(
            HealthObservationService observationService,
            LabReportService labReportService,
            TimelineService timelineService
    ) {
        this.observationService = observationService;
        this.labReportService = labReportService;
        this.timelineService = timelineService;
    }

    @PostMapping("/observations")
    @Operation(summary = "Record a health observation", description = "Ingest a normalized health observation (VitalMeasurement, LifestyleObservation, SymptomObservation)")
    public ResponseEntity<ApiResponse<ObservationResponse>> recordObservation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateObservationRequest request,
            HttpServletRequest httpRequest) {
        verifyAuthenticated(userDetails);
        String clientIp = getClientIp(httpRequest);
        ObservationResponse response = observationService.recordObservation(userDetails.getId(), request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Health observation recorded successfully", response));
    }

    @GetMapping("/observations")
    @Operation(summary = "Get health observations", description = "Query health observations with filtering by type, date range, and pagination")
    public ResponseEntity<ApiResponse<Page<ObservationResponse>>> getObservations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) ObservationType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        verifyAuthenticated(userDetails);
        Pageable pageable = PageRequest.of(page, size);
        Page<ObservationResponse> response = observationService.getObservations(userDetails.getId(), type, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/labs")
    @Operation(summary = "Record a lab report with biomarker results", description = "Ingest a lab report containing one or more biomarker results (ALT, AST, bilirubin, creatinine, hemoglobin, glucose, HbA1c, WBC, platelets, cholesterol, triglycerides, etc.)")
    public ResponseEntity<ApiResponse<LabReportResponse>> recordLabReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateLabReportRequest request,
            HttpServletRequest httpRequest) {
        verifyAuthenticated(userDetails);
        String clientIp = getClientIp(httpRequest);
        LabReportResponse response = labReportService.recordLabReport(userDetails.getId(), request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lab report and biomarker results recorded successfully", response));
    }

    @GetMapping("/labs")
    @Operation(summary = "Get lab results", description = "Query biomarker lab results with filtering by biomarker name, date range, and pagination")
    public ResponseEntity<ApiResponse<Page<LabResultResponse>>> getLabResults(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String biomarker,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        verifyAuthenticated(userDetails);
        Pageable pageable = PageRequest.of(page, size);
        Page<LabResultResponse> response = labReportService.getLabResults(userDetails.getId(), biomarker, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/timeline")
    @Operation(summary = "Get unified health timeline", description = "Retrieve a chronologically merged feed of observations, vitals, symptoms, lifestyle logs, and lab reports")
    public ResponseEntity<ApiResponse<List<TimelineItemResponse>>> getTimeline(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @Parameter(description = "Filter by type: ALL, OBSERVATIONS, LABS, VITALS, LIFESTYLE, SYMPTOMS")
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "50") int limit) {
        verifyAuthenticated(userDetails);
        List<TimelineItemResponse> response = timelineService.getUnifiedTimeline(userDetails.getId(), from, to, type, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private void verifyAuthenticated(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
