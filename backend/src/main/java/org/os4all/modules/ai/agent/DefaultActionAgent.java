package org.os4all.modules.ai.agent;

import org.os4all.modules.ai.model.StructuredHealthContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultActionAgent implements ActionAgent {

    @Override
    public ActionPlan formulateActions(StructuredHealthContext context, List<String> interpretations, double deviationScore) {
        List<String> actions = new ArrayList<>();
        String urgency;

        if (deviationScore >= 2.0) {
            urgency = "MONITOR";
            actions.add("Prioritize restorative rest and consistent sleep hygiene (target 7.5 - 8.5 hours) over the next 48 to 72 hours.");
            actions.add("Temporarily moderate strenuous physical training or high-intensity cardiovascular sessions in favor of active recovery.");
            actions.add("Ensure adequate hydration and nutritional balance.");
            actions.add("If you experience acute symptoms such as chest tightness, dizziness, shortness of breath, or fever, promptly seek professional medical evaluation.");
        } else if (deviationScore >= 1.0) {
            urgency = "MONITOR";
            actions.add("Continue continuous biometric tracking to monitor whether the observed trend stabilizes back to your baseline.");
            actions.add("Log any accompanying symptoms or notable lifestyle changes (e.g. travel, caffeine timing, work stress) in your health diary.");
        } else {
            urgency = "ROUTINE";
            actions.add("Continue your regular daily physical activity, balanced nutrition, and consistent sleep schedule.");
            actions.add("Maintain regular wearable sensor usage to sustain a robust personal baseline dataset.");
        }

        return new ActionPlan(actions, urgency);
    }
}
