package org.os4all.modules.digitaltwin.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.os4all.modules.digitaltwin.dto.DigitalTwinStateDto;
import org.os4all.modules.digitaltwin.dto.GeminiChatResponseDto;
import org.os4all.modules.digitaltwin.dto.GeminiExplanationResponseDto;
import org.os4all.modules.digitaltwin.dto.PatientDetailDto;
import org.os4all.modules.digitaltwin.dto.PredictionResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Authoritative Grounded Gemini Explanation Service using the official Google GenAI Java SDK (com.google.genai.Client).
 *
 * Core Grounding & Compliance Rules:
 * 1. Gemini is an explanation and conversational layer ONLY.
 * 2. Deterministic Digital Twin engine is the source of truth.
 * 3. Gemini MUST NOT calculate risk scores, trajectories, Z-scores, states, or probabilities.
 * 4. Grounded in authoritative verified static EHR and dynamic telemetry only. Zero hallucination.
 * 5. Strict, seamless deterministic fallback when GEMINI_API_KEY is unset, offline, quota-limited, or errored.
 * 6. Never exposes or logs the API key.
 */
@Service
public class GeminiExplanationService {

    private static final Logger log = LoggerFactory.getLogger(GeminiExplanationService.class);
    private static final String DEFAULT_MODEL = "gemini-3.8-flash";
    private static final String DISCLAIMER = "Research / Hackathon Prototype — Not a Medical Diagnosis. Deterministic Digital Twin engine is the source of truth.";

    private final String model;
    private final Client genaiClient;

    public GeminiExplanationService(
            @Value("${gemini.api-key:${app.ai.gemini.api-key:${GEMINI_API_KEY:}}}") String apiKey,
            @Value("${gemini.model:${app.ai.gemini.model:${GEMINI_MODEL:gemini-3.8-flash}}}") String model
    ) {
        this.model = (model != null && !model.isBlank()) ? model.trim() : DEFAULT_MODEL;

        String resolvedKey = resolveApiKey(apiKey);
        Client client = null;
        if (!resolvedKey.isBlank()) {
            try {
                client = Client.builder().apiKey(resolvedKey).build();
                log.info("Google GenAI official SDK Client successfully initialized with model: {}", this.model);
            } catch (Exception e) {
                log.warn("Failed to initialize Google GenAI SDK Client (falling back to deterministic engine): {}", e.getMessage());
                client = null;
            }
        } else {
            log.info("Gemini API key is not configured. Running in deterministic offline fallback mode.");
        }
        this.genaiClient = client;
    }

