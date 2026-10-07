package org.os4all.modules.ingestion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("LIFESTYLE")
public class LifestyleObservation extends HealthObservation {

    @Column(name = "lifestyle_category", length = 100)
    private String lifestyleCategory;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    public LifestyleObservation() {
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
}
