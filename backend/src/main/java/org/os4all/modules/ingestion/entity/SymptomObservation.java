package org.os4all.modules.ingestion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("SYMPTOM")
public class SymptomObservation extends HealthObservation {

    @Column(name = "symptom_name", length = 100)
    private String symptomName;

    @Column(name = "severity", length = 50)
    private String severity;

    @Column(name = "body_site", length = 100)
    private String bodySite;

    public SymptomObservation() {
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
