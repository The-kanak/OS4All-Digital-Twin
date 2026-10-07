package org.os4all.modules.consent.dto;

import org.os4all.modules.consent.entity.ConsentType;

import java.time.Instant;
import java.util.UUID;

public class ConsentResponse {

    private UUID id;
    private ConsentType consentType;
    private boolean granted;
    private Instant grantedAt;
    private Instant revokedAt;

    public ConsentResponse() {
    }

    public ConsentResponse(UUID id, ConsentType consentType, boolean granted, Instant grantedAt, Instant revokedAt) {
        this.id = id;
        this.consentType = consentType;
        this.granted = granted;
        this.grantedAt = grantedAt;
        this.revokedAt = revokedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
}
