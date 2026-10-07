package org.os4all.modules.ai.agent;

import org.os4all.modules.ai.model.StructuredHealthContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultTrendInterpretationAgent implements TrendInterpretationAgent {

    @Override
    public InterpretationResult interpretTrends(StructuredHealthContext context) {
        List<String> observations = new ArrayList<>();
        List<String> interpretations = new ArrayList<>();
        double score = 0.0;

        // Process active anomalies
        if (context.activeAnomalies() != null && !context.activeAnomalies().isEmpty()) {
            for (StructuredHealthContext.DetectedAnomalyItem anomaly : context.activeAnomalies()) {
                observations.add(anomaly.explanation());
                if (anomaly.isMultiSignalCompound()) {
                    score += 2.0;
                    interpretations.add("Multi-signal autonomic pattern detected: Compound deviation across resting heart rate, HRV, and sleep consistency indicates systemic autonomic fatigue or recovery deficit rather than an isolated sensor artifact.");
                } else if ("ANOMALY".equalsIgnoreCase(anomaly.state())) {
                    score += 1.5;
                    interpretations.add("A persistent departure from historical baseline observed for " + anomaly.metric() + " spanning " + anomaly.persistenceDays() + " days.");
                } else if ("DRIFT".equalsIgnoreCase(anomaly.state())) {
                    score += 0.8;
                    interpretations.add("Gradual trend drift observed for " + anomaly.metric() + ", reflecting potential routine adjustment or mild physiological shift.");
                }
            }
        }

        // Process recent lab biomarkers
        if (context.recentLabResults() != null) {
            for (StructuredHealthContext.LabBiomarkerItem lab : context.recentLabResults()) {
                if (lab.referenceHigh() != null && lab.value().compareTo(lab.referenceHigh()) > 0) {
                    observations.add(lab.biomarker() + " is " + lab.value() + " " + lab.unit() + " (reference upper bound: " + lab.referenceHigh() + " " + lab.unit() + ").");
                    interpretations.add("Biomarker level for " + lab.biomarker() + " is elevated above standard reference envelope.");
                    score += 0.5;
                } else if (lab.referenceLow() != null && lab.value().compareTo(lab.referenceLow()) < 0) {
                    observations.add(lab.biomarker() + " is " + lab.value() + " " + lab.unit() + " (reference lower bound: " + lab.referenceLow() + " " + lab.unit() + ").");
                    interpretations.add("Biomarker level for " + lab.biomarker() + " is below standard reference envelope.");
                    score += 0.5;
                }
            }
        }

        if (observations.isEmpty()) {
            observations.add("All monitored physiological signals remain within your established individual baseline ranges.");
            interpretations.add("Current data demonstrates stable physiological homeostasis and balanced autonomic tone.");
        }

        return new InterpretationResult(observations, interpretations, score);
    }
}
