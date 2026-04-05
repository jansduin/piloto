package com.piloto.cdi.gateway.llm.providers;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import com.piloto.cdi.gateway.llm.LLMProvider;
import com.piloto.cdi.gateway.llm.dto.LLMRequest;
import com.piloto.cdi.gateway.llm.dto.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Proveedor de OpenAI basado en gpt-4, gpt-3.5, etc.
 */
@Component
@ConditionalOnProperty(name = "piloto.llm.openai.enabled", havingValue = "true")
public class OpenAIProvider implements LLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIProvider.class);

    private final String apiKey;
    private final String modelName;
    private OpenAiService service;

    public OpenAIProvider(
            @Value("${piloto.llm.openai.apiKey:}") String apiKey,
            @Value("${piloto.llm.openai.models.planner:gpt-4o}") String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("⚠️ OpenAI API Key no configurada. OpenAIProvider no estará disponible.");
            return;
        }
        this.service = new OpenAiService(apiKey, Duration.ofSeconds(60));
        logger.info("✅ OpenAIProvider inicializado con éxito (Modelo: {})", modelName);
    }

    @Override
    public LLMResponse generateCompletion(LLMRequest request) {
        if (service == null) {
            return LLMResponse.failure(getProviderName(), getModelName(), "OpenAI API Key not set");
        }

        long startTime = System.currentTimeMillis();
        try {
            List<ChatMessage> messages = new ArrayList<>();
            if (request.systemPrompt() != null && !request.systemPrompt().isEmpty()) {
                messages.add(new ChatMessage("system", request.systemPrompt()));
            }
            messages.add(new ChatMessage("user", request.prompt()));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model(modelName)
                    .messages(messages)
                    .temperature(request.temperature())
                    .maxTokens(request.maxTokens())
                    .build();

            var result = service.createChatCompletion(completionRequest);
            String content = result.getChoices().get(0).getMessage().getContent();

            int tokens = (int) result.getUsage().getTotalTokens();
            long latency = System.currentTimeMillis() - startTime;

            // Estimación simple de costo (GPT-4o aprox $5.00 / 1M tokens)
            double cost = (tokens / 1_000_000.0) * 5.0;

            return LLMResponse.success(content, tokens, cost, latency, getProviderName(), getModelName());

        } catch (Exception e) {
            logger.error("Error en OpenAI generation: {}", e.getMessage());
            return LLMResponse.failure(getProviderName(), getModelName(), e.getMessage());
        }
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        // Implementación con service.createEmbeddings()
        throw new UnsupportedOperationException("Embeddings con OpenAI aún no implementado.");
    }

    @Override
    public boolean isHealthy() {
        return service != null; // Simplificado para Phase 1
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}
