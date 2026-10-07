package org.os4all.modules.digitaltwin.entity;

import jakarta.persistence.*;
import org.os4all.modules.user.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "digital_twin_states")
public class DigitalTwinState {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "twin_state", nullable = false, length = 50)
    private TwinStateType twinState;

    @Column(name = "overall_risk_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal overallRiskScore;

    @Column(name = "metabolic_risk_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal metabolicRiskScore;

    @Column(name = "glucose_spike_probability", precision = 5, scale = 2, nullable = false)
    private BigDecimal glucoseSpikeProbability;

    @Column(name = "prediction_horizon", length = 50)
    private String predictionHorizon;

    @Column(name = "confidence", precision = 4, scale = 3, nullable = false)
    private BigDecimal confidence;

    @Column(name = "state_drivers", columnDefinition = "TEXT")
    private String stateDrivers; // JSON or comma-separated driver list

    @Column(name = "physiological_state_summary", columnDefinition = "TEXT")
    private String physiologicalStateSummary;

    @Column(name = "last_simulation_scenario", length = 100)
    private String lastSimulationScenario;

    @Column(name = "projected_glucose_120min", precision = 5, scale = 1)
    private BigDecimal projectedGlucose120Min;

    @Column(name = "glucose_velocity", precision = 5, scale = 2)
    private BigDecimal glucoseVelocity;

    @Column(name = "trajectory_direction", length = 50)
    private String trajectoryDirection;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public DigitalTwinState() {}

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.lastUpdatedAt == null) {
            this.lastUpdatedAt = now;
        }
        if (this.twinState == null) {
            this.twinState = TwinStateType.STABLE;
        }
        if (this.overallRiskScore == null) {
            this.overallRiskScore = BigDecimal.ZERO;
        }
        if (this.metabolicRiskScore == null) {
            this.metabolicRiskScore = BigDecimal.ZERO;
        }
        if (this.glucoseSpikeProbability == null) {
            this.glucoseSpikeProbability = BigDecimal.ZERO;
        }
        if (this.predictionHorizon == null) {
            this.predictionHorizon = "Next 2 Hours";
        }
        if (this.confidence == null) {
            this.confidence = new BigDecimal("0.920");
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public TwinStateType getTwinState() { return twinState; }
    public void setTwinState(TwinStateType twinState) { this.twinState = twinState; }

    public BigDecimal getOverallRiskScore() { return overallRiskScore; }
    public void setOverallRiskScore(BigDecimal overallRiskScore) { this.overallRiskScore = overallRiskScore; }

    public BigDecimal getMetabolicRiskScore() { return metabolicRiskScore; }
    public void setMetabolicRiskScore(BigDecimal metabolicRiskScore) { this.metabolicRiskScore = metabolicRiskScore; }

    public BigDecimal getGlucoseSpikeProbability() { return glucoseSpikeProbability; }
    public void setGlucoseSpikeProbability(BigDecimal glucoseSpikeProbability) { this.glucoseSpikeProbability = glucoseSpikeProbability; }

    public String getPredictionHorizon() { return predictionHorizon; }
    public void setPredictionHorizon(String predictionHorizon) { this.predictionHorizon = predictionHorizon; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    public String getStateDrivers() { return stateDrivers; }
    public void setStateDrivers(String stateDrivers) { this.stateDrivers = stateDrivers; }

    public String getPhysiologicalStateSummary() { return physiologicalStateSummary; }
    public void setPhysiologicalStateSummary(String physiologicalStateSummary) { this.physiologicalStateSummary = physiologicalStateSummary; }

    public String getLastSimulationScenario() { return lastSimulationScenario; }
    public void setLastSimulationScenario(String lastSimulationScenario) { this.lastSimulationScenario = lastSimulationScenario; }

    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public BigDecimal getProjectedGlucose120Min() { return projectedGlucose120Min; }
    public void setProjectedGlucose120Min(BigDecimal projectedGlucose120Min) { this.projectedGlucose120Min = projectedGlucose120Min; }

    public BigDecimal getGlucoseVelocity() { return glucoseVelocity; }
    public void setGlucoseVelocity(BigDecimal glucoseVelocity) { this.glucoseVelocity = glucoseVelocity; }

    public String getTrajectoryDirection() { return trajectoryDirection; }
    public void setTrajectoryDirection(String trajectoryDirection) { this.trajectoryDirection = trajectoryDirection; }
}
