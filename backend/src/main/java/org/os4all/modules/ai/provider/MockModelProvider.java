package org.os4all.modules.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Mock Model Provider for local development and deterministic offline testing.
 * Generates valid JSON adhering strictly to the AiHealthInsightResponse schema.
 */
@Component("mockModelProvider")
public class MockModelProvider implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(MockModelProvider.class);
    private final ObjectMapper objectMapper;

    public MockModelProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String generateChatCompletion(List<ChatMessage> messages, ModelParameters parameters) {
        log.info("MockModelProvider generating response with model: {}", parameters.model());

        // Extract user context from the prompt to craft a realistic, context-aware mock response
        String userContent = messages.stream()
                .filter(m -> "user".equalsIgnoreCase(m.role()))
                .map(ChatMessage::content)
                .reduce("", (a, b) -> a + "\n" + b);

        // Check if this is a Counselor conversation request
        boolean isCounselorPrompt = messages.stream().anyMatch(m -> "system".equalsIgnoreCase(m.role()) && m.content().contains("OS4All AI Health Counselor"));
        if (isCounselorPrompt) {
            return generateMockCounselorResponse(userContent);
        }

        Map<String, Object> responseMap = new LinkedHashMap<>();

        if (userContent.contains("ANOMALY") || userContent.contains("Multi-Signal") || userContent.contains("strain")) {
            responseMap.put("summary", "OS4All observed: Multi-signal autonomic strain pattern over the last 48-72 hours with elevated resting heart rate and suppressed nocturnal HRV relative to baseline. External evidence indicates: Physiological strain is consistent with accumulated sleep debt and delayed parasympathetic recovery. OS4All recommends discussing: Review persistent multi-day departures with a medical professional if elevations persist.");
            responseMap.put("observations", List.of(
                    "Resting heart rate measured at 72 bpm, which is 12 bpm (+2.0 standard deviations) above your established 60 bpm personal baseline.",
                    "HRV measured at 38 ms, which is 17 ms (-1.8 standard deviations) below your established 55 ms baseline.",
                    "Sleep duration averaged 5.0 hours over recent nights compared to your 8.0-hour historical baseline."
            ));
            responseMap.put("possibleInterpretations", List.of(
                    "Physiological strain consistent with accumulated sleep debt and acute sympathetic autonomic tone dominance.",
                    "Potential response to acute physical overtraining, environmental stress, or early physiological response prior to overt viral illness."
            ));
            responseMap.put("evidence", List.of(
                    Map.of(
                            "title", "Heart rate variability as a marker of autonomic recovery and physical stress",
                            "source", "Frontiers in Physiology / National Library of Medicine",
                            "urlOrDoi", "https://doi.org/10.3389/fphys.2018.00532",
                            "relevance", "Suppressed nocturnal HRV paired with elevated resting heart rate strongly correlates with autonomic recovery deficit and sleep disruption."
                    )
            ));
            responseMap.put("recommendedActions", List.of(
                    "Prioritize sleep hygiene and aim for 8 hours of restorative rest over the next 2-3 consecutive evenings.",
                    "Reduce high-intensity athletic or cardiovascular training load to active recovery until autonomic metrics normalize.",
                    "Ensure adequate hydration and electrolyte intake.",
                    "If symptoms of malaise, chest discomfort, or persistent tachycardia appear, consult a medical doctor promptly."
            ));
            responseMap.put("urgency", "MONITOR");
            responseMap.put("confidence", 0.92);
        } else if (userContent.contains("DRIFT")) {
            responseMap.put("summary", "OS4All observed: Subtle persistent trend drift detected across continuous biometric signals over the past 3-5 days. External evidence indicates: Metric drift reflects gradual adaptation to lifestyle factors or autonomic shifts. OS4All recommends discussing: Routine baseline trends during regular checkups if drift continues.");
            responseMap.put("observations", List.of(
                    "Continuous physiological metric shows a gradual 4-day directional drift away from the central personal baseline average."
            ));
            responseMap.put("possibleInterpretations", List.of(
                    "Gradual adaptation to changes in daily schedule, physical activity volume, or lifestyle habits.",
                    "Subtle autonomic nervous system variation that warrants passive monitoring."
            ));
            responseMap.put("evidence", List.of(
                    Map.of(
                            "title", "Personalized Digital Health Tracking and Biometric Baselines",
                            "source", "Nature Medicine / Digital Biomarkers",
                            "urlOrDoi", "https://doi.org/10.1038/s41591-020-0859-x",
                            "relevance", "Individual baseline tracking detects longitudinal deviations earlier than rigid population-wide clinical thresholds."
                    )
            ));
            responseMap.put("recommendedActions", List.of(
                    "Continue wearing sensor continuously to collect additional baseline data points.",
                    "Log relevant lifestyle context (e.g. travel, late caffeine consumption, changes in routine)."
            ));
            responseMap.put("urgency", "ROUTINE");
            responseMap.put("confidence", 0.88);
        } else {
            responseMap.put("summary", "OS4All observed: All tracked physiological vitals and biometric observations remain stable and firmly within your personal historical baselines. External evidence indicates: Maintained homeostatic stability reflects optimal cardiovascular and autonomic equilibrium. OS4All recommends discussing: Continue routine wellness follow-ups.");
            responseMap.put("observations", List.of(
                    "Resting heart rate, HRV, SpO2, and sleep patterns are well-aligned with your personal 30-day baseline ranges."
            ));
            responseMap.put("possibleInterpretations", List.of(
                    "Healthy homeostasis with balanced autonomic nervous system tone and steady recovery patterns."
            ));
            responseMap.put("evidence", List.of(
                    Map.of(
                            "title", "Physiological metric stability and long-term health baselines",
                            "source", "Circulation / American Heart Association",
                            "urlOrDoi", "https://doi.org/10.1161/CIRCULATIONAHA.118.035878",
                            "relevance", "Metric stability within individual variance envelopes indicates optimal autonomic equilibrium."
                    )
            ));
            responseMap.put("recommendedActions", List.of(
                    "Maintain current sleep schedule, physical exercise routine, and nutritional habits."
            ));
            responseMap.put("urgency", "ROUTINE");
            responseMap.put("confidence", 0.95);
        }

        responseMap.put("disclaimer",
                "OS4All AI insights are informational health interpretations based strictly on personal historical baselines " +
                "and physiological signals. They DO NOT constitute a medical diagnosis, clinical treatment plan, or doctor-patient relationship. " +
                "Always consult a qualified healthcare professional regarding medical symptoms or clinical decisions.");

        try {
            return objectMapper.writeValueAsString(responseMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize mock AI response", e);
        }
    }

    private String generateMockCounselorResponse(String userContent) {
        Map<String, Object> resp = new LinkedHashMap<>();

        boolean isAnomalyOrChange = userContent.contains("ANOMALY") || userContent.contains("DRIFT") || userContent.toLowerCase().contains("why did my health status change");

        if (isAnomalyOrChange) {
            resp.put("message",
                    "Based on your actual OS4All physiological baseline, your health status recently shifted due to a multi-signal autonomic deviation. " +
                    "Your resting heart rate elevated above your established baseline range while nighttime HRV dipped over the last 48 hours. " +
                    "This pattern typically points to physiological fatigue, acute stress, or recovery debt rather than an underlying pathology. " +
                    "If you experience persistent or worsening symptoms beyond 4-5 days, clinical evaluation with a physician is recommended.");
            resp.put("os4allObservations", List.of(
                    "Resting heart rate measured at 72 bpm, which is +12 bpm above your personal baseline average of 60 bpm.",
                    "Nocturnal HRV dropped to 38 ms, which is 17 ms below your 55 ms baseline normal range.",
                    "Sleep duration recorded at 5.2 hours over recent nights compared to your 8.0-hour historical baseline."
            ));
            resp.put("aiInterpretations", List.of(
                    "Autonomic strain consistent with acute sleep deficit and elevated sympathetic cardiovascular tone.",
                    "Potential early physiological adaptation to physical exertion, psychological stress, or subclinical immune response."
            ));
            resp.put("externalEvidence", List.of(
                    Map.of(
                            "title", "Heart rate variability as a marker of autonomic recovery and physical stress",
                            "source", "Frontiers in Physiology / National Library of Medicine",
                            "urlOrDoi", "https://doi.org/10.3389/fphys.2018.00532",
                            "snippet", "Autonomic strain manifested by suppressed HRV and elevated resting pulse correlates reliably with systemic physiological recovery debt.",
                            "relevanceScore", 0.94
                    )
            ));
            resp.put("recommendedActions", List.of(
                    "Prioritize restorative sleep and aim for 8 hours across the next 2-3 consecutive evenings.",
                    "Reduce high-intensity athletic or cardiovascular training load to active recovery until autonomic metrics normalize.",
                    "Ensure adequate hydration and electrolyte balance."
            ));
            resp.put("professionalEvaluationGuidance",
                    "If resting pulse remains elevated for more than 5 days, or if you experience accompanying symptoms like chest tightness, lightheadedness, or shortness of breath, consult a medical professional promptly.");
            resp.put("urgency", "MONITOR");
            resp.put("confidence", 0.92);
        } else {
            resp.put("message",
                    "Your tracked physiological vitals remain well-balanced and stable within your personal historical baseline. " +
                    "Resting heart rate, HRV, SpO2, and sleep duration show healthy autonomic homeostasis with no active anomalies.");
            resp.put("os4allObservations", List.of(
                    "Resting heart rate and HRV are within your personal 30-day baseline intervals.",
                    "SpO2 averaged 98.4% and recent sleep quality metrics indicate steady circadian consistency."
            ));
            resp.put("aiInterpretations", List.of(
                    "Physiological homeostasis and balanced parasympathetic recovery tone."
            ));
            resp.put("externalEvidence", List.of(
                    Map.of(
                            "title", "Physiological metric stability and long-term health baselines",
                            "source", "Circulation / American Heart Association",
                            "urlOrDoi", "https://doi.org/10.1161/CIRCULATIONAHA.118.035878",
                            "snippet", "Longitudinal stability in physiological parameters reflects cardiovascular health and homeostatic resilience.",
                            "relevanceScore", 0.91
                    )
            ));
            resp.put("recommendedActions", List.of(
                    "Maintain your current balanced routine of physical activity, sleep regularity, and nutrition.",
                    "Continue wearing your health sensor to maintain high statistical baseline reliability."
            ));
            resp.put("professionalEvaluationGuidance",
                    "Your continuous parameters are within normal individual limits; continue standard routine wellness follow-ups.");
            resp.put("urgency", "ROUTINE");
            resp.put("confidence", 0.96);
        }

        resp.put("disclaimer",
                "OS4All AI insights and Counselor conversations are informational health interpretations grounded in individual baselines " +
                "and physiological signals. They DO NOT constitute a medical diagnosis, clinical treatment plan, or doctor-patient relationship. " +
                "Always consult a qualified healthcare provider for clinical medical concerns.");

        try {
            return objectMapper.writeValueAsString(resp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize mock counselor AI response", e);
        }
    }
}
