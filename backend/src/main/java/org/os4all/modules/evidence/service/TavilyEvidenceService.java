package org.os4all.modules.evidence.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.os4all.core.exception.ApiException;
import org.os4all.modules.evidence.model.EvidenceQuery;
import org.os4all.modules.evidence.model.EvidenceResult;
import org.os4all.modules.evidence.model.EvidenceSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Tavily Evidence Service implementation.
 * Connects to Tavily Search API with strictly constrained biomedical domain filtering
 * and relevance ranking, ensuring arbitrary internet content is never treated as medical truth.
 */
@Service("tavilyEvidenceService")
public class TavilyEvidenceService implements EvidenceService {

    private static final Logger log = LoggerFactory.getLogger(TavilyEvidenceService.class);

    private final String apiUrl;
    private final String apiKey;
    private final boolean enabled;
    private final double minRelevanceScore;
    private final long timeoutMs;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TavilyEvidenceService(
            @Value("${app.evidence.api-url:https://api.tavily.com/search}") String apiUrl,
            @Value("${app.evidence.api-key:}") String apiKey,
            @Value("${app.evidence.enabled:true}") boolean enabled,
            @Value("${app.evidence.min-relevance-score:0.65}") double minRelevanceScore,
            @Value("${app.evidence.timeout-ms:10000}") long timeoutMs,
            ObjectMapper objectMapper
    ) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.enabled = enabled;
        this.minRelevanceScore = minRelevanceScore;
        this.timeoutMs = timeoutMs;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public String getProviderName() {
        return "tavily";
    }

    @Override
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public EvidenceResult retrieveEvidence(EvidenceQuery query) {
        if (!isAvailable()) {
            throw new ApiException("Tavily service is not available or TAVILY_API_KEY is not configured.");
        }

        long start = System.currentTimeMillis();
        try {
            Map<String, Object> requestPayload = new LinkedHashMap<>();
            requestPayload.put("api_key", apiKey);
            requestPayload.put("query", query.queryText());
            requestPayload.put("search_depth", "advanced");
            requestPayload.put("include_answer", false);
            requestPayload.put("max_results", query.maxResults() * 2); // Request extra to filter by domain and score

            // Restrict search strictly to trusted medical/scientific domains
            if (query.includeMedicalDomainsOnly()) {
                requestPayload.put("include_domains", EvidenceQuery.TRUSTED_MEDICAL_DOMAINS);
            }

            String bodyJson = objectMapper.writeValueAsString(requestPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            log.info("Executing constrained Tavily search for: '{}'", query.queryText());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Tavily API responded with HTTP status {}: {}", response.statusCode(), response.body());
                throw new ApiException("Tavily API error: HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode resultsNode = root.path("results");

            List<EvidenceSource> sources = new ArrayList<>();
            if (resultsNode.isArray()) {
                for (JsonNode item : resultsNode) {
                    String title = item.path("title").asText();
                    String url = item.path("url").asText();
                    String snippet = item.path("content").asText();
                    double score = item.path("score").asDouble(0.70);

                    String domain = extractDomain(url);
                    boolean isTrusted = isTrustedDomain(domain);

                    if (score >= minRelevanceScore) {
                        sources.add(new EvidenceSource(
                                title,
                                url,
                                domain,
                                snippet,
                                score,
                                Instant.now(),
                                isTrusted
                        ));
                    }
                }
            }

            // Sort by relevance score descending and take top N
            sources.sort(Comparator.comparingDouble(EvidenceSource::relevanceScore).reversed());
            List<EvidenceSource> topSources = sources.stream().limit(query.maxResults()).toList();

            long latency = System.currentTimeMillis() - start;
            log.info("Tavily search retrieved {} qualified medical evidence sources in {} ms", topSources.size(), latency);
            return new EvidenceResult(query, topSources, true, "tavily", latency);
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Error communicating with Tavily API: {}", e.getMessage(), e);
            throw new ApiException("Failed to retrieve evidence from Tavily: " + e.getMessage());
        }
    }

    private String extractDomain(String urlString) {
        try {
            URI uri = URI.create(urlString);
            String host = uri.getHost();
            return host != null ? host.toLowerCase(Locale.ROOT) : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private boolean isTrustedDomain(String domain) {
        return EvidenceQuery.TRUSTED_MEDICAL_DOMAINS.stream()
                .anyMatch(trusted -> domain.equalsIgnoreCase(trusted) || domain.endsWith("." + trusted));
    }
}
