package org.os4all.modules.ocr.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.os4all.core.common.ApiResponse;
import org.os4all.core.exception.UnauthorizedException;
import org.os4all.core.security.CustomUserDetails;
import org.os4all.modules.lab.dto.LabReportResponse;
import org.os4all.modules.ocr.dto.ConfirmReportRequest;
import org.os4all.modules.ocr.service.ReportUploadService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Health Reports OCR & Ingestion", description = "Endpoints for uploading medical PDF/image reports, reviewing extracted biomarkers, and confirming data")
@SecurityRequirement(name = "BearerAuth")
public class ReportUploadController {

    private final ReportUploadService reportUploadService;

    public ReportUploadController(ReportUploadService reportUploadService) {
        this.reportUploadService = reportUploadService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a medical lab report (PDF/PNG/JPG) for OCR parsing and biomarker extraction")
    public ResponseEntity<ApiResponse<LabReportResponse>> uploadReport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request
    ) {
        verifyAuthenticated(userDetails);
        String clientIp = request.getRemoteAddr();
        LabReportResponse response = reportUploadService.uploadAndProcessReport(
                userDetails.getId(),
                file,
                title,
                clientIp
        );
        return ResponseEntity.ok(ApiResponse.success(
                response.getReviewRequired()
                        ? "Report uploaded and parsed with OCR. Attention: Low confidence biomarkers require confirmation before entering timeline."
                        : "Report uploaded and parsed successfully.",
                response
        ));
    }

    @GetMapping
    @Operation(summary = "Get list of ingested health reports with OCR extraction and review status")
    public ResponseEntity<ApiResponse<Page<LabReportResponse>>> getReports(
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        verifyAuthenticated(userDetails);
        Page<LabReportResponse> reports = reportUploadService.getReports(
                userDetails.getId(),
                startDate,
                endDate,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "collectionDate"))
        );
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed report with extracted biomarkers, confidence scores, and review flags")
    public ResponseEntity<ApiResponse<LabReportResponse>> getReportById(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        verifyAuthenticated(userDetails);
        LabReportResponse report = reportUploadService.getReportById(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm or adjust extracted biomarker values/units to verify uncertain OCR data before medical timeline integration")
    public ResponseEntity<ApiResponse<LabReportResponse>> confirmReport(
            @PathVariable UUID id,
            @RequestBody ConfirmReportRequest confirmRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request
    ) {
        verifyAuthenticated(userDetails);
        String clientIp = request.getRemoteAddr();
        LabReportResponse response = reportUploadService.confirmReport(
                userDetails.getId(),
                id,
                confirmRequest,
                clientIp
        );
        return ResponseEntity.ok(ApiResponse.success("Report confirmed and safely incorporated into medical timeline.", response));
    }

    private void verifyAuthenticated(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
    }
}
