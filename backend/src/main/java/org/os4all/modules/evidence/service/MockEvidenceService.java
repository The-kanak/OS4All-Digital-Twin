package org.os4all.modules.evidence.service;

import org.os4all.modules.evidence.model.EvidenceQuery;
import org.os4all.modules.evidence.model.EvidenceResult;
import org.os4all.modules.evidence.model.EvidenceSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Mock Evidence Service for deterministic local development and automated CI testing.
 * Provides curated, authoritative biomedical literature citations matching detected query patterns.
 */
@Service("mockEvidenceService")
public class MockEvidenceService implements EvidenceService {

    private static final Logger log = LoggerFactory.getLogger(MockEvidenceService.class);

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public EvidenceResult retrieveEvidence(EvidenceQuery query) {
        long start = System.currentTimeMillis();
        log.info("MockEvidenceService retrieving evidence for query: '{}'", query.queryText());

        String lower = query.queryText().toLowerCase(Locale.ROOT);
        List<EvidenceSource> sources = new ArrayList<>();

        if (lower.contains("heart rate") || lower.contains("hrv") || lower.contains("strain") || lower.contains("autonomic")) {
            sources.add(new EvidenceSource(
                    "Heart rate variability as a marker of autonomic recovery and physical stress",
                    "https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5900352/",
                    "ncbi.nlm.nih.gov",
                    "Suppression of resting nocturnal HRV paired with resting tachycardia serves as an early physiological marker of cumulative autonomic debt and delayed cardiovascular recovery.",
                    0.96,
                    Instant.now(),
                    true
            ));
            sources.add(new EvidenceSource(
                    "Physiological Sleep Loss and Sympathovagal Balance in Continuous Wearable Monitoring",
                    "https://pubmed.ncbi.nlm.nih.gov/31802914/",
                    "pubmed.ncbi.nlm.nih.gov",
                    "Consecutive nights of acute sleep restriction cause sustained sympathetic dominance, manifested in reduced rMSSD and elevated resting pulse rates.",
                    0.91,
                    Instant.now(),
                    true
            ));
        }

        if (lower.contains("glucose") || lower.contains("glycemic") || lower.contains("sugar") || lower.contains("hba1c")) {
            sources.add(new EvidenceSource(
                    "Glycemic Variability and Longitudinal Metabolic Health Profiles",
                    "https://diabetesjournals.org/care/article/46/Supplement_1/S97/148041",
                    "diabetesjournals.org",
                    "Evaluation of fasting plasma glucose and continuous glycemic variability patterns relative to established clinical reference ranges.",
                    0.94,
                    Instant.now(),
                    true
            ));
        }

        if (lower.contains("cholesterol") || lower.contains("triglycerides") || lower.contains("lipid")) {
            sources.add(new EvidenceSource(
                    "Circulation: Clinical Guidelines on Fasting and Non-Fasting Lipid Profiles",
                    "https://www.ahajournals.org/doi/10.1161/CIR.0000000000000625",
                    "ahajournals.org",
                    "Systematic evaluation of circulating cholesterol fractions and triglycerides relative to metabolic homeostasis and lifestyle factors.",
                    0.92,
                    Instant.now(),
                    true
            ));
        }

        if (sources.isEmpty()) {
            sources.add(new EvidenceSource(
                    "Personalized Digital Health Tracking and Biometric Baselines",
                    "https://www.nature.com/articles/s41591-020-0859-x",
                    "nature.com",
                    "Digital wearable devices tracking longitudinal individual baselines allow earlier and more individualized detection of physiological anomalies compared to static population averages.",
                    0.89,
                    Instant.now(),
                    true
            ));
        }

        // Limit results according to query constraints
        int limit = Math.min(query.maxResults(), sources.size());
        List<EvidenceSource> ranked = sources.subList(0, limit);

        long latency = System.currentTimeMillis() - start;
        return new EvidenceResult(query, ranked, false, "mock", latency);
    }
}
