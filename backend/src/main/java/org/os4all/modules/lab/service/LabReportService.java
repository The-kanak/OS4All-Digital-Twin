package org.os4all.modules.lab.service;

import org.os4all.core.exception.ApiException;
import org.os4all.core.exception.ResourceNotFoundException;
import org.os4all.modules.audit.service.AuditService;
import org.os4all.modules.lab.dto.*;
import org.os4all.modules.lab.entity.LabReport;
import org.os4all.modules.lab.entity.LabResult;
import org.os4all.modules.lab.repository.LabReportRepository;
import org.os4all.modules.lab.repository.LabResultRepository;
import org.os4all.modules.normalization.UnitNormalizationService;
import org.os4all.modules.user.entity.User;
import org.os4all.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class LabReportService {

    private static final Logger log = LoggerFactory.getLogger(LabReportService.class);

    private final LabReportRepository labReportRepository;
    private final LabResultRepository labResultRepository;
    private final UserRepository userRepository;
    private final UnitNormalizationService normalizationService;
    private final AuditService auditService;

    public LabReportService(
            LabReportRepository labReportRepository,
            LabResultRepository labResultRepository,
            UserRepository userRepository,
            UnitNormalizationService normalizationService,
            AuditService auditService
    ) {
        this.labReportRepository = labReportRepository;
        this.labResultRepository = labResultRepository;
        this.userRepository = userRepository;
        this.normalizationService = normalizationService;
        this.auditService = auditService;
    }

    @Transactional
    public LabReportResponse recordLabReport(UUID userId, CreateLabReportRequest request, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (request.getResults() == null || request.getResults().isEmpty()) {
            throw new ApiException("A lab report must contain at least one biomarker result");
        }

        LabReport report = new LabReport();
        report.setUser(user);
        report.setReportTitle(request.getReportTitle());
        report.setLaboratoryName(request.getLaboratoryName());
        report.setCollectionDate(request.getCollectionDate());
        report.setReportedDate(request.getReportedDate());
        report.setSource(request.getSource());
        report.setNotes(request.getNotes());

        for (CreateLabResultItemRequest itemReq : request.getResults()) {
            if (itemReq.getValue() == null) {
                throw new ApiException("Biomarker value cannot be null for biomarker: " + itemReq.getBiomarker());
            }

            UnitNormalizationService.NormalizedBiomarker norm = normalizationService.normalizeBiomarker(
                    itemReq.getBiomarker(),
                    itemReq.getValue(),
                    itemReq.getUnit(),
                    itemReq.getReferenceLow(),
                    itemReq.getReferenceHigh()
            );

            LabResult result = new LabResult();
            result.setUser(user);
            result.setBiomarker(itemReq.getBiomarker());
            result.setStandardizedBiomarker(norm.canonicalName());
            result.setValue(itemReq.getValue());
            result.setUnit(itemReq.getUnit());
            result.setStandardValue(norm.normalizedValue());
            result.setStandardUnit(norm.normalizedUnit());
            result.setReferenceLow(norm.referenceLow());
            result.setReferenceHigh(norm.referenceHigh());
            result.setCollectionDate(itemReq.getCollectionDate() != null ? itemReq.getCollectionDate() : request.getCollectionDate());
            result.setSourceReport(itemReq.getSourceReport() != null ? itemReq.getSourceReport() : request.getReportTitle());
            result.setNotes(itemReq.getNotes());

            report.addResult(result);
        }

        LabReport saved = labReportRepository.save(report);

        auditService.record(
                user,
                user.getEmail(),
                "LAB_REPORT_RECORDED",
                "LAB_REPORT",
                saved.getId().toString(),
                clientIp,
                "Report: " + saved.getReportTitle() + ", Biomarkers: " + saved.getResults().size()
        );

        log.info("Recorded lab report {} with {} results for user {}", saved.getId(), saved.getResults().size(), userId);
        return mapToReportResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<LabReportResponse> getLabReports(
            UUID userId,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    ) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return labReportRepository.findFiltered(userId, startDate, endDate, pageable)
                .map(this::mapToReportResponse);
    }

    @Transactional(readOnly = true)
    public Page<LabResultResponse> getLabResults(
            UUID userId,
            String biomarker,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    ) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return labResultRepository.findFiltered(userId, biomarker, startDate, endDate, pageable)
                .map(this::mapToResultResponse);
    }

    public LabReportResponse mapToReportResponse(LabReport report) {
        LabReportResponse resp = new LabReportResponse();
        resp.setId(report.getId());
        resp.setUserId(report.getUser().getId());
        resp.setReportTitle(report.getReportTitle());
        resp.setLaboratoryName(report.getLaboratoryName());
        resp.setCollectionDate(report.getCollectionDate());
        resp.setReportedDate(report.getReportedDate());
        resp.setSource(report.getSource());
        resp.setNotes(report.getNotes());
        resp.setFileName(report.getFileName());
        resp.setFilePath(report.getFilePath());
        resp.setMimeType(report.getMimeType());
        resp.setFileSize(report.getFileSize());
        resp.setFileHash(report.getFileHash());
        resp.setOcrStatus(report.getOcrStatus());
        resp.setRawExtractedText(report.getRawExtractedText());
        resp.setOverallConfidence(report.getOverallConfidence());
        resp.setReviewRequired(report.getReviewRequired());
        resp.setCreatedAt(report.getCreatedAt());

        if (report.getResults() != null) {
            resp.setResults(report.getResults().stream().map(this::mapToResultResponse).toList());
        }

        return resp;
    }

    public LabResultResponse mapToResultResponse(LabResult res) {
        LabResultResponse resp = new LabResultResponse();
        resp.setId(res.getId());
        if (res.getLabReport() != null) {
            resp.setLabReportId(res.getLabReport().getId());
        }
        resp.setBiomarker(res.getBiomarker());
        resp.setStandardizedBiomarker(res.getStandardizedBiomarker());
        resp.setValue(res.getValue());
        resp.setUnit(res.getUnit());
        resp.setStandardValue(res.getStandardValue());
        resp.setStandardUnit(res.getStandardUnit());
        resp.setReferenceLow(res.getReferenceLow());
        resp.setReferenceHigh(res.getReferenceHigh());
        resp.setCollectionDate(res.getCollectionDate());
        resp.setSourceReport(res.getSourceReport());
        resp.setNotes(res.getNotes());
        resp.setExtractedValue(res.getExtractedValue());
        resp.setConfidence(res.getConfidence());
        resp.setSourceText(res.getSourceText());
        resp.setReviewRequired(res.getReviewRequired());
        resp.setIsConfirmed(res.getIsConfirmed());
        resp.setCreatedAt(res.getCreatedAt());
        return resp;
    }
}
