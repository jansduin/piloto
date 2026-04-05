package com.piloto.cdi.gateway.llm.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piloto.cdi.gateway.llm.LLMProvider;
import com.piloto.cdi.gateway.llm.dto.LLMRequest;
import com.piloto.cdi.gateway.llm.dto.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Proveedor local basado en Ollama.
 */
@Component
@ConditionalOnProperty(name = "piloto.llm.ollama.enabled", havingValue = "true")
public class OllamaProvider implements LLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(OllamaProvider.class);

    private final String baseUrl;
    private final String modelName;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaProvider(
            @Value("${piloto.llm.ollama.baseUrl:http://localhost:11434}") String baseUrl,
            @Value("${piloto.llm.ollama.models.fallback:llama3}") String modelName) {
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();

        logger.info("✅ OllamaProvider configurado (BaseURL: {}, Modelo: {})", baseUrl, modelName);
    }

    @Override
    public LLMResponse generateCompletion(LLMRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", modelName);
            body.put("prompt", request.prompt());
            body.put("system", request.systemPrompt());
            body.put("stream", false);

            Map<String, Object> options = new HashMap<>();
            options.put("temperature", request.temperature());
            body.put("options", options);

            String responseJson = restTemplate.postForObject(baseUrl + "/api/generate", body, String.class);
            JsonNode root = objectMapper.readTree(responseJson);

            String content = root.path("response").asText();
            long latency = System.currentTimeMillis() - startTime;

            return LLMResponse.success(content, 0, 0.0, latency, getProviderName(), getModelName());

        } catch (Exception e) {
            logger.error("Error en Ollama generation: {}", e.getMessage());
            return LLMResponse.failure(getProviderName(), getModelName(), e.getMessage());
        }
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        throw new UnsupportedOperationException("Embeddings con Ollama aún no implementado.");
    }

    @Override
    public boolean isHealthy() {
        try {
            String status = restTemplate.getForObject(baseUrl + "/api/tags", String.class);
            return status != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "ollama";
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}
