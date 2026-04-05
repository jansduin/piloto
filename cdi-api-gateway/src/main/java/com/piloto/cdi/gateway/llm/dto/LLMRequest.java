package com.piloto.cdi.gateway.llm.dto;

import java.util.Map;

/**
 * Record que representa una solicitud a un LLM.
 */
public record LLMRequest(
        String prompt,
        String systemPrompt,
        String imageUrl,
        double temperature,
        int maxTokens,
        Map<String, Object> metadata) {
    /**
     * Constructor compacto para valores default.
     */
    public LLMRequest(String prompt, String systemPrompt) {
        this(prompt, systemPrompt, null, 0.7, 1000, Map.of());
    }
}
