package org.os4all.modules.ai.agent;

import org.os4all.modules.ai.model.StructuredHealthContext;

import java.util.List;

public interface TrendInterpretationAgent {

    record InterpretationResult(
            List<String> verifiedObservations,
            List<String> possibleInterpretations,
            double baselineDeviationScore
    ) {}

    /**
     * Synthesizes detected signals and deviations from the health context into grounded observations
     * and strictly non-diagnostic physiological hypotheses.
     */
    InterpretationResult interpretTrends(StructuredHealthContext context);
}
