package com.piloto.cdi.kernel.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Deterministic embedding provider for testing.
 * 
 * <p>Generates embeddings by hashing text content into fixed-dimension vectors.
 * Identical text produces identical embeddings (deterministic).</p>
 * 
 * <p><b>Algorithm:</b></p>
 * <ul>
 *   <li>Hash text with Java String.hashCode()</li>
 *   <li>Use hash as seed for pseudo-random number generator</li>
 *   <li>Generate N normalized values</li>
 * </ul>
 * 
 * <p><b>Properties:</b></p>
 * <ul>
 *   <li>Deterministic: same text → same vector</li>
 *   <li>Fast: no network calls or ML inference</li>
 *   <li>Suitable only for testing, NOT production</li>
 * </ul>
 * 
 * @since Phase 4
 */
public class MockEmbeddingProvider implements EmbeddingProvider {
    
    private final int dimensions;
    private final String providerName;
    
    /**
     * Constructs mock embedding provider with specified dimensions.
     * 
     * @param dimensions embedding vector size (e.g., 384, 768, 1536)
     */
    public MockEmbeddingProvider(int dimensions) {
        if (dimensions <= 0) {
            throw new IllegalArgumentException("dimensions must be > 0, got: " + dimensions);
        }
        this.dimensions = dimensions;
        this.providerName = "Mock-" + dimensions;
    }
    
    /**
     * Default constructor with 384 dimensions (common for sentence transformers).
     */
    public MockEmbeddingProvider() {
        this(384);
    }
    
    @Override
    public CompletableFuture<List<Double>> embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalArgumentException("text cannot be null or empty")
            );
        }
        
        return CompletableFuture.supplyAsync(() -> generateDeterministicEmbedding(text));
    }
    
    @Override
    public int getDimensions() {
        return dimensions;
    }
    
    @Override
    public String getProviderName() {
        return providerName;
    }
    
    /**
     * Generates deterministic embedding from text.
     * 
     * <p>Uses text hash as PRNG seed to produce repeatable vectors.</p>
     * 
     * @param text input text
     * @return normalized embedding vector
     */
    private List<Double> generateDeterministicEmbedding(String text) {
        // Use text hash as seed for repeatability
        long seed = text.hashCode();
        
        // Simple linear congruential generator (LCG) for deterministic pseudo-random numbers
        List<Double> vector = new ArrayList<>(dimensions);
        long state = seed;
        
        for (int i = 0; i < dimensions; i++) {
            // LCG parameters (from Numerical Recipes)
            state = (state * 1664525L + 1013904223L) & 0xFFFFFFFFL;
            // Normalize to [-1, 1]
            double value = (state / (double) 0xFFFFFFFFL) * 2.0 - 1.0;
            vector.add(value);
        }
        
        // Normalize vector to unit length (L2 norm = 1)
        return normalizeVector(vector);
    }
    
    /**
     * Normalizes vector to unit length.
     * 
     * @param vector input vector
     * @return normalized vector with L2 norm = 1
     */
    private List<Double> normalizeVector(List<Double> vector) {
        double norm = 0.0;
        for (double value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        
        if (norm < 1e-10) {
            // Avoid division by zero for zero vectors
            return vector;
        }
        
        List<Double> normalized = new ArrayList<>(dimensions);
        for (double value : vector) {
            normalized.add(value / norm);
        }
        
        return normalized;
    }
}
