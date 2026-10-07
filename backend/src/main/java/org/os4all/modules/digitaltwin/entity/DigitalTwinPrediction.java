package org.os4all.modules.digitaltwin.entity;

import jakarta.persistence.*;
import org.os4all.modules.user.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "digital_twin_predictions")
public class DigitalTwinPrediction {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "prediction_type", nullable = false, length = 100)
    private String predictionType; // GLUCOSE_SPIKE_2H, METABOLIC_DRIFT_24H

    @Column(name = "horizon_window", nullable = false, length = 50)
    private String horizonWindow; // "Next 2 Hours"

    @Column(name = "risk_level", nullable = false, length = 50)
    private String riskLevel; // LOW, MODERATE, HIGH, CRITICAL

    @Column(name = "probability", precision = 5, scale = 2, nullable = false)
    private BigDecimal probability; // 0.00 to 100.00

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "headline", nullable = false)
    private String headline;

    @Column(name = "clinical_explanation", columnDefinition = "TEXT", nullable = false)
    private String clinicalExplanation;

    @Column(name = "top_contributing_factors", columnDefinition = "TEXT")
    private String topContributingFactors; // JSON list of contributing features

    @Column(name = "historical_evidence", columnDefinition = "TEXT")
    private String historicalEvidence;

    @Column(name = "recommended_clinical_actions", columnDefinition = "TEXT")
    private String recommendedClinicalActions;

    @Column(name = "confidence", precision = 4, scale = 3, nullable = false)
    private BigDecimal confidence;

    @Column(name = "current_glucose", precision = 5, scale = 1)
    private BigDecimal currentGlucose;

    @Column(name = "glucose_velocity", precision = 5, scale = 2)
    private BigDecimal glucoseVelocity;

    @Column(name = "projected_glucose_120min", precision = 5, scale = 1)
    private BigDecimal projectedGlucose120Min;

    @Column(name = "projected_delta", precision = 5, scale = 1)
    private BigDecimal projectedDelta;

    @Column(name = "trajectory_direction", length = 50)
    private String trajectoryDirection;

    @Column(name = "trajectory_points_json", columnDefinition = "TEXT")
    private String trajectoryPointsJson;

    @Column(name = "is_simulation", nullable = false)
    private boolean isSimulation;

    @Column(name = "disclaimer", nullable = false)
    private String disclaimer = "Research / Hackathon Prototype — Not a Medical Diagnosis.";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public DigitalTwinPrediction() {}

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.disclaimer == null) {
            this.disclaimer = "Research / Hackathon Prototype — Not a Medical Diagnosis.";
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getPredictionType() { return predictionType; }
    public void setPredictionType(String predictionType) { this.predictionType = predictionType; }

    public String getHorizonWindow() { return horizonWindow; }
    public void setHorizonWindow(String horizonWindow) { this.horizonWindow = horizonWindow; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public BigDecimal getProbability() { return probability; }
    public void setProbability(BigDecimal probability) { this.probability = probability; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getClinicalExplanation() { return clinicalExplanation; }
    public void setClinicalExplanation(String clinicalExplanation) { this.clinicalExplanation = clinicalExplanation; }

    public String getTopContributingFactors() { return topContributingFactors; }
    public void setTopContributingFactors(String topContributingFactors) { this.topContributingFactors = topContributingFactors; }

    public String getHistoricalEvidence() { return historicalEvidence; }
    public void setHistoricalEvidence(String historicalEvidence) { this.historicalEvidence = historicalEvidence; }

    public String getRecommendedClinicalActions() { return recommendedClinicalActions; }
    public void setRecommendedClinicalActions(String recommendedClinicalActions) { this.recommendedClinicalActions = recommendedClinicalActions; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    public boolean isSimulation() { return isSimulation; }
    public void setSimulation(boolean simulation) { isSimulation = simulation; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public BigDecimal getCurrentGlucose() { return currentGlucose; }
    public void setCurrentGlucose(BigDecimal currentGlucose) { this.currentGlucose = currentGlucose; }

    public BigDecimal getGlucoseVelocity() { return glucoseVelocity; }
    public void setGlucoseVelocity(BigDecimal glucoseVelocity) { this.glucoseVelocity = glucoseVelocity; }

    public BigDecimal getProjectedGlucose120Min() { return projectedGlucose120Min; }
    public void setProjectedGlucose120Min(BigDecimal projectedGlucose120Min) { this.projectedGlucose120Min = projectedGlucose120Min; }

    public BigDecimal getProjectedDelta() { return projectedDelta; }
    public void setProjectedDelta(BigDecimal projectedDelta) { this.projectedDelta = projectedDelta; }

    public String getTrajectoryDirection() { return trajectoryDirection; }
    public void setTrajectoryDirection(String trajectoryDirection) { this.trajectoryDirection = trajectoryDirection; }

    public String getTrajectoryPointsJson() { return trajectoryPointsJson; }
    public void setTrajectoryPointsJson(String trajectoryPointsJson) { this.trajectoryPointsJson = trajectoryPointsJson; }
}
