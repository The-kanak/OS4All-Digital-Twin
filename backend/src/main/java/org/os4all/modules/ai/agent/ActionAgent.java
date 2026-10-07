package org.os4all.modules.ai.agent;

import org.os4all.modules.ai.model.StructuredHealthContext;

import java.util.List;

public interface ActionAgent {

    record ActionPlan(
            List<String> recommendedActions,
            String urgency // "ROUTINE", "MONITOR", "EVALUATE_SOON", "URGENT"
    ) {}

    /**
     * Determines pragmatic, non-invasive lifestyle recommendations and advises medical consultation
     * when appropriate, strictly according to the detected urgency and signal divergence.
     */
    ActionPlan formulateActions(StructuredHealthContext context, List<String> interpretations, double deviationScore);
}
