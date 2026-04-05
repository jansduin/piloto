package com.piloto.cdi.gateway.llm.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.piloto.cdi.gateway.llm.LLMProvider;
import com.piloto.cdi.gateway.llm.dto.LLMRequest;
import com.piloto.cdi.gateway.llm.dto.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

/**
 * Proveedor de Anthropic optimizado para visión multimodal (Claude Sonnet 4.6).
 */
@Component
@ConditionalOnProperty(name = "piloto.llm.anthropic.enabled", havingValue = "true")
public class AnthropicProvider implements LLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(AnthropicProvider.class);

    private final String apiKey;
    private final String modelName;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnthropicProvider(
            @Value("${piloto.llm.anthropic.apiKey:}") String apiKey,
            @Value("${piloto.llm.anthropic.models.strategy:claude-3-5-sonnet-20241022}") String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("⚠️ AnthropicProvider habilitado pero sin API Key configurada.");
        } else {
            logger.info("🎨 AnthropicProvider cargado exitosamente (Modelo: {}).", modelName);
        }
    }

    @Override
    public LLMResponse generateCompletion(LLMRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            URL url = URI.create("https://api.anthropic.com/v1/messages").toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("x-api-key", apiKey);
            con.setRequestProperty("anthropic-version", "2023-06-01");
            con.setDoOutput(true);

            // Payload assembly
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", modelName);
            payload.put("max_tokens", request.maxTokens() > 0 ? request.maxTokens() : 1024);
            
            if (request.systemPrompt() != null && !request.systemPrompt().isEmpty()) {
                payload.put("system", request.systemPrompt());
            }

            ArrayNode messages = payload.putArray("messages");
            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            
            ArrayNode contentArray = userMessage.putArray("content");

            // Add Image if present
            if (request.imageUrl() != null && !request.imageUrl().isEmpty()) {
                try {
                    String imageUrl = request.imageUrl();
                    String mediaType = "image/jpeg"; // Default
                    String base64Image;

                    if (imageUrl.startsWith("data:")) {
                        // Extraer media type de data:image/png;base64,...
                        int typeEnd = imageUrl.indexOf(";");
                        if (typeEnd > 5) {
                            mediaType = imageUrl.substring(5, typeEnd);
                        }
                        base64Image = imageUrl.substring(imageUrl.indexOf(",") + 1);
                    } else {
                        base64Image = downloadAndEncodeImage(imageUrl);
                    }

                    ObjectNode imageBlock = contentArray.addObject();
                    imageBlock.put("type", "image");
                    ObjectNode source = imageBlock.putObject("source");
                    source.put("type", "base64");
                    source.put("media_type", mediaType);
                    source.put("data", base64Image);
                    logger.info("🖼️ Imagen ({}) adjuntada exitosamente a la solicitud de Anthropic.", mediaType);
                } catch (Exception e) {
                    logger.error("❌ Error procesando imagen para Anthropic: {}", e.getMessage());
                    // Fallback: Continue without image but log error
                }
            }

            // Add Text
            ObjectNode textBlock = contentArray.addObject();
            textBlock.put("type", "text");
            textBlock.put("text", request.prompt());

            String jsonPayload = objectMapper.writeValueAsString(payload);

            try (OutputStream os = con.getOutputStream()) {
                os.write(jsonPayload.getBytes("utf-8"));
            }

            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                try (InputStream is = con.getInputStream()) {
                    JsonNode root = objectMapper.readTree(is);
                    String content = extractTextFromAnthropic(root);
                    int tokens = root.path("usage").path("output_tokens").asInt();
                    return LLMResponse.success(content, tokens, 0.0, System.currentTimeMillis() - startTime,
                            getProviderName(), getModelName());
                }
            } else {
                String errorBody;
                try (Scanner s = new Scanner(con.getErrorStream()).useDelimiter("\\A")) {
                    errorBody = s.hasNext() ? s.next() : "no body";
                }
                logger.error("Anthropic API error {}: {}", responseCode, errorBody);
                return LLMResponse.failure(getProviderName(), getModelName(), "HTTP " + responseCode + ": " + errorBody);
            }
        } catch (Exception e) {
            logger.error("❌ Error en AnthropicProvider: {}", e.getMessage(), e);
            return LLMResponse.failure(getProviderName(), getModelName(), e.getMessage());
        }
    }

    private String downloadAndEncodeImage(String imageUrl) throws Exception {
        if (imageUrl.startsWith("data:")) {
            // It's already a data URI
            return imageUrl.substring(imageUrl.indexOf(",") + 1);
        }
        
        URL url = URI.create(imageUrl).toURL();
        try (InputStream is = url.openStream()) {
            byte[] bytes = is.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        }
    }

    private String extractTextFromAnthropic(JsonNode root) {
        JsonNode content = root.path("content");
        if (content.isArray() && !content.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : content) {
                if ("text".equals(part.path("type").asText())) {
                    sb.append(part.path("text").asText());
                }
            }
            return sb.toString();
        }
        return "Respuesta sin contenido de texto.";
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        throw new UnsupportedOperationException("Anthropic no soporta embeddings nativos a través de esta API.");
    }

    @Override
    public boolean isHealthy() {
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public String getProviderName() {
        return "anthropic";
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}
