package org.os4all.modules.ai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.os4all.modules.user.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_inference_logs")
public class AiInferenceLog {

    @Id
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "workflow_id", length = 100)
    private String workflowId;

    @Column(name = "input_context_version", length = 50)
    private String inputContextVersion;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "workflow_state", nullable = false, length = 50)
    private String workflowState;

    @JsonIgnore
    @Column(name = "anonymized_prompt", nullable = false, columnDefinition = "TEXT")
    private String anonymizedPrompt;

    @JsonIgnore
    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "structured_summary", columnDefinition = "TEXT")
    private String structuredSummary;

    @Column(name = "evidence_used", columnDefinition = "TEXT")
    private String evidenceUsed;

    @Column(name = "urgency", length = 50)
    private String urgency;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "SUCCESS";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AiInferenceLog() {
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

    // Getters & Setters

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

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getInputContextVersion() {
        return inputContextVersion;
    }

    public void setInputContextVersion(String inputContextVersion) {
        this.inputContextVersion = inputContextVersion;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getWorkflowState() {
        return workflowState;
    }

    public void setWorkflowState(String workflowState) {
        this.workflowState = workflowState;
    }

    public String getAnonymizedPrompt() {
        return anonymizedPrompt;
    }

    public void setAnonymizedPrompt(String anonymizedPrompt) {
        this.anonymizedPrompt = anonymizedPrompt;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public String getStructuredSummary() {
        return structuredSummary;
    }

    public void setStructuredSummary(String structuredSummary) {
        this.structuredSummary = structuredSummary;
    }

    public String getEvidenceUsed() {
        return evidenceUsed;
    }

    public void setEvidenceUsed(String evidenceUsed) {
        this.evidenceUsed = evidenceUsed;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
