package org.os4all.modules.digitaltwin.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MetabolicPredictionEngine {

    public record TrajectoryPoint(
            int minuteOffset,
            BigDecimal projectedGlucose,
            String trendDirection
    ) {}

    public record GlucoseTrajectoryProjection(
            BigDecimal currentGlucose,
            BigDecimal glucoseVelocityMgDlPerMin,
            BigDecimal projectedGlucose120Min,
            BigDecimal projectedDeltaMgDl,
            String trajectoryDirection, // RISING, FALLING, STABLE
            String horizonWindow,       // "Next 2 Hours"
            List<TrajectoryPoint> trajectoryPoints,
            BigDecimal confidence,
            String disclaimer
    ) {}

    public record EngineeredFeatures(
            BigDecimal meanGlucose,
            BigDecimal glucoseSlope, // mg/dL per hour or per reading step
            BigDecimal glucoseVariabilityCv, // Coefficient of Variation in %
            BigDecimal hrvDeviationPct, // % deviation from baseline (negative = deficit)
            Double hrvZScore,
            BigDecimal restingHrDeviationBpm,
            Double restingHrZScore,
            BigDecimal sleepDeficitHours, // positive means deficit vs baseline
            BigDecimal stepDeviationPct,
            BigDecimal currentBmi,
            BigDecimal hba1c,
            BigDecimal fastingGlucose,
            int hourOfDay,
            boolean isPostPrandialWindow,
            boolean hasDiagnosedT2D,
            String activityLevel,
            BigDecimal glucoseVelocityMgDlPerMin,
            BigDecimal baselineGlucoseMean,
            BigDecimal sensorConfidence,
            Instant telemetryTimestamp
    ) {
        // Constructor with 16 params (backward compatibility)
        public EngineeredFeatures(
                BigDecimal meanGlucose,
                BigDecimal glucoseSlope,
                BigDecimal glucoseVariabilityCv,
                BigDecimal hrvDeviationPct,
                Double hrvZScore,
                BigDecimal restingHrDeviationBpm,
                Double restingHrZScore,
                BigDecimal sleepDeficitHours,
                BigDecimal stepDeviationPct,
                BigDecimal currentBmi,
                BigDecimal hba1c,
                BigDecimal fastingGlucose,
                int hourOfDay,
                boolean isPostPrandialWindow,
                boolean hasDiagnosedT2D,
                String activityLevel
        ) {
            this(meanGlucose, glucoseSlope, glucoseVariabilityCv, hrvDeviationPct, hrvZScore,
                    restingHrDeviationBpm, restingHrZScore, sleepDeficitHours, stepDeviationPct,
                    currentBmi, hba1c, fastingGlucose, hourOfDay, isPostPrandialWindow,
                    hasDiagnosedT2D, activityLevel,
                    glucoseSlope != null ? glucoseSlope.divide(BigDecimal.valueOf(5.0), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                    fastingGlucose != null ? fastingGlucose : new BigDecimal("95.0"),
                    BigDecimal.valueOf(0.95), Instant.now());
        }

        // Constructor with 14 params (backward compatibility)
        public EngineeredFeatures(
                BigDecimal meanGlucose,
                BigDecimal glucoseSlope,
                BigDecimal glucoseVariabilityCv,
                BigDecimal hrvDeviationPct,
                Double hrvZScore,
                BigDecimal restingHrDeviationBpm,
                Double restingHrZScore,
                BigDecimal sleepDeficitHours,
                BigDecimal stepDeviationPct,
                BigDecimal currentBmi,
                BigDecimal hba1c,
                BigDecimal fastingGlucose,
                int hourOfDay,
                boolean isPostPrandialWindow
        ) {
            this(meanGlucose, glucoseSlope, glucoseVariabilityCv, hrvDeviationPct, hrvZScore,
                    restingHrDeviationBpm, restingHrZScore, sleepDeficitHours, stepDeviationPct,
                    currentBmi, hba1c, fastingGlucose, hourOfDay, isPostPrandialWindow,
                    false, "MODERATE");
        }
    }

    public record PredictionOutput(
            String predictionType,
            String horizonWindow,
            String riskLevel, // LOW, MODERATE, HIGH, CRITICAL
            BigDecimal spikeProbability, // 0.00 to 100.00
            BigDecimal metabolicRiskScore, // 0.00 to 100.00
            BigDecimal overallRiskScore, // 0.00 to 100.00
            String twinState, // STABLE, PRE_SYMPTOMATIC_DRIFT, ELEVATED_RISK, ACTIVE_ANOMALY
            String headline,
            String clinicalExplanation,
            List<FeatureContribution> contributions,
            List<String> stateDrivers,
            BigDecimal confidence,
            GlucoseTrajectoryProjection trajectoryProjection
    ) {
        public PredictionOutput(
                String predictionType,
                String horizonWindow,
                String riskLevel,
                BigDecimal spikeProbability,
                BigDecimal metabolicRiskScore,
                BigDecimal overallRiskScore,
                String twinState,
                String headline,
                String clinicalExplanation,
                List<FeatureContribution> contributions,
                List<String> stateDrivers,
                BigDecimal confidence
        ) {
            this(predictionType, horizonWindow, riskLevel, spikeProbability, metabolicRiskScore,
                    overallRiskScore, twinState, headline, clinicalExplanation, contributions,
                    stateDrivers, confidence, null);
        }
    }

    public record FeatureContribution(
            String featureName,
            BigDecimal weight,
            String description,
            String impactDirection // RISK_INCREASING, PROTECTIVE, NEUTRAL
    ) {}

    /**
     * Deterministic, explainable scoring engine for glucose spike and metabolic risk.
     * Combines:
     * - Glucose trajectory (slope & recent levels)
     * - Autonomic tone (HRV suppression & resting HR elevation)
     * - Sleep debt (impaired insulin sensitivity)
     * - Sedentary / low activity deficit
     * - Static risk (BMI, baseline HbA1c, fasting glucose)
     */
    public static PredictionOutput predict(EngineeredFeatures features) {
        double score = 10.0; // Base baseline probability
        List<FeatureContribution> contributions = new ArrayList<>();
        List<String> drivers = new ArrayList<>();

        // 1. Glucose Trajectory & Slope
        if (features.glucoseSlope() != null) {
            double slope = features.glucoseSlope().doubleValue();
            if (slope > 15.0) {
                double pts = Math.min(25.0, slope * 0.8);
                score += pts;
                contributions.add(new FeatureContribution(
                        "Glucose Rapid Upward Slope",
                        BigDecimal.valueOf(pts).setScale(1, RoundingMode.HALF_UP),
                        String.format("Rising at +%.1f mg/dL/hr (steep positive velocity)", slope),
                        "RISK_INCREASING"
                ));
                drivers.add(String.format("Rapid upward glucose velocity (+%.1f mg/dL/hr)", slope));
            } else if (slope > 5.0) {
                double pts = 8.0;
                score += pts;
                contributions.add(new FeatureContribution(
                        "Glucose Moderate Elevation",
                        BigDecimal.valueOf(pts).setScale(1, RoundingMode.HALF_UP),
                        String.format("Rising at +%.1f mg/dL/hr", slope),
                        "RISK_INCREASING"
                ));
            } else if (slope < -5.0) {
                score -= 5.0;
                contributions.add(new FeatureContribution(
                        "Downward Glucose Recovery",
                        BigDecimal.valueOf(-5.0).setScale(1, RoundingMode.HALF_UP),
                        "Downward trend indicates active metabolic clearance",
                        "PROTECTIVE"
                ));
            }
        }

        // 2. Glucose Variability (CV > 36% indicates high glycemic variability)
        if (features.glucoseVariabilityCv() != null) {
            double cv = features.glucoseVariabilityCv().doubleValue();
            if (cv > 36.0) {
                double pts = 12.0;
                score += pts;
                contributions.add(new FeatureContribution(
                        "Elevated Glycemic Variability",
                        BigDecimal.valueOf(pts).setScale(1, RoundingMode.HALF_UP),
                        String.format("Glucose CV is %.1f%% (above 36%% metabolic target)", cv),
                        "RISK_INCREASING"
                ));
                drivers.add(String.format("High glycemic variability (CV %.1f%%)", cv));
            }
        }

        // 3. Sleep Deficit (acute sleep restriction impairs insulin sensitivity by ~20-30%)
        if (features.sleepDeficitHours() != null) {
            double deficit = features.sleepDeficitHours().doubleValue();
            if (deficit >= 2.0) {
                double pts = 18.0;
                score += pts;
                contributions.add(new FeatureContribution(
                        "Severe Cumulative Sleep Deficit",
                        BigDecimal.valueOf(pts).setScale(1, RoundingMode.HALF_UP),
                        String.format("Sleep deficit of %.1f hours below personal baseline (known to induce acute insulin resistance)", deficit),
                        "RISK_INCREASING"
                ));
                drivers.add(String.format("Sleep %.1f hours below baseline", deficit));
            } else if (deficit >= 1.0) {
                double pts = 10.0;
                score += pts;
                contributions.add(new FeatureContribution(
                        "Mild Sleep Debt",
                        BigDecimal.valueOf(pts).setScale(1, RoundingMode.HALF_UP),
                        String.format("Sleep deficit of %.1f hours below baseline", deficit),
                        "RISK_INCREASING"
                ));
                drivers.add(String.format("Sleep %.1f hours below baseline", deficit));
            } else if (deficit <= 0.0) {
                score -= 4.0;
                contributions.add(new FeatureContribution(
                        "Restorative Sleep Duration",
                        BigDecimal.valueOf(-4.0).setScale(1, RoundingMode.HALF_UP),
                        "Optimal sleep duration supporting glycemic homeostasis",
                        "PROTECTIVE"
                ));
            }
        }

        // 4. Autonomic Tone / HRV Suppression
        if (features.hrvDeviationPct() != null) {
            double hrvDev = features.hrvDeviationPct().doubleValue();
            if (hrvDev <= -20.0) {
                double pts = 15.0;
                score += pts;
                contributions.add(new FeatureContribution(
                        "Marked Autonomic HRV Suppression",
                        BigDecimal.valueOf(pts).setScale(1, RoundingMode.HALF_UP),
                        String.format("HRV is %.1f%% below baseline (reflecting sympathetic dominance / stress)", Math.abs(hrvDev)),
                        "RISK_INCREASING"
                ));
                drivers.add(String.format("HRV %.1f%% below baseline", Math.abs(hrvDev)));
            } else if (hrvDev <= -10.0) {
                double pts = 7.0;
                score += pts;
                contributions.add(new FeatureContribution(
                        "Moderate HRV Reduction",
                        BigDecimal.valueOf(pts).setScale(1, RoundingMode.HALF_UP),
                        String.format("HRV is %.1f%% below baseline", Math.abs(hrvDev)),
                        "RISK_INCREASING"
                ));
                drivers.add(String.format("HRV %.1f%% below baseline", Math.abs(hrvDev)));
            }
        }

        // 5. Resting Heart Rate Elevation
        if (features.restingHrDeviationBpm() != null) {
            double rhrDiff = features.restingHrDeviationBpm().doubleValue();
            if (rhrDiff >= 10.0) {
                double pts = 12.0;
                score += pts;
                contributions.add(new FeatureContribution(
                        "Substantial Resting Heart Rate Elevation",
                        BigDecimal.valueOf(pts).setScale(1, RoundingMode.HALF_UP),
                        String.format("Resting HR is +%.1f bpm above baseline", rhrDiff),
                        "RISK_INCREASING"
                ));
                drivers.add(String.format("Resting HR +%.1f bpm above baseline", rhrDiff));
            } else if (rhrDiff >= 5.0) {
                double pts = 6.0;
                score += pts;
                contributions.add(new FeatureContribution(
                        "Mild Resting HR Increase",
                        BigDecimal.valueOf(pts).setScale(1, RoundingMode.HALF_UP),
                        String.format("Resting HR is +%.1f bpm above baseline", rhrDiff),
                        "RISK_INCREASING"
                ));
            }
        }

        // 6. Sedentary / Physical Activity Deficit
        if (features.stepDeviationPct() != null) {
            double stepDev = features.stepDeviationPct().doubleValue();
            if (stepDev <= -30.0) {
                double pts = 8.0;
                score += pts;
                contributions.add(new FeatureContribution(
                        "Sedentary Inactivity / Low Step Count",
                        BigDecimal.valueOf(pts).setScale(1, RoundingMode.HALF_UP),
                        String.format("Daily activity %.1f%% below personal baseline", Math.abs(stepDev)),
                        "RISK_INCREASING"
                ));
            } else if (stepDev >= 20.0) {
                score -= 8.0;
                contributions.add(new FeatureContribution(
                        "Post-Meal Physical Activity",
                        BigDecimal.valueOf(-8.0).setScale(1, RoundingMode.HALF_UP),
                        "Elevated muscular glucose uptake via GLUT4 translocation",
                        "PROTECTIVE"
                ));
            }
        }

        // 7. Static Clinical Risk Modifiers (HbA1c & Fasting Glucose)
        if (features.hba1c() != null) {
            double a1c = features.hba1c().doubleValue();
            if (a1c >= 6.5) {
                score += 15.0;
                contributions.add(new FeatureContribution(
                        "Diabetic Range HbA1c",
                        BigDecimal.valueOf(15.0),
                        String.format("Baseline HbA1c is %.1f%% (high baseline metabolic vulnerability)", a1c),
                        "RISK_INCREASING"
                ));
            } else if (a1c >= 5.7) {
                score += 8.0;
                contributions.add(new FeatureContribution(
                        "Prediabetic Range HbA1c",
                        BigDecimal.valueOf(8.0),
                        String.format("Baseline HbA1c is %.1f%% (borderline insulin sensitivity)", a1c),
                        "RISK_INCREASING"
                ));
            }
        }

        // 8. Postprandial Window modifier
        if (features.isPostPrandialWindow()) {
            score += 10.0;
            contributions.add(new FeatureContribution(
                    "Post-Prandial Phase Window",
                    BigDecimal.valueOf(10.0),
                    "Active post-meal absorption window (typical peak at 45–90 mins)",
                    "RISK_INCREASING"
            ));
        }

        // 9. Static EHR: Established Type 2 Diabetes Diagnosis [Synthea FHIR]
        if (features.hasDiagnosedT2D()) {
            double pts = 12.0;
            score += pts;
            contributions.add(new FeatureContribution(
                    "Established Type 2 Diabetes [Synthea FHIR]",
                    BigDecimal.valueOf(pts),
                    "Documented clinical Type 2 Diabetes history with chronic pancreatic beta-cell dysfunction",
                    "RISK_INCREASING"
            ));
            drivers.add("Chronic Type 2 Diabetes [Synthea FHIR]");
        }

        // 10. Dynamic Telemetry: Current Activity Level
        if (features.activityLevel() != null) {
            String act = features.activityLevel().trim().toUpperCase();
            if ("VIGOROUS".equals(act)) {
                score -= 6.0;
                contributions.add(new FeatureContribution(
                        "Dynamic Aerobic Activity",
                        BigDecimal.valueOf(-6.0),
                        "Active physical telemetry confirms GLUT4 muscular glucose translocation",
                        "PROTECTIVE"
                ));
            } else if ("SEDENTARY".equals(act) && (features.stepDeviationPct() == null || features.stepDeviationPct().doubleValue() < 0)) {
                score += 5.0;
                contributions.add(new FeatureContribution(
                        "Sedentary Telemetry State",
                        BigDecimal.valueOf(5.0),
                        "Impaired postprandial glucose uptake due to prolonged sedentary posture",
                        "RISK_INCREASING"
                ));
            }
        }

        // Clamp probabilities [0, 99.5]
        score = Math.max(3.0, Math.min(99.0, score));
        BigDecimal finalProb = BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP);

        // Derive Metabolic Risk and Overall Risk
        double metabolicRisk = Math.min(100.0, score * 0.95);
        double overallRisk = Math.min(100.0, (metabolicRisk * 0.6) + (features.hrvDeviationPct() != null && features.hrvDeviationPct().doubleValue() < -15 ? 20.0 : 5.0));

        // Determine Risk Level & State
        String riskLevel;
        String twinState;
        String headline;
        String explanation;

        if (score >= 75.0) {
            riskLevel = "CRITICAL";
            twinState = "ACTIVE_ANOMALY";
            headline = "Imminent Postprandial Glucose Spike Risk (Prototype Risk Score: " + finalProb + " / 100)";
            explanation = "Compound metabolic strain: acute sleep restriction and autonomic stress have markedly impaired insulin sensitivity while glucose trajectory exhibits a steep positive velocity. (Prototype algorithmic model — not clinically validated; this prototype score is generated from engineered physiological features and is not a clinically calibrated probability).";
        } else if (score >= 55.0) {
            riskLevel = "HIGH";
            twinState = "ELEVATED_RISK";
            headline = "Elevated Risk of Glucose Spike Horizon Within Next 2 Hours (Score: " + finalProb + " / 100)";
            explanation = "Multi-signal indicators (reduced HRV, sleep debt, and positive glucose velocity) reflect elevated susceptibility to a post-meal glycemic excursion. (Prototype algorithmic model — not clinically validated).";
        } else if (score >= 35.0) {
            riskLevel = "MODERATE";
            twinState = "PRE_SYMPTOMATIC_DRIFT";
            headline = "Subtle Pre-Symptomatic Drift in Metabolic Regulation (Score: " + finalProb + " / 100)";
            explanation = "Biometric signals show gradual departure from personal baseline; glycemic variability is creeping upward while parasympathetic recovery is dampened. (Prototype algorithmic model — not clinically validated).";
        } else {
            riskLevel = "LOW";
            twinState = "STABLE";
            headline = "Metabolic Equilibrium — Low Spike Risk (Score: " + finalProb + " / 100)";
            explanation = "Glucose and autonomic metrics remain well-anchored within personal baseline tolerances. Normal insulin sensitivity indicated. (Prototype algorithmic model — not clinically validated).";
        }

        if (drivers.isEmpty()) {
            drivers.add("All signals within individual baseline tolerance envelopes");
        }

        GlucoseTrajectoryProjection trajectory = calculateTrajectoryProjection(features);

        return new PredictionOutput(
                "GLUCOSE_SPIKE_2H",
                "Next 2 Hours",
                riskLevel,
                finalProb,
                BigDecimal.valueOf(metabolicRisk).setScale(1, RoundingMode.HALF_UP),
                BigDecimal.valueOf(overallRisk).setScale(1, RoundingMode.HALF_UP),
                twinState,
                headline,
                explanation,
                contributions,
                drivers,
                new BigDecimal("0.940"),
                trajectory
        );
    }

    /**
     * Computes the deterministic, physiologically bounded 2-hour glucose trajectory projection.
     * Formulation A (authoritative model):
     *   effectiveMinutes = (1.0 - exp(-lambda * t)) / lambda
     *   delta(t) = (v0 + v_activity) * effectiveMinutes
     *   G(t) = clamp[40.0, 400.0](G0 + delta(t))
     *
     * Model Parameters:
     * - G0: Current CGM glucose (mg/dL, clamped [40.0, 400.0])
     * - v0: Glucose velocity rate of change (mg/dL/min, clamped [-3.0, +3.0])
     * - v_activity: Activity velocity modifier (mg/dL/min): -0.12 for VIGOROUS, +0.04 for SEDENTARY, 0.0 for MODERATE
     * - lambda: Prototype clearance parameter (min^-1):
     *     * Base non-diabetic: 0.015 min^-1 (half-life ~ 46 min)
     *     * Impaired / T2D / HbA1c >= 6.5%: 0.010 min^-1 (half-life ~ 69 min)
     *     * Vigorous exercise floor: max(lambda, 0.022 min^-1)
     *     * Sedentary cap: min(lambda, 0.011 min^-1)
     * - Clamping: [40.0, 400.0] mg/dL is a configured prototype safety floor and ceiling, not biological equilibrium.
     * - Sensor confidence and telemetry staleness guards attenuate velocity and confidence.
     */
    public static GlucoseTrajectoryProjection calculateTrajectoryProjection(EngineeredFeatures features) {
        // 1. Current Glucose
        double g0 = 95.0;
        if (features.meanGlucose() != null) {
            g0 = features.meanGlucose().doubleValue();
        } else if (features.fastingGlucose() != null) {
            g0 = features.fastingGlucose().doubleValue();
        }
        g0 = Math.max(40.0, Math.min(400.0, g0));

        // 2. Velocity / Rate of change in mg/dL/min
        double v0 = 0.0;
        if (features.glucoseVelocityMgDlPerMin() != null) {
            v0 = features.glucoseVelocityMgDlPerMin().doubleValue();
        } else if (features.glucoseSlope() != null) {
            // Fall back to slope divided by 5 minutes
            v0 = features.glucoseSlope().doubleValue() / 5.0;
        }

        // Clamp extreme velocity artifacts
        v0 = Math.max(-3.0, Math.min(3.0, v0));

        // 3. Sensor confidence and staleness damping
        double conf = 0.95;
        if (features.sensorConfidence() != null) {
            conf = Math.max(0.1, Math.min(1.0, features.sensorConfidence().doubleValue()));
        }

        // Check telemetry freshness (if older than 60 mins, damp velocity by 80%)
        if (features.telemetryTimestamp() != null) {
            long minsOld = java.time.Duration.between(features.telemetryTimestamp(), java.time.Instant.now()).toMinutes();
            if (minsOld > 60) {
                conf = Math.max(0.4, conf * 0.5);
                v0 = v0 * 0.2; // Stale rate of change decays rapidly
            }
        }

        // If confidence is low, pull velocity towards 0
        if (conf < 0.6) {
            v0 = v0 * (conf / 0.6);
        }

        // 4. Physiological decay constant lambda
        // Base rate: lambda = 0.015 min^-1 (half-life ~ 46 min)
        double lambda = 0.015;

        // Static EHR: Type 2 Diabetes impairs first-phase insulin response, leading to prolonged excursions
        if (features.hasDiagnosedT2D() || (features.hba1c() != null && features.hba1c().doubleValue() >= 6.5)) {
            lambda = 0.010; // Slower clearance, longer postprandial elevation
        }

        // Dynamic activity modifier (GLUT4 non-insulin clearance)
        double activityClearanceRate = 0.0;
        if (features.activityLevel() != null) {
            String act = features.activityLevel().trim().toUpperCase();
            if ("VIGOROUS".equals(act)) {
                lambda = Math.max(lambda, 0.022); // Accelerate return to homeostasis
                activityClearanceRate = -0.12;    // Muscular glucose uptake offset
            } else if ("SEDENTARY".equals(act)) {
                lambda = Math.min(lambda, 0.011);
                activityClearanceRate = +0.04;    // Inactivity sluggishness
            }
        }

        // 5. Generate discrete trajectory points across 120 minutes (0, 30, 60, 90, 120 mins)
        List<TrajectoryPoint> points = new ArrayList<>();
        int[] intervals = new int[]{0, 30, 60, 90, 120};

        for (int t : intervals) {
            double projectedG;
            if (t == 0) {
                projectedG = g0;
            } else {
                // Integrated damped velocity: v0 * (1 - exp(-lambda * t)) / lambda
                double effectiveMinutes = (1.0 - Math.exp(-lambda * t)) / lambda;
                double delta = (v0 + activityClearanceRate) * effectiveMinutes;
                projectedG = Math.max(40.0, Math.min(400.0, g0 + delta));
            }

            BigDecimal gVal = BigDecimal.valueOf(projectedG).setScale(1, RoundingMode.HALF_UP);
            String dir = "STABLE";
            if (projectedG > g0 + 3.0) dir = "RISING";
            else if (projectedG < g0 - 3.0) dir = "FALLING";

            points.add(new TrajectoryPoint(t, gVal, dir));
        }

        // 6. 120-minute horizon value
        BigDecimal g120 = points.get(points.size() - 1).projectedGlucose();
        BigDecimal gCurrent = BigDecimal.valueOf(g0).setScale(1, RoundingMode.HALF_UP);
        BigDecimal projectedDelta = g120.subtract(gCurrent).setScale(1, RoundingMode.HALF_UP);

        String overallDirection = "STABLE";
        if (projectedDelta.doubleValue() > 10.0) overallDirection = "RISING";
        else if (projectedDelta.doubleValue() < -10.0) overallDirection = "FALLING";

        return new GlucoseTrajectoryProjection(
                gCurrent,
                BigDecimal.valueOf(v0).setScale(2, RoundingMode.HALF_UP),
                g120,
                projectedDelta,
                overallDirection,
                "Next 2 Hours",
                points,
                BigDecimal.valueOf(conf).setScale(2, RoundingMode.HALF_UP),
                "Prototype 2-hour glucose trajectory projection — algorithmic model, not for clinical diagnosis"
        );
    }
}
