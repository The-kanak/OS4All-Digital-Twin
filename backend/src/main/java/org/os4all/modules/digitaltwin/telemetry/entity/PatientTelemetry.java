package org.os4all.modules.digitaltwin.telemetry.entity;

import jakarta.persistence.*;
import org.os4all.modules.user.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a standardized 5-minute simulated wearable and CGM telemetry packet.
 * Captures live dynamic data stream for fusion with static Synthea EHR.
 */
@Entity
@Table(name = "patient_telemetry", indexes = {
        @Index(name = "idx_telemetry_user_ts", columnList = "user_id, timestamp DESC"),
        @Index(name = "idx_telemetry_user_scenario", columnList = "user_id, scenario")
})
public class PatientTelemetry {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "glucose", precision = 6, scale = 2)
    private BigDecimal glucose; // mg/dL

    @Column(name = "glucose_velocity", precision = 6, scale = 2)
    private BigDecimal glucoseVelocity; // mg/dL per minute or reading step

    @Column(name = "heart_rate", precision = 5, scale = 1)
    private BigDecimal heartRate; // bpm

    @Column(name = "hrv", precision = 5, scale = 1)
    private BigDecimal hrv; // ms (RMSSD / nocturnal HRV)

    @Column(name = "resting_heart_rate", precision = 5, scale = 1)
    private BigDecimal restingHeartRate; // bpm

    @Column(name = "sleep_duration_hours", precision = 4, scale = 2)
    private BigDecimal sleepDurationHours; // hours

    @Column(name = "sleep_quality_score", precision = 5, scale = 2)
    private BigDecimal sleepQualityScore; // 0.0 to 100.0

    @Column(name = "steps")
    private Integer steps;

    @Column(name = "activity_level", length = 50)
    private String activityLevel; // SEDENTARY, LIGHT, MODERATE, VIGOROUS

    @Column(name = "source", length = 100)
    private String source; // e.g. "SIMULATED_CGM_WEARABLE"

    @Column(name = "scenario", length = 50)
    private String scenario; // STABLE, POOR_SLEEP, HIGH_ACTIVITY, GLUCOSE_RISE, RECOVERY

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence = BigDecimal.valueOf(0.980);

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PatientTelemetry() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    public PatientTelemetry(User user, Instant timestamp) {
        this();
        this.user = user;
        this.timestamp = timestamp;
    }

    // Getters and setters

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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getGlucose() {
        return glucose;
    }

    public void setGlucose(BigDecimal glucose) {
        this.glucose = glucose;
    }

    public BigDecimal getGlucoseVelocity() {
        return glucoseVelocity;
    }

    public void setGlucoseVelocity(BigDecimal glucoseVelocity) {
        this.glucoseVelocity = glucoseVelocity;
    }

    public BigDecimal getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(BigDecimal heartRate) {
        this.heartRate = heartRate;
    }

    public BigDecimal getHrv() {
        return hrv;
    }

    public void setHrv(BigDecimal hrv) {
        this.hrv = hrv;
    }

    public BigDecimal getRestingHeartRate() {
        return restingHeartRate;
    }

    public void setRestingHeartRate(BigDecimal restingHeartRate) {
        this.restingHeartRate = restingHeartRate;
    }

    public BigDecimal getSleepDurationHours() {
        return sleepDurationHours;
    }

    public void setSleepDurationHours(BigDecimal sleepDurationHours) {
        this.sleepDurationHours = sleepDurationHours;
    }

    public BigDecimal getSleepQualityScore() {
        return sleepQualityScore;
    }

    public void setSleepQualityScore(BigDecimal sleepQualityScore) {
        this.sleepQualityScore = sleepQualityScore;
    }

    public Integer getSteps() {
        return steps;
    }

    public void setSteps(Integer steps) {
        this.steps = steps;
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
