package org.os4all.modules.lab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public class CreateLabResultItemRequest {

    @NotBlank(message = "Biomarker is required")
    private String biomarker;

    @NotNull(message = "Value is required")
    private BigDecimal value;

    @NotBlank(message = "Unit is required")
    private String unit;

    private BigDecimal referenceLow;
    private BigDecimal referenceHigh;
    private Instant collectionDate;
    private String sourceReport;
    private String notes;

    public CreateLabResultItemRequest() {
    }

    public String getBiomarker() {
        return biomarker;
    }

    public void setBiomarker(String biomarker) {
        this.biomarker = biomarker;
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
}
