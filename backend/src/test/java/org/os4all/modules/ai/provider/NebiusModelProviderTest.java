package org.os4all.modules.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.os4all.core.exception.ApiException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class NebiusModelProviderTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private HttpResponse mockHttpResponse;

    private ObjectMapper objectMapper;
    private NebiusModelProvider provider;

    private static final String SAMPLE_NEBIUS_SUCCESS_JSON = """
            {
              "id": "chatcmpl-test-12345",
              "object": "chat.completion",
              "created": 1700000000,
              "model": "nvidia/Llama-3_1-Nemotron-70B-Instruct",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "{\\"summary\\": \\"Autonomic balance stable.\\", \\"urgency\\": \\"ROUTINE\\", \\"confidence\\": 0.95}"
                  },
                  "finish_reason": "stop"
                }
              ],
              "usage": {
                "prompt_tokens": 120,
                "completion_tokens": 45,
                "total_tokens": 165
              }
            }
            """;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        provider = new NebiusModelProvider(
                "https://api.studio.nebius.ai/v1",
                "test-nebius-api-key-12345678",
                5000,
                objectMapper,
                mockHttpClient
        );
    }

    @Test
    @DisplayName("Provider identification and availability contract")
    void testProviderNameAndAvailability() {
        assertEquals("nebius", provider.getProviderName());
        assertTrue(provider.isAvailable());

        NebiusModelProvider unconfigured = new NebiusModelProvider(
                "https://api.studio.nebius.ai/v1",
                "",
                5000,
                objectMapper,
                mockHttpClient
        );
        assertFalse(unconfigured.isAvailable());
    }

    @Test
    @DisplayName("Throws ApiException if generateChatCompletion is called without API key")
    void shouldThrowIfApiKeyMissing() {
        NebiusModelProvider unconfigured = new NebiusModelProvider(
                "https://api.studio.nebius.ai/v1",
                null,
                5000,
                objectMapper,
                mockHttpClient
        );

        ApiException ex = assertThrows(ApiException.class, () ->
                unconfigured.generateChatCompletion(
                        List.of(new ModelProvider.ChatMessage("user", "Hello")),
                        new ModelProvider.ModelParameters("nvidia/Llama-3_1-Nemotron-70B-Instruct", 0.2, 500)
                ));

        assertTrue(ex.getMessage().contains("Nebius API key is not configured"));
    }

    @Test
    @DisplayName("Successful completion sends structured JSON request and parses content cleanly")
    void shouldSuccessfullySendAndParseCompletion() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(SAMPLE_NEBIUS_SUCCESS_JSON);
        when(mockHttpClient.send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);

        List<ModelProvider.ChatMessage> messages = List.of(
                new ModelProvider.ChatMessage("system", "You are OS4All Health Intelligence."),
                new ModelProvider.ChatMessage("user", "Analyze baseline stability.")
        );
        ModelProvider.ModelParameters params = new ModelProvider.ModelParameters(
                "nvidia/Llama-3_1-Nemotron-70B-Instruct",
                0.2,
                1000
        );

        String result = provider.generateChatCompletion(messages, params);

        assertNotNull(result);
        assertTrue(result.contains("Autonomic balance stable."));
        assertTrue(result.contains("ROUTINE"));

        // Verify HTTP request structure & headers
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any());
        HttpRequest sentRequest = requestCaptor.getValue();

        assertEquals("POST", sentRequest.method());
        assertEquals("https://api.studio.nebius.ai/v1/chat/completions", sentRequest.uri().toString());
        assertTrue(sentRequest.headers().firstValue("Authorization").orElse("").startsWith("Bearer test-nebius"));
        assertEquals("application/json", sentRequest.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    @DisplayName("Handles markdown code fences generated by model and returns clean JSON")
    void shouldStripMarkdownFencesFromModelContent() throws Exception {
        String wrappedJson = """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "```json\\n{\\"summary\\": \\"Resting HR normal.\\"}\\n```"
                      }
                    }
                  ]
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(wrappedJson);
        when(mockHttpClient.send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);

        String result = provider.generateChatCompletion(
                List.of(new ModelProvider.ChatMessage("user", "test")),
                new ModelProvider.ModelParameters("nvidia/Llama-3_1-Nemotron-70B-Instruct", 0.2, 500)
        );

        assertEquals("{\"summary\": \"Resting HR normal.\"}", result);
    }

    @Test
    @DisplayName("Handles HTTP 401 / 403 Authentication Error with diagnostic message")
    void shouldHandleAuthenticationErrors() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(401);
        when(mockHttpResponse.body()).thenReturn("{\"error\": {\"message\": \"Invalid API key provided\"}}");
        when(mockHttpClient.send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);

        ApiException ex = assertThrows(ApiException.class, () ->
                provider.generateChatCompletion(
                        List.of(new ModelProvider.ChatMessage("user", "test")),
                        new ModelProvider.ModelParameters("nvidia/Llama-3_1-Nemotron-70B-Instruct", 0.2, 500)
                ));

        assertTrue(ex.getMessage().contains("authentication failed"));
    }

    @Test
    @DisplayName("Handles HTTP 429 Rate Limit with informative error")
    void shouldHandleRateLimitErrors() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(429);
        when(mockHttpResponse.body()).thenReturn("{\"error\": {\"message\": \"Rate limit exceeded\"}}");
        when(mockHttpClient.send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);

        ApiException ex = assertThrows(ApiException.class, () ->
                provider.generateChatCompletion(
                        List.of(new ModelProvider.ChatMessage("user", "test")),
                        new ModelProvider.ModelParameters("nvidia/Llama-3_1-Nemotron-70B-Instruct", 0.2, 500)
                ));

        assertTrue(ex.getMessage().contains("rate limit exceeded (HTTP 429)"));
    }

    @Test
    @DisplayName("Handles HTTP request timeout gracefully")
    void shouldHandleTimeoutGracefully() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new HttpTimeoutException("Request timed out"));

        ApiException ex = assertThrows(ApiException.class, () ->
                provider.generateChatCompletion(
                        List.of(new ModelProvider.ChatMessage("user", "test")),
                        new ModelProvider.ModelParameters("nvidia/Llama-3_1-Nemotron-70B-Instruct", 0.2, 500)
                ));

        assertTrue(ex.getMessage().contains("timed out"));
    }

    @Test
    @DisplayName("Handles malformed response without choices array")
    void shouldHandleMalformedResponseWithoutChoices() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"unexpected\": \"payload\"}");
        when(mockHttpClient.send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);

        ApiException ex = assertThrows(ApiException.class, () ->
                provider.generateChatCompletion(
                        List.of(new ModelProvider.ChatMessage("user", "test")),
                        new ModelProvider.ModelParameters("nvidia/Llama-3_1-Nemotron-70B-Instruct", 0.2, 500)
                ));

        assertTrue(ex.getMessage().contains("empty or malformed completion choices"));
    }

    @Test
    @DisplayName("Handles HTTP 404 Model Unavailable and reports exact error rather than silently substituting")
    void shouldReportExactErrorWhenModelUnavailable() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(404);
        when(mockHttpResponse.body()).thenReturn("{\"error\": {\"message\": \"Model 'nvidia/Llama-3_1-Nemotron-70B-Instruct' does not exist or is deprecated\"}}");
        when(mockHttpClient.send(any(HttpRequest.class), any())).thenReturn(mockHttpResponse);

        ApiException ex = assertThrows(ApiException.class, () ->
                provider.generateChatCompletion(
                        List.of(new ModelProvider.ChatMessage("user", "test")),
                        new ModelProvider.ModelParameters("nvidia/Llama-3_1-Nemotron-70B-Instruct", 0.2, 500)
                ));

        assertTrue(ex.getMessage().contains("is unavailable or not found (HTTP 404)"));
        assertTrue(ex.getMessage().contains("nvidia/Llama-3_1-Nemotron-70B-Instruct"));
    }
}
