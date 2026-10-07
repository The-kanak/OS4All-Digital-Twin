package org.os4all.modules.counselor.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "counselor_messages")
public class CounselorMessage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private CounselorConversation conversation;

    @Column(nullable = false, length = 32)
    private String role; // "USER", "ASSISTANT", "SYSTEM"

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "structured_json", columnDefinition = "TEXT")
    private String structuredJson;

    @Column(name = "is_emergency_flagged", nullable = false)
    private boolean isEmergencyFlagged = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CounselorMessage() {
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CounselorConversation getConversation() {
        return conversation;
    }

    public void setConversation(CounselorConversation conversation) {
        this.conversation = conversation;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStructuredJson() {
        return structuredJson;
    }

    public void setStructuredJson(String structuredJson) {
        this.structuredJson = structuredJson;
    }

    public boolean isEmergencyFlagged() {
        return isEmergencyFlagged;
    }

    public void setEmergencyFlagged(boolean emergencyFlagged) {
        isEmergencyFlagged = emergencyFlagged;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
