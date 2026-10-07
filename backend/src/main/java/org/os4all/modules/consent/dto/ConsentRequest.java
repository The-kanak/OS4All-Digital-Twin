package org.os4all.modules.consent.dto;

import jakarta.validation.constraints.NotNull;
import org.os4all.modules.consent.entity.ConsentType;

public class ConsentRequest {

    @NotNull(message = "Consent type is required")
    private ConsentType consentType;

    @NotNull(message = "Granted status flag is required")
    private Boolean granted;

    public ConsentRequest() {
    }

    public ConsentRequest(ConsentType consentType, Boolean granted) {
        this.consentType = consentType;
        this.granted = granted;
    }

    public ConsentType getConsentType() {
        return consentType;
    }

    public void setConsentType(ConsentType consentType) {
        this.consentType = consentType;
    }

    public Boolean getGranted() {
        return granted;
    }

    public void setGranted(Boolean granted) {
        this.granted = granted;
    }
}
