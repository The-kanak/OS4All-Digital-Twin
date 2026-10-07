package org.os4all.modules.ai.agent;

import org.os4all.modules.ai.model.StructuredHealthContext;
import org.os4all.modules.ai.provider.ModelProvider;

import java.util.List;

public interface ExplanationAgent {

    /**
     * Synthesizes clear, plain-language explanations of physiological patterns.
     * Enforces that the explanation strictly avoids claiming diagnosis, highlights personal deviation,
     * acknowledges uncertainty, and presents clear reasoning.
     */
    String generateExplanation(
            StructuredHealthContext context,
            List<String> verifiedObservations,
            List<String> interpretations,
            ModelProvider provider
    );
}
