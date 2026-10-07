package org.os4all.modules.digitaltwin.fhir.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.os4all.core.common.ApiResponse;
import org.os4all.modules.digitaltwin.dto.PatientDetailDto;
import org.os4all.modules.digitaltwin.dto.PatientSummaryDto;
import org.os4all.modules.digitaltwin.fhir.dto.FhirParsedPatient;
import org.os4all.modules.digitaltwin.fhir.service.FhirImportService;
import org.os4all.modules.digitaltwin.service.DigitalTwinService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ehr")
@Tag(name = "Static EHR & Synthea FHIR API", description = "Endpoints for static EHR patient history, conditions, labs, medications, and Synthea FHIR import")
public class EhrController {

    private final DigitalTwinService digitalTwinService;
    private final FhirImportService fhirImportService;

    public EhrController(DigitalTwinService digitalTwinService, FhirImportService fhirImportService) {
        this.digitalTwinService = digitalTwinService;
        this.fhirImportService = fhirImportService;
    }

    @GetMapping("/patients")
    @Operation(summary = "List all EHR patients", description = "Returns synthetic longitudinal patients with static EHR profiles and active Digital Twin state.")
    public ResponseEntity<ApiResponse<List<PatientSummaryDto>>> getEhrPatients() {
        List<PatientSummaryDto> patients = digitalTwinService.getAllPatients();
        return ResponseEntity.ok(ApiResponse.success(patients));
    }

    @GetMapping("/patients/{patientId}")
    @Operation(summary = "Get patient EHR detail", description = "Returns comprehensive static EHR data including conditions, labs, and demographics.")
    public ResponseEntity<ApiResponse<PatientDetailDto>> getPatientEhrDetail(@PathVariable UUID patientId) {
        PatientDetailDto detail = digitalTwinService.getPatientDetail(patientId);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @GetMapping("/patients/{patientId}/conditions")
    @Operation(summary = "Get patient historical conditions", description = "Returns ICD-10 coded conditions and historical diagnoses.")
    public ResponseEntity<ApiResponse<List<PatientDetailDto.HistoricalRecordDto>>> getPatientConditions(@PathVariable UUID patientId) {
        PatientDetailDto detail = digitalTwinService.getPatientDetail(patientId);
        List<PatientDetailDto.HistoricalRecordDto> conditions = detail.historicalRecords().stream()
                .filter(r -> "CONDITION".equalsIgnoreCase(r.recordType()) || "PREVIOUS_DIAGNOSIS".equalsIgnoreCase(r.recordType()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(conditions));
    }

    @GetMapping("/patients/{patientId}/labs")
    @Operation(summary = "Get patient historical lab observations", description = "Returns standardized laboratory biomarkers (HbA1c, glucose, lipid profile, etc.).")
    public ResponseEntity<ApiResponse<List<PatientDetailDto.LatestLabBiomarkerDto>>> getPatientLabs(@PathVariable UUID patientId) {
        PatientDetailDto detail = digitalTwinService.getPatientDetail(patientId);
        return ResponseEntity.ok(ApiResponse.success(detail.recentBiomarkers()));
    }

    @GetMapping("/patients/{patientId}/medications")
    @Operation(summary = "Get patient prescribed medications", description = "Returns list of active and historical medications from EHR.")
    public ResponseEntity<ApiResponse<List<String>>> getPatientMedications(@PathVariable UUID patientId) {
        PatientDetailDto detail = digitalTwinService.getPatientDetail(patientId);
        List<String> meds = detail.historicalRecords().stream()
                .filter(r -> r.medications() != null && !r.medications().isBlank())
                .flatMap(r -> List.of(r.medications().split(",")).stream())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        return ResponseEntity.ok(ApiResponse.success(meds));
    }

    @GetMapping("/patients/{patientId}/history")
    @Operation(summary = "Get patient full medical history", description = "Returns historical records, ICD-10 codes, and clinical notes.")
    public ResponseEntity<ApiResponse<List<PatientDetailDto.HistoricalRecordDto>>> getPatientHistory(@PathVariable UUID patientId) {
        PatientDetailDto detail = digitalTwinService.getPatientDetail(patientId);
        return ResponseEntity.ok(ApiResponse.success(detail.historicalRecords()));
    }

    @GetMapping("/synthea/candidates")
    @Operation(summary = "Scan candidate Synthea FHIR patients", description = "Scans external Synthea FHIR bundle output and returns detected candidate metabolic profiles.")
    public ResponseEntity<ApiResponse<List<FhirParsedPatient>>> getSyntheaCandidates(
            @RequestParam(required = false) String customPath
    ) {
        List<FhirParsedPatient> candidates = fhirImportService.scanCandidatePatients(customPath);
        return ResponseEntity.ok(ApiResponse.success("Scanned " + candidates.size() + " Synthea FHIR candidate bundles.", candidates));
    }

    @PostMapping("/import/synthea")
    @Operation(summary = "Import Synthea FHIR bundles into Static EHR", description = "Parses and ingests synthetic diabetes & metabolic patient bundles into the OS4All database.")
    public ResponseEntity<ApiResponse<FhirImportService.ImportSummary>> importSynthea(
            @RequestBody(required = false) Map<String, String> body
    ) {
        String path = body != null ? body.get("path") : null;
        boolean metabolicOnly = body == null || !"false".equalsIgnoreCase(body.get("metabolicOnly"));
        FhirImportService.ImportSummary summary = fhirImportService.importSyntheaCohort(path, metabolicOnly);
        return ResponseEntity.ok(ApiResponse.success("Synthea FHIR ingestion complete.", summary));
    }
}
