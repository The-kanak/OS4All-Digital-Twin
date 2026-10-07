package org.os4all.modules.lab.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.os4all.modules.user.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lab_results")
public class LabResult {

    @Id
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_report_id")
    private LabReport labReport;

    @Column(name = "biomarker", nullable = false, length = 100)
    private String biomarker;

    @Column(name = "standardized_biomarker", nullable = false, length = 100)
    private String standardizedBiomarker;

    @Column(name = "\"value\"", nullable = false, precision = 12, scale = 4)
    private BigDecimal value;

    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    @Column(name = "standard_value", precision = 12, scale = 4)
    private BigDecimal standardValue;

    @Column(name = "standard_unit", length = 50)
    private String standardUnit;

    @Column(name = "reference_low", precision = 12, scale = 4)
    private BigDecimal referenceLow;

    @Column(name = "reference_high", precision = 12, scale = 4)
    private BigDecimal referenceHigh;

    @Column(name = "collection_date", nullable = false)
    private Instant collectionDate;

    @Column(name = "source_report", length = 255)
    private String sourceReport;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "extracted_value", length = 100)
    private String extractedValue;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence = BigDecimal.valueOf(1.000);

    @Column(name = "source_text", columnDefinition = "TEXT")
    private String sourceText;

    @Column(name = "review_required")
    private Boolean reviewRequired = false;

    @Column(name = "is_confirmed")
    private Boolean isConfirmed = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public LabResult() {
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LabReport getLabReport() {
        return labReport;
    }

    public void setLabReport(LabReport labReport) {
        this.labReport = labReport;
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
