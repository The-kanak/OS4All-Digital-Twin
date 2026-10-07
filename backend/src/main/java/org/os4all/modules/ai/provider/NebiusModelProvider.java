package org.os4all.modules.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.os4all.core.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.*;

/**
 * Model provider integration for Nebius AI Studio (supporting NVIDIA Nemotron-4-340B-Instruct).
 * Uses OpenAI-compatible chat completions REST API with Bearer token authentication.
 *
 * Handles:
 * - Structured prompts & structured JSON output request
 * - HTTP timeouts (connect and request timeout)
 * - Rate limiting (HTTP 429)
 * - Authentication errors (HTTP 401 / 403)
 * - Malformed model responses & markdown code fence stripping
 * - Diagnostic logging with zero secret/key leakage
 */
@Component("nebiusModelProvider")
public class NebiusModelProvider implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(NebiusModelProvider.class);

    private final String apiUrl;
    private final String apiKey;
    private final long timeoutMs;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public NebiusModelProvider(
            @Value("${app.ai.api-url:https://api.studio.nebius.ai/v1}") String apiUrl,
            @Value("${app.ai.api-key:}") String apiKey,
            @Value("${app.ai.timeout-ms:30000}") long timeoutMs,
            ObjectMapper objectMapper
    ) {
        this(apiUrl, apiKey, timeoutMs, objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(Math.min(timeoutMs, 10000)))
                        .build()
        );
    }

    /**
     * Constructor for unit testing and customized HttpClient injection.
     */
    public NebiusModelProvider(
            String apiUrl,
            String apiKey,
            long timeoutMs,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        String url = (apiUrl != null && !apiUrl.isBlank()) ? apiUrl.trim() : "https://api.studio.nebius.ai/v1";
        this.apiUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : 30000;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public String getProviderName() {
        return "nebius";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String generateChatCompletion(List<ChatMessage> messages, ModelParameters parameters) {
        if (!isAvailable()) {
            throw new ApiException("Nebius API key is not configured. Set NEBIUS_API_KEY environment variable.");
        }

        long startTime = System.currentTimeMillis();
        String maskedKey = maskApiKey(apiKey);
        log.info("Initiating Nebius AI chat completion: endpoint='{}/chat/completions', model='{}', key='{}'",
                apiUrl, parameters.model(), maskedKey);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", parameters.model());
            requestBody.put("temperature", parameters.temperature());
            requestBody.put("max_tokens", parameters.maxTokens());

            List<Map<String, String>> msgList = new ArrayList<>();
            for (ChatMessage m : messages) {
                msgList.add(Map.of("role", m.role(), "content", m.content()));
            }
            requestBody.put("messages", msgList);

            // Request structured JSON output
            Map<String, String> responseFormat = Map.of("type", "json_object");
            requestBody.put("response_format", responseFormat);

            String requestPayload = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/chat/completions"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = System.currentTimeMillis() - startTime;
            int statusCode = response.statusCode();

            log.info("Nebius AI response received in {}ms with HTTP status {}", latencyMs, statusCode);

            // Handle specific HTTP error status codes
            if (statusCode == 401 || statusCode == 403) {
                String errorDetail = extractErrorMessage(response.body());
                log.error("Nebius AI authentication failed (HTTP {}). Detail: {}. Check NEBIUS_API_KEY validity.", statusCode, errorDetail);
                throw new ApiException("Nebius AI Studio authentication failed (HTTP " + statusCode + "): " +
                        (errorDetail.isEmpty() ? "Invalid or expired API credentials." : errorDetail));
            }

            if (statusCode == 429) {
                String errorDetail = extractErrorMessage(response.body());
                log.warn("Nebius AI rate limit reached (HTTP 429). Detail: {}", errorDetail);
                throw new ApiException("Nebius AI rate limit exceeded (HTTP 429): " +
                        (errorDetail.isEmpty() ? "Please retry after backoff interval." : errorDetail));
            }

            if (statusCode == 404) {
                String errorDetail = extractErrorMessage(response.body());
                log.error("Nebius AI model not found (HTTP 404): model='{}', detail='{}'", parameters.model(), errorDetail);
                throw new ApiException("Nebius AI Studio model '" + parameters.model() + "' is unavailable or not found (HTTP 404): " + errorDetail);
            }

            if (statusCode < 200 || statusCode >= 300) {
                String errorDetail = extractErrorMessage(response.body());
                log.error("Nebius AI Studio returned error (HTTP {}): {}", statusCode, errorDetail);
                throw new ApiException("Nebius AI Studio API error (HTTP " + statusCode + "): " +
                        (errorDetail.isEmpty() ? response.body() : errorDetail));
            }

            // Parse response body and extract message content
            JsonNode root;
            try {
                root = objectMapper.readTree(response.body());
            } catch (Exception pe) {
                log.error("Failed to parse Nebius response as JSON: {}", response.body());
                throw new ApiException("Nebius AI returned an invalid non-JSON HTTP response body.");
            }

            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.error("Nebius AI response missing 'choices' array: {}", response.body());
                throw new ApiException("Received empty or malformed completion choices from Nebius AI Studio.");
            }

            JsonNode messageNode = choices.get(0).path("message");
            String rawContent = messageNode.path("content").asText("");
            if (rawContent.isBlank()) {
                log.error("Nebius AI response choice contains empty message content: {}", response.body());
                throw new ApiException("Nebius AI returned an empty message content response.");
            }

            // Strip markdown code fences if present (e.g. ```json ... ```)
            String cleanedContent = stripMarkdownCodeFences(rawContent);

            // Validate that the stripped content is valid JSON
            try {
                objectMapper.readTree(cleanedContent);
            } catch (Exception je) {
                log.warn("Nebius AI raw content could not be parsed as valid JSON: {}", cleanedContent);
                // Return cleanedContent; the workflow's strict validator will handle fallback if needed
            }

            return cleanedContent;

        } catch (HttpTimeoutException te) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Nebius AI request timed out after {}ms (timeout threshold: {}ms)", latencyMs, timeoutMs);
            throw new ApiException("Nebius AI request timed out after " + timeoutMs + "ms. Consider increasing AI_TIMEOUT_MS.");
        } catch (ApiException ae) {
            throw ae;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Nebius AI request thread interrupted: {}", ie.getMessage());
            throw new ApiException("Nebius AI request execution interrupted: " + ie.getMessage());
        } catch (Exception e) {
            log.error("Communication failure with Nebius AI Studio: {}", e.getMessage(), e);
            throw new ApiException("Failed to communicate with Nebius AI Studio: " + e.getMessage());
        }
    }

    /**
     * Strips leading/trailing ```json ... ``` or ``` ... ``` markdown fences that LLMs sometimes generate.
     */
    public static String stripMarkdownCodeFences(String content) {
        if (content == null) return "";
        String trimmed = content.trim();

        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7).trim();
        } else if (trimmed.startsWith("```JSON")) {
            trimmed = trimmed.substring(7).trim();
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3).trim();
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }

        return trimmed;
    }

    /**
     * Extracts readable error messages from JSON error responses returned by Nebius.
     */
    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("error")) {
                JsonNode errorNode = root.get("error");
                if (errorNode.isObject() && errorNode.has("message")) {
                    return errorNode.get("message").asText();
                } else if (errorNode.isTextual()) {
                    return errorNode.asText();
                }
            }
            if (root.has("detail")) {
                JsonNode detailNode = root.get("detail");
                if (detailNode.isTextual()) {
                    return detailNode.asText();
                }
                return detailNode.toString();
            }
            if (root.has("message")) {
                return root.get("message").asText();
            }
        } catch (Exception ignored) {
            // Not a JSON error body, return raw or substring
        }
        return responseBody.length() > 300 ? responseBody.substring(0, 300) + "..." : responseBody;
    }

    /**
     * Masks the API key so it can be logged safely without leaking secrets.
     */
    private String maskApiKey(String key) {
        if (key == null || key.isBlank()) return "[NONE]";
        if (key.length() <= 8) return "***";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}
