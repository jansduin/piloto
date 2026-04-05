package com.piloto.cdi.gateway.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry y Factory para proveedores de LLM.
 * Registra automáticamente todos los beans que implementen LLMProvider.
 */
@Component
public class LLMProviderFactory {
    private static final Logger logger = LoggerFactory.getLogger(LLMProviderFactory.class);

    private final Map<String, LLMProvider> providers = new ConcurrentHashMap<>();

    @Autowired
    public LLMProviderFactory(List<LLMProvider> providerList) {
        for (LLMProvider provider : providerList) {
            String key = provider.getProviderName().toLowerCase();
            providers.put(key, provider);
            logger.info("✅ LLM Provider registrado: {} (Modelo: {})", key, provider.getModelName());
        }
    }

    /**
     * Obtiene un proveedor por su nombre único.
     */
    public LLMProvider getProvider(String name) {
        return providers.get(name.toLowerCase());
    }

    /**
     * Retorna todos los proveedores registrados.
     */
    public Map<String, LLMProvider> getAllProviders() {
        return Map.copyOf(providers);
    }

    /**
     * Retorna solo los proveedores que están saludables.
     */
    public List<LLMProvider> getHealthyProviders() {
        return providers.values().stream()
                .filter(LLMProvider::isHealthy)
                .collect(Collectors.toList());
    }
}
