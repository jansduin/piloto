package com.piloto.cdi.gateway.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.piloto.cdi.kernel.memory.EmbeddingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Production EmbeddingProvider backed by the Google Gemini Embedding API.
 *
 * <p>
 * Uses {@code text-embedding-004} (768 dimensions) via the REST API,
 * reusing the same API key as the GoogleGeminiProvider for LLM generation.
 * </p>
 *
 * <p>
 * <b>API Endpoint:</b><br>
 * {@code POST https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=<APIKEY>}
 * </p>
 *
 * <p>
 * <b>Request body:</b>
 * </p>
 * 
 * <pre>
 * {
 *   "model": "models/text-embedding-004",
 *   "content": { "parts": [{ "text": "..." }] }
 * }
 * </pre>
 *
 * <p>
 * <b>Response structure:</b>
 * </p>
 * 
 * <pre>
 * { "embedding": { "values": [0.123, -0.456, ...] } }
 * </pre>
 *
 * <p>
 * Falls back to {@code MockEmbeddingProvider} when API key is not configured,
 * allowing graceful startup in environments without a Gemini key.
 * </p>
 *
 * @since Phase 4 (Production upgrade)
 */
@Component
public class GeminiEmbeddingProvider implements EmbeddingProvider {

    private static final Logger logger = LoggerFactory.getLogger(GeminiEmbeddingProvider.class);

    /** Gemini gemini-embedding-001 produces 768-dimensional vectors. */
    private static final int DIMENSIONS = 768;
    private static final String MODEL_NAME = "gemini-embedding-001";
    // Google AI Studio REST API (API Key auth) uses /v1beta/ for embeddings
    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final boolean configured;

    public GeminiEmbeddingProvider(
            @Value("${piloto.llm.google.apiKey:}") String apiKey) {
        this.apiKey = apiKey;
        this.configured = apiKey != null && !apiKey.isBlank();
        if (!configured) {
            logger.warn("⚠️ GeminiEmbeddingProvider: GOOGLE_API_KEY not set. " +
                    "Embeddings will fall back to deterministic mock vectors. " +
                    "Set GOOGLE_API_KEY environment variable for real semantic search.");
        } else {
            logger.info("✅ GeminiEmbeddingProvider initialized with model={}, dimensions={}",
                    MODEL_NAME, DIMENSIONS);
        }
    }

    @Override
    public CompletableFuture<List<Double>> embed(String text) {
        if (text == null || text.isBlank()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("text cannot be null or empty"));
        }

        if (!configured) {
            // Graceful fallback: deterministic mock embedding
            return CompletableFuture.supplyAsync(() -> generateMockEmbedding(text));
        }

        return CompletableFuture.supplyAsync(() -> callGeminiEmbeddingApi(text));
    }

    @Override
    public int getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public String getProviderName() {
        return configured ? "gemini-" + MODEL_NAME : "gemini-mock-fallback";
    }

    // ─── Private Methods ──────────────────────────────────────────────────────

    private List<Double> callGeminiEmbeddingApi(String text) {
        String url = API_BASE + MODEL_NAME + ":embedContent?key=" + apiKey;

        try {
            // Build JSON payload
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", "models/" + MODEL_NAME);
            ObjectNode content = payload.putObject("content");
            content.putArray("parts").addObject().put("text", text);
            payload.put("outputDimensionality", DIMENSIONS);

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpURLConnection con = (HttpURLConnection) new java.net.URI(url).toURL().openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setConnectTimeout(10_000);
            con.setReadTimeout(15_000);
            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                os.write(jsonPayload.getBytes("utf-8"));
            }

            int status = con.getResponseCode();
            if (status == 200) {
                try (InputStream is = con.getInputStream()) {
                    return parseEmbeddingResponse(is);
                }
            } else {
                String errorBody;
                try (java.util.Scanner s = new java.util.Scanner(con.getErrorStream()).useDelimiter("\\A")) {
                    errorBody = s.hasNext() ? s.next() : "no body";
                }
                logger.error("Gemini Embedding API error HTTP {}: {}", status, errorBody);
                // Fall back to mock on API error to keep the system functional
                logger.warn("Falling back to mock embedding for this request.");
                return generateMockEmbedding(text);
            }

        } catch (Exception e) {
            logger.error("Error calling Gemini Embedding API: {}", e.getMessage());
            logger.warn("Falling back to mock embedding for this request.");
            return generateMockEmbedding(text);
        }
    }

    private List<Double> parseEmbeddingResponse(InputStream responseStream) throws Exception {
        JsonNode root = objectMapper.readTree(responseStream);
        JsonNode values = root.path("embedding").path("values");

        if (!values.isArray() || values.isEmpty()) {
            throw new RuntimeException("Gemini embedding response did not contain values array: " + root);
        }

        List<Double> embedding = new ArrayList<>(DIMENSIONS);
        for (JsonNode v : values) {
            embedding.add(v.asDouble());
        }

        logger.debug("Gemini embedding generated: {} dimensions", embedding.size());
        return embedding;
    }

    /**
     * Deterministic fallback embedding using LCG hash.
     * Produces 768-dimension vectors to match the real Gemini output dimensions.
     * Used when API key is absent or on API errors.
     */
    private List<Double> generateMockEmbedding(String text) {
        long seed = text.hashCode();
        List<Double> vector = new ArrayList<>(DIMENSIONS);
        long state = seed;
        for (int i = 0; i < DIMENSIONS; i++) {
            state = (state * 1664525L + 1013904223L) & 0xFFFFFFFFL;
            vector.add((state / (double) 0xFFFFFFFFL) * 2.0 - 1.0);
        }
        // L2 normalize
        double norm = Math.sqrt(vector.stream().mapToDouble(v -> v * v).sum());
        if (norm > 1e-10) {
            for (int i = 0; i < vector.size(); i++) {
                vector.set(i, vector.get(i) / norm);
            }
        }
        return vector;
    }
}
