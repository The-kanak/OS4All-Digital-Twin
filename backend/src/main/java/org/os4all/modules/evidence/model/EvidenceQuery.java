package org.os4all.modules.evidence.model;

import java.util.List;

/**
 * Structured query constructed by the evidence pipeline.
 * Constrained specifically to authoritative scientific and medical research domains
 * to prevent the AI from ingesting arbitrary unvetted web blogs or medical misinformation.
 */
public record EvidenceQuery(
        String queryText,
        List<String> topicKeywords,
        String physiologicalContext,
        boolean includeMedicalDomainsOnly,
        int maxResults
) {
    public static final List<String> TRUSTED_MEDICAL_DOMAINS = List.of(
            "nih.gov",
            "ncbi.nlm.nih.gov",
            "pubmed.ncbi.nlm.nih.gov",
            "nature.com",
            "nejm.org",
            "thelancet.com",
            "ahajournals.org",
            "diabetesjournals.org",
            "frontiersin.org",
            "bmj.com",
            "cdc.gov",
            "who.int"
    );

    public static EvidenceQuery of(String queryText, List<String> topicKeywords, String physiologicalContext) {
        return new EvidenceQuery(queryText, topicKeywords, physiologicalContext, true, 3);
    }
}
