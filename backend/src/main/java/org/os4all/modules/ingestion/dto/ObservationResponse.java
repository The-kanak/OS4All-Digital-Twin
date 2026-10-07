package org.os4all.modules.ingestion.dto;

import org.os4all.modules.ingestion.entity.ObservationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ObservationResponse {

    private UUID id;
    private UUID userId;
    private ObservationType type;
    private String observationClass;
    private BigDecimal valueNumeric;
    private String valueText;
    private String unit;
    private BigDecimal standardValueNumeric;
    private String standardUnit;
    private Instant timestamp;
    private String source;
    private BigDecimal confidence;
    private String metadata;
    private Instant createdAt;

    // Vital
    private String vitalName;
    private String deviceModel;

    // Lifestyle
    private String lifestyleCategory;
    private Integer durationMinutes;

    // Symptom
    private String symptomName;
    private String severity;
    private String bodySite;

    public ObservationResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public ObservationType getType() {
        return type;
    }

    public void setType(ObservationType type) {
        this.type = type;
    }

    public String getObservationClass() {
        return observationClass;
    }

    public void setObservationClass(String observationClass) {
        this.observationClass = observationClass;
    }

    public BigDecimal getValueNumeric() {
        return valueNumeric;
    }

    public void setValueNumeric(BigDecimal valueNumeric) {
        this.valueNumeric = valueNumeric;
    }

    public String getValueText() {
        return valueText;
    }

    public void setValueText(String valueText) {
        this.valueText = valueText;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getStandardValueNumeric() {
        return standardValueNumeric;
    }

    public void setStandardValueNumeric(BigDecimal standardValueNumeric) {
        this.standardValueNumeric = standardValueNumeric;
    }

    public String getStandardUnit() {
        return standardUnit;
    }

    public void setStandardUnit(String standardUnit) {
        this.standardUnit = standardUnit;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getVitalName() {
        return vitalName;
    }

    public void setVitalName(String vitalName) {
        this.vitalName = vitalName;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getLifestyleCategory() {
        return lifestyleCategory;
    }

    public void setLifestyleCategory(String lifestyleCategory) {
        this.lifestyleCategory = lifestyleCategory;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getSymptomName() {
        return symptomName;
    }

    public void setSymptomName(String symptomName) {
        this.symptomName = symptomName;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getBodySite() {
        return bodySite;
    }

    public void setBodySite(String bodySite) {
        this.bodySite = bodySite;
    }
}