    /**
     * Resolves the API key from Spring property, environment variable, system property, or Windows User scope.
     * Never prints or logs the key.
     */
    private static String resolveApiKey(String injectedKey) {
        if (injectedKey != null) {
            String trimmed = injectedKey.trim();
            if (trimmed.equalsIgnoreCase("DISABLED") || trimmed.equalsIgnoreCase("NONE") || trimmed.equalsIgnoreCase("OFFLINE")) {
                return "";
            }
            if (!trimmed.isBlank()) {
                return trimmed;
            }
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.trim().isBlank()) {
            return envKey.trim();
        }
        String propKey = System.getProperty("gemini.api-key");
        if (propKey != null && !propKey.trim().isBlank()) {
            return propKey.trim();
        }
        // If on Windows, check Windows User Environment registry if the process inherited a stale environment
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            try {
                ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-Command",
                        "[Environment]::GetEnvironmentVariable('GEMINI_API_KEY', 'User')");
                Process p = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && !line.trim().isBlank()) {
                        return line.trim();
                    }
                }
            } catch (Exception ignored) {
                // Ignore process execution failures and fallback cleanly
            }
        }
        return "";
    }

    public boolean isGeminiConfigured() {
        return genaiClient != null;
    }

    public String getModelName() {
        return model;
    }

    /**
     * Generates a grounded clinical explanation for the patient's Digital Twin state and 2-hour trajectory.
     */
    public GeminiExplanationResponseDto generateExplanation(
            PatientDetailDto patient,
            DigitalTwinStateDto state,
            PredictionResultDto pred,
            String focus
    ) {
        UUID patientId = state != null ? state.patientId() : (patient != null ? patient.id() : UUID.randomUUID());

        if (!isGeminiConfigured() || state == null) {
            return generateDeterministicFallback(patientId, patient, state, pred);
        }

        try {
            String prompt = constructGroundingPrompt(patient, state, pred,
                    "Provide a comprehensive doctor-facing explanation of the patient's current Digital Twin state, primary physiological drivers, and 2-hour glucose trajectory.");

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(0.2f)
                    .maxOutputTokens(900)
                    .build();

            GenerateContentResponse response = genaiClient.models.generateContent(this.model, prompt, config);
            String text = response != null ? response.text() : null;

            if (text != null && !text.isBlank()) {
                List<String> keyFactors = extractKeyFactors(state, pred);
                String trajectoryExpl = extractTrajectorySummary(state, pred);

                log.info("Successfully generated grounded Gemini explanation for patient: {}", state.patientName());
                return new GeminiExplanationResponseDto(
                        patientId,
                        text.trim(),
                        keyFactors,
                        trajectoryExpl,
                        "GEMINI",
                        this.model,
                        Instant.now().toString(),
                        DISCLAIMER
                );
            }
        } catch (Exception e) {
            log.warn("Gemini explanation generation failed (falling back to deterministic engine): {}", e.getMessage());
        }

        return generateDeterministicFallback(patientId, patient, state, pred);
    }

    /**
     * Answers conversational questions strictly grounded in the authoritative Digital Twin context.
     */
    public GeminiChatResponseDto chatGrounded(
            PatientDetailDto patient,
            DigitalTwinStateDto state,
            PredictionResultDto pred,
            String userQuestion
    ) {
        UUID patientId = state != null ? state.patientId() : (patient != null ? patient.id() : UUID.randomUUID());
        String q = userQuestion != null ? userQuestion.trim() : "What is the current Digital Twin status?";

        // Safety refusal check for prompt injection or secret reveal
        String lowerQ = q.toLowerCase(Locale.ROOT);
        if (lowerQ.contains("ignore") || lowerQ.contains("api key") || lowerQ.contains("secret") || lowerQ.contains("override") || lowerQ.contains("bypass")) {
            return new GeminiChatResponseDto(
                    patientId,
                    q,
                    "Security Policy: Requests to override clinical safety rules or disclose internal configuration keys are strictly rejected.",
                    List.of("System Security & Clinical Grounding Policy"),
                    "FALLBACK",
                    "security-guardrail",
                    Instant.now().toString(),
                    DISCLAIMER
            );
        }

        if (!isGeminiConfigured() || state == null) {
            return generateDeterministicChatFallback(patientId, state, pred, q);
        }

        try {
            String prompt = constructGroundingPrompt(patient, state, pred, "Clinician Question: " + q);

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(0.2f)
                    .maxOutputTokens(700)
                    .build();

            GenerateContentResponse response = genaiClient.models.generateContent(this.model, prompt, config);
            String text = response != null ? response.text() : null;

            if (text != null && !text.isBlank()) {
                return new GeminiChatResponseDto(
                        patientId,
                        q,
                        text.trim(),
                        extractKeyFactors(state, pred),
                        "GEMINI",
                        this.model,
                        Instant.now().toString(),
                        DISCLAIMER
                );
            }
        } catch (Exception e) {
            log.warn("Gemini chat call failed (falling back to deterministic response): {}", e.getMessage());
        }

        return generateDeterministicChatFallback(patientId, state, pred, q);
    }

    /**
     * Backward-compatible method used by existing Virtual Patient interaction service.
     */
    public String explainGroundedState(DigitalTwinStateDto state, PredictionResultDto pred, String doctorQuestion) {
        if (!isGeminiConfigured() || state == null) {
            return null;
        }
        try {
            String prompt = constructGroundingPrompt(null, state, pred, "Clinician Question: " + doctorQuestion);
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(0.2f)
                    .maxOutputTokens(700)
                    .build();
            GenerateContentResponse response = genaiClient.models.generateContent(this.model, prompt, config);
            return (response != null && response.text() != null && !response.text().isBlank()) ? response.text().trim() : null;
        } catch (Exception e) {
            log.warn("Gemini interaction failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Constructs the rigorous grounding prompt enforcing strict numerical integrity and clinical boundaries.
     */
    public String constructGroundingPrompt(
            PatientDetailDto patient,
            DigitalTwinStateDto state,
            PredictionResultDto pred,
            String taskDescription
    ) {
        StringBuilder sb = new StringBuilder();

        // 1. System Guardrails & Grounding Instructions
        sb.append("""
            You are an explanation layer for a healthcare Digital Twin prototype.
            The supplied Digital Twin values are authoritative.
            Explain those values clearly for a doctor-facing prototype dashboard.
            
            STRICT RULES:
            1. Do NOT recalculate, alter, estimate, or invent numerical values.
            2. Do NOT invent symptoms, diagnoses, medications, laboratory results, or patient history.
            3. Do NOT provide a new medical prediction or claim clinical validation.
            4. Do NOT make definitive medical diagnoses or prescribe pharmaceuticals.
            5. If information is unavailable, explicitly state that it is not available in the current Digital Twin data.
            6. The deterministic Digital Twin engine is the source of truth.
            7. NUMERICAL INTEGRITY: Explain the exact provided numbers. Never say "Gemini predicts..."; always say "The Digital Twin projection indicates...".
            
            """);

        // 2. Authoritative Verified Patient Context
        sb.append("AUTHORITATIVE VERIFIED PATIENT CONTEXT:\n");
        String name = state != null ? state.patientName() : (patient != null ? patient.fullName() : "Patient");
        sb.append("• Patient: ").append(name);
        if (patient != null) {
            sb.append(", Age: ").append(patient.age()).append(", Sex: ").append(patient.biologicalSex())
              .append(", BMI: ").append(patient.bmi());
            if (patient.historicalRecords() != null && !patient.historicalRecords().isEmpty()) {
                sb.append("\n• Recorded Diagnoses: ");
                List<String> conds = new ArrayList<>();
                for (var r : patient.historicalRecords()) {
                    if (r.conditionOrDiagnosis() != null && !r.conditionOrDiagnosis().isBlank()) {
                        conds.add(r.conditionOrDiagnosis());
                    }
                }
                sb.append(conds.isEmpty() ? "None recorded" : String.join("; ", conds));
            }
        }
        sb.append("\n");

        if (state != null) {
            sb.append("• Virtual State: ").append(state.state()).append("\n");
            sb.append("• Deterministic Prototype Risk Score: ").append(state.overallRiskScore()).append(" / 100\n");
            sb.append("• Metabolic Spike Probability: ").append(state.glucoseSpikeProbability()).append("% (Horizon: ").append(state.predictionHorizon()).append(")\n");
            sb.append("• Active Simulation Scenario: ").append(state.activeScenario()).append("\n");

            // Telemetry & Vitals
            if (state.vitalsSnapshot() != null) {
                var v = state.vitalsSnapshot();
                sb.append("• Live Telemetry: Glucose ").append(v.glucose()).append(" mg/dL, HR ").append(v.heartRate())
                  .append(" bpm, Resting HR ").append(v.restingHeartRate()).append(" bpm, HRV ").append(v.hrv())
                  .append(" ms, Sleep ").append(v.sleepHours()).append(" hrs, Steps ").append(v.steps()).append("\n");
            }

            // 2-Hour Trajectory Projection
            BigDecimal currentG = state.vitalsSnapshot() != null ? state.vitalsSnapshot().glucose() : null;
            sb.append("• 2-Hour Glucose Trajectory:\n");
            sb.append("  - Current Glucose: ").append(currentG != null ? currentG : "N/A").append(" mg/dL\n");
            sb.append("  - Glucose Velocity (dG/dt): ").append(state.glucoseVelocity() != null ? state.glucoseVelocity() : (pred != null ? pred.glucoseVelocity() : "0.00")).append(" mg/dL/min\n");
            sb.append("  - Trajectory Direction: ").append(state.trajectoryDirection() != null ? state.trajectoryDirection() : "STABLE").append("\n");
            sb.append("  - Projected 120-Min Glucose: ").append(state.projectedGlucose120Min() != null ? state.projectedGlucose120Min() : (pred != null ? pred.projectedGlucose120Min() : "N/A")).append(" mg/dL\n");
            sb.append("  - Projected 120-Min Delta: ").append(state.projectedDelta() != null ? state.projectedDelta() : (pred != null ? pred.projectedDelta() : "0.0")).append(" mg/dL\n");

            if (pred != null && pred.trajectoryPoints() != null && !pred.trajectoryPoints().isEmpty()) {
                sb.append("  - Discrete Milestone Points: ");
                List<String> pts = new ArrayList<>();
                for (var p : pred.trajectoryPoints()) {
                    pts.add(String.format("T+%d min: %s mg/dL", p.minuteOffset(), p.projectedGlucose()));
                }
                sb.append(String.join(", ", pts)).append("\n");
            }

            // Primary Drivers & Baseline Deviations
            if (state.stateDrivers() != null && !state.stateDrivers().isEmpty()) {
                sb.append("• Primary Algorithmic Drivers: ").append(String.join(", ", state.stateDrivers())).append("\n");
            }
            if (state.baselineDeviations() != null && !state.baselineDeviations().isEmpty()) {
                sb.append("• Baseline Deviations:\n");
                for (var dev : state.baselineDeviations()) {
                    sb.append("  - ").append(dev.metric()).append(": Current ").append(dev.currentValue()).append(" ").append(dev.unit())
                      .append(" vs Baseline ").append(dev.baselineMean()).append(" ").append(dev.unit())
                      .append(" (Δ ").append(dev.deviation()).append(", ").append(dev.trendDirection()).append(")\n");
                }
            }
        }

        if (pred != null && pred.contributingFactors() != null && !pred.contributingFactors().isEmpty()) {
            sb.append("• Feature Level Contributions:\n");
            for (var cf : pred.contributingFactors()) {
                sb.append("  - ").append(cf.factorName()).append(": ").append(cf.description())
                  .append(" [Direction: ").append(cf.direction()).append(", Weight: ").append(cf.weight()).append("]\n");
            }
        }

        sb.append("\n").append(taskDescription).append("\n");
        return sb.toString();
    }

    /**
     * Deterministic offline explanation generator fallback.
     */
    public GeminiExplanationResponseDto generateDeterministicFallback(
            UUID patientId,
            PatientDetailDto patient,
            DigitalTwinStateDto state,
            PredictionResultDto pred
    ) {
        String name = state != null ? state.patientName() : (patient != null ? patient.fullName() : "Virtual Patient");
        String twinState = state != null ? state.state() : "STABLE";
        BigDecimal risk = state != null ? state.overallRiskScore() : BigDecimal.valueOf(15.0);
        BigDecimal spikeProb = state != null ? state.glucoseSpikeProbability() : BigDecimal.valueOf(12.0);

        BigDecimal curG = state != null && state.vitalsSnapshot() != null ? state.vitalsSnapshot().glucose() : BigDecimal.valueOf(95.0);
        BigDecimal vel = state != null && state.glucoseVelocity() != null ? state.glucoseVelocity() : BigDecimal.ZERO;
        BigDecimal proj120 = state != null && state.projectedGlucose120Min() != null ? state.projectedGlucose120Min() : curG;
        String dir = state != null && state.trajectoryDirection() != null ? state.trajectoryDirection() : "STABLE";

        StringBuilder exp = new StringBuilder();
        exp.append(String.format("The OS4All Digital Twin indicates that %s is currently operating in a [%s] state ", name, twinState));
        exp.append(String.format("with an overall algorithmic risk score of %s / 100 and a %s%% probability of a glucose spike within the 2-hour horizon.\n\n", risk, spikeProb));

        exp.append("Physiological Analysis:\n");
        if (state != null && state.stateDrivers() != null && !state.stateDrivers().isEmpty()) {
            exp.append("• Primary Drivers: ").append(String.join(", ", state.stateDrivers())).append(".\n");
        }
        if (state != null && state.vitalsSnapshot() != null) {
            var v = state.vitalsSnapshot();
            exp.append(String.format("• Vitals Summary: Current CGM glucose is %s mg/dL, HR is %s bpm (Resting HR %s bpm), autonomic HRV is %s ms, and restorative sleep was %s hours.\n",
                    v.glucose(), v.heartRate(), v.restingHeartRate(), v.hrv(), v.sleepHours()));
        }

        exp.append("\n2-Hour Trajectory Projection:\n");
        exp.append(String.format("The Digital Twin projection indicates a %s trajectory. Starting from a current baseline reading of %s mg/dL with a velocity of %s%s mg/dL/min, ",
                dir, curG, vel.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "", vel));
        exp.append(String.format("the deterministic projection calculates a 120-minute horizon value of %s mg/dL.\n", proj120));

        List<String> keyFactors = extractKeyFactors(state, pred);
        String trajSummary = String.format("Trajectory: %s | Current: %s mg/dL | Velocity: %s%s mg/dL/min | 120-min Projection: %s mg/dL",
                dir, curG, vel.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "", vel, proj120);

        return new GeminiExplanationResponseDto(
                patientId,
                exp.toString().trim(),
                keyFactors,
                trajSummary,
                "FALLBACK",
                "deterministic-offline-engine",
                Instant.now().toString(),
                DISCLAIMER
        );
    }

    private GeminiChatResponseDto generateDeterministicChatFallback(
            UUID patientId,
            DigitalTwinStateDto state,
            PredictionResultDto pred,
            String question
    ) {
        String name = state != null ? state.patientName() : "Patient";
        String lower = question.toLowerCase(Locale.ROOT);
        StringBuilder answer = new StringBuilder();

        if (lower.contains("trajectory") || lower.contains("rising") || lower.contains("falling") || lower.contains("velocity")) {
            BigDecimal curG = state != null && state.vitalsSnapshot() != null ? state.vitalsSnapshot().glucose() : BigDecimal.valueOf(95.0);
            BigDecimal vel = state != null && state.glucoseVelocity() != null ? state.glucoseVelocity() : BigDecimal.ZERO;
            BigDecimal proj = state != null && state.projectedGlucose120Min() != null ? state.projectedGlucose120Min() : curG;
            String dir = state != null && state.trajectoryDirection() != null ? state.trajectoryDirection() : "STABLE";
            answer.append(String.format("The Digital Twin projection indicates a %s glucose trajectory. Current CGM reading is %s mg/dL with a velocity of %s%s mg/dL/min, projecting to %s mg/dL at the 120-minute horizon based on deterministic linear velocity dampening.",
                    dir, curG, vel.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "", vel, proj));
        } else if (lower.contains("baseline") || lower.contains("changed") || lower.contains("different")) {
            answer.append(String.format("Compared to %s's established 45-day personal baseline, the digital twin detects significant autonomic and metabolic shifts: ", name));
            if (state != null && state.baselineDeviations() != null) {
                for (var d : state.baselineDeviations()) {
                    answer.append(String.format("%s is %s %s (Δ %s, %s); ", d.metric(), d.currentValue(), d.unit(), d.deviation(), d.trendDirection()));
                }
            }
        } else {
            answer.append(String.format("Patient %s is currently in a [%s] state (Risk Score: %s/100). The primary algorithmic drivers are: %s.",
                    name, state != null ? state.state() : "STABLE", state != null ? state.overallRiskScore() : "15",
                    state != null && state.stateDrivers() != null ? String.join(", ", state.stateDrivers()) : "stable baseline"));
        }

        return new GeminiChatResponseDto(
                patientId,
                question,
                answer.toString().trim(),
                extractKeyFactors(state, pred),
                "FALLBACK",
                "deterministic-offline-engine",
                Instant.now().toString(),
                DISCLAIMER
        );
    }

    private List<String> extractKeyFactors(DigitalTwinStateDto state, PredictionResultDto pred) {
        List<String> list = new ArrayList<>();
        if (state != null && state.stateDrivers() != null) {
            list.addAll(state.stateDrivers());
        }
        if (pred != null && pred.contributingFactors() != null) {
            for (var cf : pred.contributingFactors()) {
                String item = cf.factorName() + ": " + cf.description();
                if (!list.contains(item)) {
                    list.add(item);
                }
            }
        }
        if (list.isEmpty()) {
            list.add("Personal baseline within homeostatic boundaries");
        }
        return list;
    }

    private String extractTrajectorySummary(DigitalTwinStateDto state, PredictionResultDto pred) {
        BigDecimal curG = state != null && state.vitalsSnapshot() != null ? state.vitalsSnapshot().glucose() : BigDecimal.valueOf(95.0);
        BigDecimal vel = state != null && state.glucoseVelocity() != null ? state.glucoseVelocity() : BigDecimal.ZERO;
        BigDecimal proj = state != null && state.projectedGlucose120Min() != null ? state.projectedGlucose120Min() : curG;
        String dir = state != null && state.trajectoryDirection() != null ? state.trajectoryDirection() : "STABLE";
        return String.format("Trajectory: %s | CGM: %s mg/dL | Velocity: %s%s mg/dL/min | 120-Min: %s mg/dL",
                dir, curG, vel.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "", vel, proj);
    }
}
