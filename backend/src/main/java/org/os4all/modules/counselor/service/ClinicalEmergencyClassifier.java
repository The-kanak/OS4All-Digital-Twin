package org.os4all.modules.counselor.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Deterministic clinical emergency classifier.
 * Evaluates queries for symptoms indicating potentially life-threatening or urgent medical emergencies.
 * If triggered, the counselor strictly diverts to emergency triage rather than interpreting health data.
 */
@Component
public class ClinicalEmergencyClassifier {

    private static final List<Pattern> EMERGENCY_PATTERNS = List.of(
            // Cardiac / Vascular
            Pattern.compile("\\b(chest pain|chest pressure|heart attack|crushing chest|angina|radiating to (arm|jaw|neck))\\b", Pattern.CASE_INSENSITIVE),
            // Respiratory
            Pattern.compile("\\b(can'?t breathe|severe shortness of breath|suffocating|gasping for air|turning blue|asphyxi)\\b", Pattern.CASE_INSENSITIVE),
            // Neurological / Stroke
            Pattern.compile("\\b(stroke|sudden weakness|facial droop|slurred speech|sudden numbness|loss of consciousness|passed out|seizure)\\b", Pattern.CASE_INSENSITIVE),
            // Severe Bleeding / Trauma
            Pattern.compile("\\b(coughing blood|vomiting blood|uncontrolled bleeding|severe hemorrhag)\\b", Pattern.CASE_INSENSITIVE),
            // Anaphylaxis
            Pattern.compile("\\b(anaphylax|throat closing|severe allergic reaction|swelling of tongue)\\b", Pattern.CASE_INSENSITIVE),
            // Psychiatric crisis
            Pattern.compile("\\b(kill myself|suicid|end my life|want to die|self-harm)\\b", Pattern.CASE_INSENSITIVE)
    );

    public boolean isEmergencyQuery(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }

        String normalized = query.toLowerCase(Locale.ROOT).trim();
        for (Pattern pattern : EMERGENCY_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                return true;
            }
        }
        return false;
    }
}
