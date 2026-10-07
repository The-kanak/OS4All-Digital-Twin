package org.os4all.modules.ai.provider;

import java.util.List;

public interface ModelProvider {

    /**
     * Provider identification key, e.g. "mock", "nebius".
     */
    String getProviderName();

    /**
     * Returns true if this provider is currently configured and operational.
     */
    boolean isAvailable();

    /**
     * Generates a raw completion given system instructions and prompt messages.
     */
    String generateChatCompletion(List<ChatMessage> messages, ModelParameters parameters);

    record ChatMessage(
            String role, // "system", "user", "assistant"
            String content
    ) {}

    record ModelParameters(
            String model,
            double temperature,
            int maxTokens
    ) {}
}
