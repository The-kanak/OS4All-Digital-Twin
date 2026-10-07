package org.os4all.modules.consent.entity;

import jakarta.persistence.*;
import org.os4all.modules.user.entity.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_consents", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "consent_type"})
})
public class UserConsent {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    private ConsentType consentType;

    @Column(name = "is_granted", nullable = false)
    private boolean granted;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UserConsent() {
    }

    public UserConsent(User user, ConsentType consentType, boolean granted, String ipAddress) {
        this.user = user;
        this.consentType = consentType;
        this.granted = granted;
        this.ipAddress = ipAddress;
        Instant now = Instant.now();
        if (granted) {
            this.grantedAt = now;
        } else {
            this.revokedAt = now;
        }
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ConsentType getConsentType() {
        return consentType;
    }

    public void setConsentType(ConsentType consentType) {
        this.consentType = consentType;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
        Instant now = Instant.now();
        if (granted) {
            this.grantedAt = now;
            this.revokedAt = null;
        } else {
            this.revokedAt = now;
        }
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
