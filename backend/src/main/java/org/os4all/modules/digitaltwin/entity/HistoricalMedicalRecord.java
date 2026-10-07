package org.os4all.modules.digitaltwin.entity;

import jakarta.persistence.*;
import org.os4all.modules.user.entity.User;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "historical_medical_records")
public class HistoricalMedicalRecord {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "record_type", nullable = false, length = 100)
    private String recordType; // CONDITION, FAMILY_HISTORY, MEDICATION, PREVIOUS_DIAGNOSIS, ALLERGY

    @Column(name = "condition_or_diagnosis", nullable = false)
    private String conditionOrDiagnosis;

    @Column(name = "icd10_code", length = 20)
    private String icd10Code;

    @Column(name = "severity", length = 50)
    private String severity; // MILD, MODERATE, SEVERE, CONTROLLED

    @Column(name = "status", nullable = false, length = 50)
    private String status; // ACTIVE, RESOLVED, CHRONIC

    @Column(name = "diagnosed_date")
    private LocalDate diagnosedDate;

    @Column(name = "medications", columnDefinition = "TEXT")
    private String medications;

    @Column(name = "family_history_notes", columnDefinition = "TEXT")
    private String familyHistoryNotes;

    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public HistoricalMedicalRecord() {}

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getConditionOrDiagnosis() { return conditionOrDiagnosis; }
    public void setConditionOrDiagnosis(String conditionOrDiagnosis) { this.conditionOrDiagnosis = conditionOrDiagnosis; }

    public String getIcd10Code() { return icd10Code; }
    public void setIcd10Code(String icd10Code) { this.icd10Code = icd10Code; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getDiagnosedDate() { return diagnosedDate; }
    public void setDiagnosedDate(LocalDate diagnosedDate) { this.diagnosedDate = diagnosedDate; }

    public String getMedications() { return medications; }
    public void setMedications(String medications) { this.medications = medications; }

    public String getFamilyHistoryNotes() { return familyHistoryNotes; }
    public void setFamilyHistoryNotes(String familyHistoryNotes) { this.familyHistoryNotes = familyHistoryNotes; }

    public String getClinicalNotes() { return clinicalNotes; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
