package com.piloto.cdi.gateway.llm.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.piloto.cdi.gateway.llm.LLMProvider;
import com.piloto.cdi.gateway.llm.dto.LLMRequest;
import com.piloto.cdi.gateway.llm.dto.LLMResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Proveedor de Google Gemini basado en el SDK de Vertex AI.
 */
@Component
@ConditionalOnProperty(name = "piloto.llm.google.enabled", havingValue = "true")
public class GoogleGeminiProvider implements LLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(GoogleGeminiProvider.class);

    private final String projectId;
    private final String location;
    private final String modelName;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private VertexAI vertexAI;
    private GenerativeModel model;
    private boolean useRestApi = false;

    public GoogleGeminiProvider(
            @Value("${piloto.llm.google.projectId:}") String projectId,
            @Value("${piloto.llm.google.location:us-central1}") String location,
            @Value("${piloto.llm.google.models.strategy:gemini-2.5-pro}") String modelName,
            @Value("${piloto.llm.google.apiKey:}") String apiKey) {
        this.projectId = projectId;
        this.location = location;
        this.modelName = modelName;
        this.apiKey = apiKey;
    }

    @PostConstruct
    public void init() {
        try {
            if (apiKey != null && !apiKey.isEmpty() && (projectId == null || projectId.isEmpty())) {
                logger.info("🔑 Modo API Key de Google AI Studio activado.");
                this.useRestApi = true;
            } else if (projectId != null && !projectId.isEmpty()) {
                logger.info("☁️ Modo Google Cloud (Vertex AI SDK) activado con Project: {}", projectId);
                this.vertexAI = new VertexAI(projectId, location);
                this.model = new GenerativeModel(modelName, vertexAI);
                this.useRestApi = false;
            } else {
                logger.warn("⚠️ No se detectó configuración válida (ApiKey o ProjectId).");
            }
        } catch (Exception e) {
            logger.error("❌ Error inicializando GoogleGeminiProvider: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void close() {
        if (vertexAI != null) {
            vertexAI.close();
        }
    }

    @Override
    public LLMResponse generateCompletion(LLMRequest request) {
        if (useRestApi) {
            return generateWithRest(request);
        } else if (model != null) {
            return generateWithSdk(request);
        }
        return LLMResponse.failure(getProviderName(), getModelName(), "Proveedor no configurado correctamente.");
    }

    private LLMResponse generateWithSdk(LLMRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            String fullPrompt = (request.systemPrompt() != null && !request.systemPrompt().isEmpty())
                    ? request.systemPrompt() + "\n\n" + request.prompt()
                    : request.prompt();
            GenerateContentResponse response = model.generateContent(fullPrompt);
            String content = ResponseHandler.getText(response);
            return LLMResponse.success(content, 0, 0.0, System.currentTimeMillis() - startTime, getProviderName(),
                    getModelName());
        } catch (Exception e) {
            return LLMResponse.failure(getProviderName(), getModelName(), e.getMessage());
        }
    }

    private LLMResponse generateWithRest(LLMRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            String url = String.format(
                    "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    modelName, apiKey);

            // Build payload using Jackson (handles all escaping correctly)
            ObjectNode payload = objectMapper.createObjectNode();
            ArrayNode contents = payload.putArray("contents");
            ObjectNode contentObj = contents.addObject();
            ArrayNode parts = contentObj.putArray("parts");
            parts.addObject().put("text", request.prompt());

            if (request.systemPrompt() != null && !request.systemPrompt().isEmpty()) {
                ObjectNode sysInstruction = payload.putObject("systemInstruction");
                ArrayNode sysParts = sysInstruction.putArray("parts");
                sysParts.addObject().put("text", request.systemPrompt());
            }

            String jsonPayload = objectMapper.writeValueAsString(payload);

            java.net.HttpURLConnection con = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            try (java.io.OutputStream os = con.getOutputStream()) {
                os.write(jsonPayload.getBytes("utf-8"));
            }

            if (con.getResponseCode() == 200) {
                try (java.io.InputStream is = con.getInputStream()) {
                    String content = extractTextFromJson(is);
                    return LLMResponse.success(content, 0, 0.0, System.currentTimeMillis() - startTime,
                            getProviderName(), getModelName());
                }
            } else {
                try (java.util.Scanner s = new java.util.Scanner(con.getErrorStream()).useDelimiter("\\A")) {
                    String errorBody = s.hasNext() ? s.next() : "no body";
                    logger.error("Gemini API error {}: {}", con.getResponseCode(), errorBody);
                }
                return LLMResponse.failure(getProviderName(), getModelName(), "HTTP " + con.getResponseCode());
            }
        } catch (Exception e) {
            return LLMResponse.failure(getProviderName(), getModelName(), e.getMessage());
        }
    }

    /**
     * Extracts the text content from a Gemini API JSON response using Jackson.
     * Response structure: {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
     */
    private String extractTextFromJson(java.io.InputStream responseStream) {
        try {
            JsonNode root = objectMapper.readTree(responseStream);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode textParts = firstCandidate.path("content").path("parts");
                if (textParts.isArray() && !textParts.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode part : textParts) {
                        if (part.has("text")) {
                            sb.append(part.get("text").asText());
                        }
                    }
                    return sb.toString();
                }
            }
            logger.warn("Could not extract text from Gemini response: {}", root);
            return "Respuesta recibida sin contenido válido (ver logs)";
        } catch (Exception e) {
            logger.error("Error parsing Gemini response JSON: {}", e.getMessage());
            return "Error parsing response";
        }
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        throw new UnsupportedOperationException("Embeddings con Gemini API Key pendientes.");
    }

    @Override
    public boolean isHealthy() {
        return useRestApi || (model != null);
    }

    @Override
    public String getProviderName() {
        return "google";
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}
