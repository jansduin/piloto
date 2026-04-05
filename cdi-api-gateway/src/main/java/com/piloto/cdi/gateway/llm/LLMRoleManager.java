package com.piloto.cdi.gateway.llm;

import com.piloto.cdi.gateway.llm.dto.LLMRequest;
import com.piloto.cdi.gateway.llm.dto.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Orquestador de roles LLM.
 * Decide qué proveedor usar para cada tarea específica.
 */
@Service
public class LLMRoleManager {
    private static final Logger logger = LoggerFactory.getLogger(LLMRoleManager.class);

    private final LLMProviderFactory providerFactory;
    private final Map<LLMRole, String> roleMapping = new HashMap<>();

    @Autowired
    public LLMRoleManager(
            LLMProviderFactory providerFactory,
            @Value("${piloto.llm.role-mapping.planner:google}") String plannerProvider,
            @Value("${piloto.llm.role-mapping.chat_agent:google}") String chatProvider,
            @Value("${piloto.llm.role-mapping.validator:google}") String validatorProvider,
            @Value("${piloto.llm.role-mapping.proposer:google}") String proposerProvider,
            @Value("${piloto.llm.role-mapping.critic:google}") String criticProvider,
            @Value("${piloto.llm.role-mapping.verifier:google}") String verifierProvider,
            @Value("${piloto.llm.role-mapping.arbiter:google}") String arbiterProvider,
            @Value("${piloto.llm.role-mapping.vision_analyzer:anthropic}") String visionProvider,
            @Value("${piloto.llm.role-mapping.fallback:ollama}") String fallbackProvider,
            @Value("${piloto.llm.role-mapping.optimizer:google-execution}") String optimizerProvider) {

        this.providerFactory = providerFactory;

        roleMapping.put(LLMRole.PLANNER, plannerProvider);
        roleMapping.put(LLMRole.CHAT_AGENT, chatProvider);
        roleMapping.put(LLMRole.VALIDATOR, validatorProvider);
        roleMapping.put(LLMRole.PROPOSER, proposerProvider);
        roleMapping.put(LLMRole.CRITIC, criticProvider);
        roleMapping.put(LLMRole.VERIFIER, verifierProvider);
        roleMapping.put(LLMRole.ARBITER, arbiterProvider);
        roleMapping.put(LLMRole.VISION_ANALYZER, visionProvider);
        roleMapping.put(LLMRole.OPTIMIZER, optimizerProvider);
        roleMapping.put(LLMRole.FALLBACK, fallbackProvider);

        logger.info("✅ LLMRoleManager configurado con mappings: {}", roleMapping);
    }

    /**
     * Ejecuta una solicitud LLM para un rol específico.
     */
    public LLMResponse executeForRole(LLMRole role, LLMRequest request) {
        String providerName = roleMapping.getOrDefault(role, roleMapping.get(LLMRole.FALLBACK));
        LLMProvider provider = providerFactory.getProvider(providerName);

        if (provider == null || !provider.isHealthy()) {
            logger.warn("⚠️ Provider {} para rol {} no disponible o unhealthy. Reintentando con fallback...",
                    providerName, role);
            provider = providerFactory.getProvider(roleMapping.get(LLMRole.FALLBACK));
        }

        if (provider == null) {
            return LLMResponse.failure("system", "none", "No LLM provider available for role: " + role);
        }

        return provider.generateCompletion(request);
    }
}
