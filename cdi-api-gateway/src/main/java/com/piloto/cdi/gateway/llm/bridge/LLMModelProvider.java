package com.piloto.cdi.gateway.llm.bridge;

import com.piloto.cdi.gateway.llm.LLMRole;
import com.piloto.cdi.gateway.llm.LLMRoleManager;
import com.piloto.cdi.gateway.llm.dto.LLMRequest;
import com.piloto.cdi.gateway.llm.dto.LLMResponse;
import com.piloto.cdi.kernel.orchestrator.ModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Adaptador que conecta el ModelProvider del Kernel con el LLMRoleManager del
 * Gateway.
 * Permite al ReasoningOrchestrator del Kernel usar los modelos delegados del
 * Gateway.
 */
public class LLMModelProvider extends ModelProvider {
    private static final Logger logger = LoggerFactory.getLogger(LLMModelProvider.class);

    private final LLMRoleManager roleManager;
    private final LLMRole defaultRole;
    private final String providerName;
    private final String modelName;

    public LLMModelProvider(LLMRoleManager roleManager, LLMRole defaultRole, String providerName, String modelName) {
        this.roleManager = roleManager;
        this.defaultRole = defaultRole;
        this.providerName = providerName;
        this.modelName = modelName;
    }

    @Override
    public CompletableFuture<String> generate(String systemPrompt, String userPrompt, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Cognitive Brain: Generating completion for role: {} using model: {}", defaultRole, modelName);

            String imageUrl = context.containsKey("imageUrl") ? (String) context.get("imageUrl") : null;
            LLMRequest request = new LLMRequest(userPrompt, systemPrompt, imageUrl, 0.7, 1000, Map.of());
            // Enriquecer request con metadata del contexto si es necesario

            LLMResponse response = roleManager.executeForRole(defaultRole, request);

            if (!response.success()) {
                throw new RuntimeException("LLM Generation failed: " +
                        (response.error() != null ? response.error().message() : "Unknown error"));
            }

            return response.content();
        });
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}
