package org.os4all.modules.ai.agent;

import org.os4all.modules.ai.model.StructuredHealthContext;
import org.os4all.modules.ai.provider.ModelProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultExplanationAgent implements ExplanationAgent {

    @Override
    public String generateExplanation(
            StructuredHealthContext context,
            List<String> verifiedObservations,
            List<String> interpretations,
            ModelProvider provider
    ) {
        StringBuilder sb = new StringBuilder();

        if (context.activeAnomalies() != null && !context.activeAnomalies().isEmpty()) {
            sb.append("OS4All observed: Noticeable directional departures detected relative to your personal historical baseline across continuous biometric signals. ");
            if (verifiedObservations != null && !verifiedObservations.isEmpty()) {
                sb.append(verifiedObservations.get(0)).append(" ");
            }

            if (interpretations != null && !interpretations.isEmpty()) {
                sb.append("External evidence indicates: ").append(interpretations.get(0)).append(" ");
            } else {
                sb.append("External evidence indicates: Multi-signal departures frequently reflect cumulative physiological stress or recovery debt. ");
            }

            sb.append("OS4All recommends discussing: Review persistent multi-day departures with a medical professional if elevations persist.");
        } else {
            sb.append("OS4All observed: All tracked continuous biometric parameters (resting heart rate, HRV, SpO2, sleep duration, and activity) remain consistent with your individual historical baseline intervals. ");
            sb.append("External evidence indicates: Maintained homeostatic stability reflects balanced autonomic tone. ");
            sb.append("OS4All recommends discussing: Routine preventative checkups at scheduled medical appointments.");
        }

        return sb.toString().trim();
    }
}
