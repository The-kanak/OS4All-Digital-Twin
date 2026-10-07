package org.os4all.modules.ingestion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("VITAL")
public class VitalMeasurement extends HealthObservation {

    @Column(name = "vital_name", length = 100)
    private String vitalName;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    public VitalMeasurement() {
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
}
