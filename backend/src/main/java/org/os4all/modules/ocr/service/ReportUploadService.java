package org.os4all.modules.ocr.service;

import org.os4all.core.exception.ApiException;
import org.os4all.core.exception.ResourceNotFoundException;
import org.os4all.modules.audit.service.AuditService;
import org.os4all.modules.lab.dto.LabReportResponse;
import org.os4all.modules.lab.entity.LabReport;
import org.os4all.modules.lab.entity.LabResult;
import org.os4all.modules.lab.repository.LabReportRepository;
import org.os4all.modules.lab.repository.LabResultRepository;
import org.os4all.modules.lab.service.LabReportService;
import org.os4all.modules.normalization.UnitNormalizationService;
import org.os4all.modules.ocr.dto.ConfirmReportItemRequest;
import org.os4all.modules.ocr.dto.ConfirmReportRequest;
import org.os4all.modules.user.entity.User;
import org.os4all.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class ReportUploadService {

    private static final Logger log = LoggerFactory.getLogger(ReportUploadService.class);

    private final FileValidationAndStorageService storageService;
    private final LabReportOcrExtractor ocrExtractor;
    private final UnitNormalizationService normalizationService;
    private final LabReportRepository labReportRepository;
    private final LabResultRepository labResultRepository;
    private final UserRepository userRepository;
    private final LabReportService labReportService;
    private final AuditService auditService;

    public ReportUploadService(
            FileValidationAndStorageService storageService,
            LabReportOcrExtractor ocrExtractor,
            UnitNormalizationService normalizationService,
            LabReportRepository labReportRepository,
            LabResultRepository labResultRepository,
            UserRepository userRepository,
            LabReportService labReportService,
            AuditService auditService
    ) {
        this.storageService = storageService;
        this.ocrExtractor = ocrExtractor;
        this.normalizationService = normalizationService;
        this.labReportRepository = labReportRepository;
        this.labResultRepository = labResultRepository;
        this.userRepository = userRepository;
        this.labReportService = labReportService;
        this.auditService = auditService;
    }

    /**
     * Uploads, validates magic bytes, stores to isolated storage, executes OCR extraction,
     * checks confidence scores, and creates LabReport and LabResults.
     * Flags uncertain results for user review (never trusted silently).
     */
    @Transactional
    public LabReportResponse uploadAndProcessReport(UUID userId, MultipartFile file, String customTitle, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // 1. File validation and isolated storage
        FileValidationAndStorageService.StoredFileMetadata metadata = storageService.validateAndStore(file, userId);

        // 2. OCR and biomarker extraction
        File storedFile = new File(metadata.storedPath());
        LabReportOcrExtractor.ExtractionResult extraction = ocrExtractor.processReportFile(storedFile, metadata.mimeType());

        // 3. Create LabReport entity
        LabReport report = new LabReport();
        report.setUser(user);
        report.setReportTitle(customTitle != null && !customTitle.isBlank() ? customTitle.trim() : metadata.originalFilename());
        report.setLaboratoryName(extraction.laboratoryName());
        report.setCollectionDate(extraction.collectionDate() != null ? extraction.collectionDate() : Instant.now());
        report.setReportedDate(Instant.now());
        report.setSource("OCR_UPLOAD");
        report.setFileName(metadata.originalFilename());
        report.setFilePath(metadata.storedPath());
        report.setMimeType(metadata.mimeType());
        report.setFileSize(metadata.fileSize());
        report.setFileHash(metadata.sha256Hex());
        report.setOcrStatus("COMPLETED");
        report.setRawExtractedText(extraction.rawText());
        report.setOverallConfidence(extraction.overallConfidence());
        report.setReviewRequired(extraction.reviewRequired());

        // 4. Create LabResult entities
        for (LabReportOcrExtractor.ExtractedBiomarkerItem item : extraction.items()) {
            if (item.value() == null) {
                // Skip or create entry requiring review
                continue;
            }

            UnitNormalizationService.NormalizedBiomarker norm = normalizationService.normalizeBiomarker(
                    item.canonicalBiomarker(),
                    item.value(),
                    item.unit(),
                    item.referenceLow(),
                    item.referenceHigh()
            );

            LabResult result = new LabResult();
            result.setUser(user);
            result.setBiomarker(item.biomarker());
            result.setStandardizedBiomarker(norm.canonicalName());
            result.setValue(item.value());
            result.setUnit(item.unit());
            result.setStandardValue(norm.normalizedValue());
            result.setStandardUnit(norm.normalizedUnit());
            result.setReferenceLow(norm.referenceLow());
            result.setReferenceHigh(norm.referenceHigh());
            result.setCollectionDate(report.getCollectionDate());
            result.setSourceReport(report.getReportTitle());
            result.setExtractedValue(item.value().toPlainString());
            result.setConfidence(item.confidence());
            result.setSourceText(item.sourceText());
            result.setReviewRequired(item.reviewRequired());

            // Never silently convert uncertain OCR into trusted medical data!
            // If review is required, isConfirmed is false until explicitly confirmed by user.
            result.setIsConfirmed(!item.reviewRequired());
            if (item.reviewRequired()) {
                result.setNotes("PENDING USER REVIEW: " + item.reviewReason());
            }

            report.addResult(result);
        }

        LabReport saved = labReportRepository.save(report);

        auditService.record(
                user,
                user.getEmail(),
                "REPORT_OCR_UPLOADED",
                "LAB_REPORT",
                saved.getId().toString(),
                clientIp,
                "Uploaded: " + saved.getFileName() + ", Hash: " + saved.getFileHash() +
                        ", Confidence: " + saved.getOverallConfidence() + ", ReviewRequired: " + saved.getReviewRequired()
        );

        log.info("Report {} processed via OCR for user {}: extracted {} biomarkers, reviewRequired: {}",
                saved.getId(), userId, saved.getResults().size(), saved.getReviewRequired());

        return labReportService.mapToReportResponse(saved);
    }

    /**
     * Lists reports for user with pagination.
     */
    @Transactional(readOnly = true)
    public Page<LabReportResponse> getReports(UUID userId, Instant startDate, Instant endDate, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return labReportRepository.findFiltered(userId, startDate, endDate, pageable)
                .map(labReportService::mapToReportResponse);
    }

    /**
     * Gets single report by ID.
     */
    @Transactional(readOnly = true)
    public LabReportResponse getReportById(UUID userId, UUID reportId) {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));
        if (!report.getUser().getId().equals(userId)) {
            throw new ApiException("Access denied: You do not own this report");
        }
        return labReportService.mapToReportResponse(report);
    }

    /**
     * Confirms and validates uncertain OCR extraction results, updating any edited biomarker values/units
     * and marking them as confirmed so they safely enter the unified patient health timeline.
     */
    @Transactional
    public LabReportResponse confirmReport(UUID userId, UUID reportId, ConfirmReportRequest request, String clientIp) {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));

        if (!report.getUser().getId().equals(userId)) {
            throw new ApiException("Access denied: You do not own this report");
        }

        if (request.getReportTitle() != null && !request.getReportTitle().isBlank()) {
            report.setReportTitle(request.getReportTitle().trim());
        }
        if (request.getLaboratoryName() != null && !request.getLaboratoryName().isBlank()) {
            report.setLaboratoryName(request.getLaboratoryName().trim());
        }

        Map<UUID, ConfirmReportItemRequest> itemUpdates = new HashMap<>();
        if (request.getItems() != null) {
            for (ConfirmReportItemRequest itemReq : request.getItems()) {
                if (itemReq.getId() != null) {
                    itemUpdates.put(itemReq.getId(), itemReq);
                }
            }
        }

        for (LabResult res : report.getResults()) {
            ConfirmReportItemRequest update = itemUpdates.get(res.getId());
            if (update != null) {
                if (update.getBiomarker() != null && !update.getBiomarker().isBlank()) {
                    res.setBiomarker(update.getBiomarker().trim());
                }
                if (update.getValue() != null) {
                    res.setValue(update.getValue());
                }
                if (update.getUnit() != null && !update.getUnit().isBlank()) {
                    res.setUnit(update.getUnit().trim());
                }
                if (update.getReferenceLow() != null) {
                    res.setReferenceLow(update.getReferenceLow());
                }
                if (update.getReferenceHigh() != null) {
                    res.setReferenceHigh(update.getReferenceHigh());
                }
                if (update.getIsConfirmed() != null) {
                    res.setIsConfirmed(update.getIsConfirmed());
                } else {
                    res.setIsConfirmed(true);
                }
            } else {
                // If the entire report is confirmed, mark unmentioned existing items as confirmed
                res.setIsConfirmed(true);
            }

            // Normalization check with verified user values
            UnitNormalizationService.NormalizedBiomarker norm = normalizationService.normalizeBiomarker(
                    res.getBiomarker(),
                    res.getValue(),
                    res.getUnit(),
                    res.getReferenceLow(),
                    res.getReferenceHigh()
            );
            res.setStandardizedBiomarker(norm.canonicalName());
            res.setStandardValue(norm.normalizedValue());
            res.setStandardUnit(norm.normalizedUnit());
            res.setReviewRequired(false);
            res.setNotes(res.getNotes() != null ? res.getNotes().replace("PENDING USER REVIEW:", "[CONFIRMED]:") : "[USER CONFIRMED]");
            res.setConfidence(BigDecimal.valueOf(1.000));
        }

        report.setReviewRequired(false);
        report.setOverallConfidence(BigDecimal.valueOf(1.000));
        LabReport saved = labReportRepository.save(report);

        auditService.record(
                report.getUser(),
                report.getUser().getEmail(),
                "REPORT_CONFIRMED",
                "LAB_REPORT",
                saved.getId().toString(),
                clientIp,
                "Confirmed report: " + saved.getReportTitle() + ", Biomarkers confirmed: " + saved.getResults().size()
        );

        log.info("Report {} confirmed by user {}", reportId, userId);
        return labReportService.mapToReportResponse(saved);
    }
}
