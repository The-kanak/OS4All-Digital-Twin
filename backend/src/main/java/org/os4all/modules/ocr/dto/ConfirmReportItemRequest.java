package org.os4all.modules.ocr.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ConfirmReportItemRequest {

    private UUID id;
    private String biomarker;
    private BigDecimal value;
    private String unit;
    private BigDecimal referenceLow;
    private BigDecimal referenceHigh;
    private Boolean isConfirmed;

    public ConfirmReportItemRequest() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public Boolean getIsConfirmed() {
        return isConfirmed;
    }

    public void setIsConfirmed(Boolean isConfirmed) {
        this.isConfirmed = isConfirmed;
    }
}
