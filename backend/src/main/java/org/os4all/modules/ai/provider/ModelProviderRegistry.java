package org.os4all.modules.ai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory and registry for resolving the active ModelProvider based on environment configuration.
 */
@Service
public class ModelProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModelProviderRegistry.class);

    private final Map<String, ModelProvider> providerMap;
    private final String configuredProvider;

    public ModelProviderRegistry(
            List<ModelProvider> providers,
            @Value("${app.ai.provider:mock}") String configuredProvider
    ) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(ModelProvider::getProviderName, Function.identity()));
        this.configuredProvider = configuredProvider.trim().toLowerCase();
        log.info("Initialized ModelProviderRegistry with providers: {}. Active default: '{}'",
                providerMap.keySet(), this.configuredProvider);
    }

    public ModelProvider getActiveProvider() {
        if ("nebius".equalsIgnoreCase(configuredProvider)) {
            ModelProvider nebiusProvider = providerMap.get("nebius");
            if (nebiusProvider == null) {
                throw new org.os4all.core.exception.ApiException(
                        "Nebius AI provider is configured (OS4ALL_AI_PROVIDER=nebius), but NebiusModelProvider bean is not registered."
                );
            }
            if (!nebiusProvider.isAvailable()) {
                throw new org.os4all.core.exception.ApiException(
                        "Nebius AI provider is configured (OS4ALL_AI_PROVIDER=nebius), but NEBIUS_API_KEY is not configured or empty. " +
                        "Please provide a valid NEBIUS_API_KEY environment variable to use the Nebius provider."
                );
            }
            return nebiusProvider;
        }

        ModelProvider provider = providerMap.get(configuredProvider);
        if (provider != null && provider.isAvailable()) {
            return provider;
        }

        if ("mock".equalsIgnoreCase(configuredProvider)) {
            ModelProvider mockProvider = providerMap.get("mock");
            if (mockProvider != null) {
                return mockProvider;
            }
        }

        throw new org.os4all.core.exception.ApiException("Configured AI provider '" + configuredProvider + "' is not available or registered.");
    }

    public boolean isNebiusAvailable() {
        ModelProvider nebiusProvider = providerMap.get("nebius");
        return nebiusProvider != null && nebiusProvider.isAvailable();
    }

    public String getConfiguredProviderName() {
        return configuredProvider;
    }
}
