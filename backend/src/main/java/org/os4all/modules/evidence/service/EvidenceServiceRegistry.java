package org.os4all.modules.evidence.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry resolving the active EvidenceService provider based on environment configuration.
 */
@Service
public class EvidenceServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(EvidenceServiceRegistry.class);

    private final Map<String, EvidenceService> providerMap;
    private final String configuredProvider;
    private final boolean evidenceEnabled;

    public EvidenceServiceRegistry(
            List<EvidenceService> providers,
            @Value("${app.evidence.provider:mock}") String configuredProvider,
            @Value("${app.evidence.enabled:true}") boolean evidenceEnabled
    ) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(EvidenceService::getProviderName, Function.identity()));
        this.configuredProvider = configuredProvider.trim().toLowerCase();
        this.evidenceEnabled = evidenceEnabled;
        log.info("Initialized EvidenceServiceRegistry with providers: {}. Active default: '{}', enabled: {}",
                providerMap.keySet(), this.configuredProvider, this.evidenceEnabled);
    }

    public boolean isEvidenceEnabled() {
        return evidenceEnabled;
    }

    public EvidenceService getActiveService() {
        if (!evidenceEnabled) {
            return null;
        }

        EvidenceService service = providerMap.get(configuredProvider);
        if (service != null && service.isAvailable()) {
            return service;
        }

        // Graceful fallback to mock provider if configured provider (e.g. Tavily without API key) is unavailable
        log.warn("Configured evidence provider '{}' is not available. Falling back to 'mock' evidence provider.", configuredProvider);
        EvidenceService mockService = providerMap.get("mock");
        if (mockService != null) {
            return mockService;
        }

        throw new IllegalStateException("No evidence provider is available.");
    }
}
