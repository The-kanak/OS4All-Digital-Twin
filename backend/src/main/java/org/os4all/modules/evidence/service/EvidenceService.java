package org.os4all.modules.evidence.service;

import org.os4all.modules.evidence.model.EvidenceQuery;
import org.os4all.modules.evidence.model.EvidenceResult;

public interface EvidenceService {

    /**
     * Provider identification key, e.g. "mock", "tavily".
     */
    String getProviderName();

    /**
     * Returns true if this provider is configured and enabled.
     */
    boolean isAvailable();

    /**
     * Retrieves and ranks external scientific evidence for the structured query.
     */
    EvidenceResult retrieveEvidence(EvidenceQuery query);
}
