package org.os4all.modules.evidence.model;

import java.util.List;

/**
 * Filtered, ranked outcome of an evidence retrieval execution.
 */
public record EvidenceResult(
        EvidenceQuery query,
        List<EvidenceSource> sources,
        boolean retrievedFromExternalApi,
        String providerName,
        long latencyMs
) {}
