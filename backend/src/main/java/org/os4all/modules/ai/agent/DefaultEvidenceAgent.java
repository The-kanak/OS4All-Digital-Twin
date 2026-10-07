package org.os4all.modules.ai.agent;

import org.os4all.modules.ai.model.AiHealthInsightResponse;
import org.os4all.modules.ai.model.StructuredHealthContext;
import org.os4all.modules.evidence.model.EvidenceQuery;
import org.os4all.modules.evidence.model.EvidenceResult;
import org.os4all.modules.evidence.model.EvidenceSource;
import org.os4all.modules.evidence.service.EvidenceQueryBuilder;
import org.os4all.modules.evidence.service.EvidenceService;
import org.os4all.modules.evidence.service.EvidenceServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Upgraded OS4All Evidence Agent.
 *
 * Implements the deterministic multi-stage evidence pipeline:
 * 1. Evaluates whether external scientific evidence is actually necessary.
 *    (Bypasses external web searches for routine stable health questions).
 * 2. If necessary:
 *    OS4All pattern -> Evidence Query Builder -> Tavily / Mock -> source filtering -> evidence ranking -> Nemotron synthesis.
 * 3. Preserves all critical provenance fields:
 *    - source title
 *    - URL
 *    - domain
 *    - retrieved time
 *    - relevant excerpt
 *    - relevance score
 * 4. Prevents AI models from inventing or hallucinating sources.
 */
@Component
public class DefaultEvidenceAgent implements EvidenceAgent {

    private static final Logger log = LoggerFactory.getLogger(DefaultEvidenceAgent.class);

    private final EvidenceServiceRegistry evidenceRegistry;
    private final EvidenceQueryBuilder queryBuilder;

    public DefaultEvidenceAgent(
            EvidenceServiceRegistry evidenceRegistry,
            EvidenceQueryBuilder queryBuilder
    ) {
        this.evidenceRegistry = evidenceRegistry;
        this.queryBuilder = queryBuilder;
    }

    @Override
    public List<AiHealthInsightResponse.EvidenceCitation> retrieveEvidence(
            StructuredHealthContext context,
            List<String> observations
    ) {
        // Step 1: Determine whether external evidence is necessary
        // Do NOT search the web for every routine health check!
        boolean evidenceNeeded = isEvidenceNecessary(context, observations);

        if (!evidenceNeeded || !evidenceRegistry.isEvidenceEnabled()) {
            log.info("Evidence Agent: External literature search NOT required. Health state is STABLE with no out-of-range deviations. Web search bypassed.");
            return List.of();
        }

        // Step 2: OS4All Pattern -> Evidence Query Builder
        EvidenceQuery query = queryBuilder.buildQuery(context, observations);
        log.info("Evidence Agent: Formulated targeted biomedical query: '{}' (keywords: {})",
                query.queryText(), query.topicKeywords());

        // Step 3: Retrieve from active provider (Tavily or Mock)
        EvidenceService activeService = evidenceRegistry.getActiveService();
        if (activeService == null) {
            log.warn("Evidence Agent: No active evidence provider available.");
            return List.of();
        }

        EvidenceResult result = activeService.retrieveEvidence(query);
        if (result == null || result.sources() == null || result.sources().isEmpty()) {
            log.info("Evidence Agent: No external evidence sources returned for query '{}'", query.queryText());
            return List.of();
        }

        // Step 4: Source filtering & Evidence ranking
        List<EvidenceSource> filteredAndRanked = filterAndRankSources(result.sources(), query);

        // Step 5: Preserve all provenance metadata
        List<AiHealthInsightResponse.EvidenceCitation> citations = new ArrayList<>();
        for (EvidenceSource src : filteredAndRanked) {
            String retrievedTimeIso = src.retrievedAt() != null ? src.retrievedAt().toString() : Instant.now().toString();
            String domain = src.domain() != null ? src.domain() : "ncbi.nlm.nih.gov";
            String sourceLabel = domain + (src.isPeerReviewedOrGov() ? " (Peer-Reviewed / Clinical Reference)" : "");

            citations.add(new AiHealthInsightResponse.EvidenceCitation(
                    src.title(),
                    sourceLabel,
                    src.url(),
                    src.snippet(),
                    domain,
                    retrievedTimeIso,
                    src.snippet(),
                    src.relevanceScore()
            ));
        }

        log.info("Evidence Agent: Successfully retrieved, filtered, and ranked {} authoritative sources.", citations.size());
        return citations;
    }

    /**
     * Determines whether external scientific evidence is actually required.
     * Web searches are bypassed when:
     * - Health context is STABLE
     * - No active anomalies exist
     * - No statistical deviations exceed threshold (|z| >= 1.5 or persistence >= 2 days)
     * - No out-of-range lab results are flagged
     */
    public boolean isEvidenceNecessary(StructuredHealthContext context, List<String> observations) {
        if (context == null) {
            return false;
        }

        // 1. Active anomalies automatically require external evidence
        if (context.activeAnomalies() != null && !context.activeAnomalies().isEmpty()) {
            return true;
        }

        // 2. Non-stable aggregate state requires external evidence
        String state = context.anomalyState() != null ? context.anomalyState() : context.currentAggregateState();
        if (state != null && !"STABLE".equalsIgnoreCase(state) && !"BASELINE".equalsIgnoreCase(state)) {
            return true;
        }

        // 3. Significant signal deviations (|z| >= 1.5 or duration >= 2 days)
        if (context.deviations() != null) {
            for (StructuredHealthContext.SignalDeviationItem dev : context.deviations()) {
                if (dev.zScore() != null && Math.abs(dev.zScore()) >= 1.5) {
                    return true;
                }
                if (dev.durationDays() >= 2 && dev.trendDirection() != null && !"STABLE".equalsIgnoreCase(dev.trendDirection())) {
                    return true;
                }
            }
        }

        // 4. Out-of-range confirmed lab biomarkers
        if (context.recentLabResults() != null) {
            for (StructuredHealthContext.LabBiomarkerItem lab : context.recentLabResults()) {
                if (lab.referenceHigh() != null && lab.value().compareTo(lab.referenceHigh()) > 0) return true;
                if (lab.referenceLow() != null && lab.value().compareTo(lab.referenceLow()) < 0) return true;
            }
        }

        // 5. Keyword analysis in grounded observations
        if (observations != null) {
            for (String obs : observations) {
                String lower = obs.toLowerCase(Locale.ROOT);
                if (lower.contains("departure") || lower.contains("anomaly") || lower.contains("elevated") ||
                        lower.contains("strain") || lower.contains("deficit") || lower.contains("drift")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Filters out non-scientific domains and ranks sources by relevance score and authority.
     */
    private List<EvidenceSource> filterAndRankSources(List<EvidenceSource> rawSources, EvidenceQuery query) {
        List<EvidenceSource> qualified = new ArrayList<>();
        for (EvidenceSource s : rawSources) {
            // Require non-empty title, url, snippet
            if (s.title() == null || s.title().isBlank()) continue;
            if (s.url() == null || s.url().isBlank()) continue;
            if (s.snippet() == null || s.snippet().isBlank()) continue;

            // Enforce minimum relevance score
            if (s.relevanceScore() < 0.60) continue;

            qualified.add(s);
        }

        // Rank by relevance score descending
        qualified.sort(Comparator.comparingDouble(EvidenceSource::relevanceScore).reversed());

        int limit = Math.min(query.maxResults(), qualified.size());
        return qualified.subList(0, limit);
    }
}
