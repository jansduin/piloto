package com.piloto.cdi.gateway.llm.dto;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.piloto.cdi.gateway.llm.util.LLMJsonUtils;

/**
 * Record que representa la respuesta normalizada de cualquier LLM.
 */
public record LLMResponse(
        boolean success,
        String content,
        Usage usage,
        Map<String, Object> metadata,
        LLMError error) {
    /**
     * Factory method para respuesta exitosa.
     */
    public static LLMResponse success(String content, int tokens, double cost, long latency, String provider,
            String model) {
        // Enforce cleanup at the boundary
        String cleanContent = LLMJsonUtils.cleanJson(content);
        return new LLMResponse(true, cleanContent, new Usage(tokens, cost, latency),
                Map.of("provider", provider, "model", model), null);
    }

    /**
     * Factory method para error.
     */
    public static LLMResponse failure(String provider, String model, String errorMessage) {
        return new LLMResponse(false, null, new Usage(0, 0.0, 0),
                Map.of("provider", provider, "model", model), new LLMError("system_error", errorMessage));
    }

    public record Usage(int tokens, double cost, long latency) {
    }

    public record LLMError(String code, String message) {
    }

    // Accessors for backward compatibility and convenience
    public String providerName() {
        return (String) metadata.getOrDefault("provider", "unknown");
    }

    public String modelName() {
        return (String) metadata.getOrDefault("model", "unknown");
    }

    public long latencyMs() {
        return usage != null ? usage.latency() : 0;
    }

    public int tokensUsed() {
        return usage != null ? usage.tokens() : 0;
    }

    public double costUSD() {
        return usage != null ? usage.cost() : 0.0;
    }
}
