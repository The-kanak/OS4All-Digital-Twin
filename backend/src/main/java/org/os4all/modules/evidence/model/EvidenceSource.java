package org.os4all.modules.evidence.model;

import java.time.Instant;

/**
 * An individual evidence source retrieved from external literature/search engines.
 * Separated cleanly from the patient's private medical record.
 */
public record EvidenceSource(
        String title,
        String url,
        String domain,
        String snippet,
        double relevanceScore,
        Instant retrievedAt,
        boolean isPeerReviewedOrGov
) {}
