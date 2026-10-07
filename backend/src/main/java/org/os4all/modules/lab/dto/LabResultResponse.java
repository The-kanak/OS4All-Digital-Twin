package org.os4all.modules.lab.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class LabResultResponse {

    private UUID id;
    private UUID labReportId;
    private String biomarker;
    private String standardizedBiomarker;
    private BigDecimal value;
    private String unit;
    private BigDecimal standardValue;
    private String standardUnit;
    private BigDecimal referenceLow;
    private BigDecimal referenceHigh;
    private Instant collectionDate;
    private String sourceReport;
    private String notes;
    private String extractedValue;
    private BigDecimal confidence;
    private String sourceText;
    private Boolean reviewRequired;
    private Boolean isConfirmed;
    private Instant createdAt;

    public LabResultResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLabReportId() {
        return labReportId;
    }

    public void setLabReportId(UUID labReportId) {
        this.labReportId = labReportId;
    }

    public String getBiomarker() {
        return biomarker;
    }

    public void setBiomarker(String biomarker) {
        this.biomarker = biomarker;
    }

    public String getStandardizedBiomarker() {
        return standardizedBiomarker;
    }

    public void setStandardizedBiomarker(String standardizedBiomarker) {
        this.standardizedBiomarker = standardizedBiomarker;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getStandardValue() {
        return standardValue;
    }

    public void setStandardValue(BigDecimal standardValue) {
        this.standardValue = standardValue;
    }

    public String getStandardUnit() {
        return standardUnit;
    }

    public void setStandardUnit(String standardUnit) {
        this.standardUnit = standardUnit;
    }

    public BigDecimal getReferenceLow() {
        return referenceLow;
    }

    public void setReferenceLow(BigDecimal referenceLow) {
        this.referenceLow = referenceLow;
    }

    public BigDecimal getReferenceHigh() {
        return referenceHigh;
    }

    public void setReferenceHigh(BigDecimal referenceHigh) {
        this.referenceHigh = referenceHigh;
    }

    public Instant getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(Instant collectionDate) {
        this.collectionDate = collectionDate;
    }

    public String getSourceReport() {
        return sourceReport;
    }

    public void setSourceReport(String sourceReport) {
        this.sourceReport = sourceReport;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getExtractedValue() {
        return extractedValue;
    }

    public void setExtractedValue(String extractedValue) {
        this.extractedValue = extractedValue;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public Boolean getReviewRequired() {
        return reviewRequired;
    }

    public void setReviewRequired(Boolean reviewRequired) {
        this.reviewRequired = reviewRequired;
    }

    public Boolean getIsConfirmed() {
        return isConfirmed;
    }

    public void setIsConfirmed(Boolean isConfirmed) {
        this.isConfirmed = isConfirmed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
