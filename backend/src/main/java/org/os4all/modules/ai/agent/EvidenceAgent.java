package org.os4all.modules.ai.agent;

import org.os4all.modules.ai.model.AiHealthInsightResponse;
import org.os4all.modules.ai.model.StructuredHealthContext;

import java.util.List;

public interface EvidenceAgent {

    /**
     * Retrieves grounded scientific and clinical literature citations relevant to the detected signals.
     * Prevents AI hallucinations by anchoring evidence in verified references.
     */
    List<AiHealthInsightResponse.EvidenceCitation> retrieveEvidence(StructuredHealthContext context, List<String> observations);
}
