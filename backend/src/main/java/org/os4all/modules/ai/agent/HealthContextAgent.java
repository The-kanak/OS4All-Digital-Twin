package org.os4all.modules.ai.agent;

import org.os4all.modules.ai.model.StructuredHealthContext;

import java.util.UUID;

public interface HealthContextAgent {

    /**
     * Extracts and constructs a curated, strictly read-only, anonymized StructuredHealthContext
     * for the given user. The AI receives only this structured snapshot, never direct database queries.
     */
    StructuredHealthContext buildHealthContext(UUID userId);
}
