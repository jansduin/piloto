package com.piloto.cdi.kernel.memory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract interface for text embedding generation.
 * 
 * <p>Decouples memory system from specific embedding providers (OpenAI, local models, etc.).
 * Implementations can use any embedding backend.</p>
 * 
 * <p><b>Design Principles:</b></p>
 * <ul>
 *   <li>Provider-agnostic: Works with any embedding API</li>
 *   <li>Async-ready: Returns CompletableFuture for non-blocking operations</li>
 *   <li>Stateless: No internal state, safe for concurrent use</li>
 * </ul>
 * 
 * <p><b>Example Implementations:</b></p>
 * <ul>
 *   <li>OpenAIEmbeddingProvider: Uses OpenAI text-embedding-ada-002</li>
 *   <li>LocalEmbeddingProvider: Uses sentence-transformers locally</li>
 *   <li>MockEmbeddingProvider: Deterministic embeddings for testing</li>
 * </ul>
 * 
 * @since Phase 4
 */
public interface EmbeddingProvider {
    
    /**
     * Generates embedding vector for input text.
     * 
     * <p>Vector dimensions depend on provider:</p>
 * <ul>
     *   <li>OpenAI ada-002: 1536 dimensions</li>
     *   <li>sentence-transformers: typically 384 or 768</li>
     * </ul>
     * 
     * @param text input text to embed (non-null, non-empty)
     * @return CompletableFuture resolving to embedding vector
     * @throws IllegalArgumentException if text is null or empty
     * @throws RuntimeException if embedding generation fails
     */
    CompletableFuture<List<Double>> embed(String text);
    
    /**
     * Returns dimensionality of embeddings produced by this provider.
     * 
     * <p>Used for validation and vector store configuration.</p>
     * 
     * @return embedding dimension count (e.g., 1536 for OpenAI ada-002)
     */
    int getDimensions();
    
    /**
     * Returns provider identifier for logging and debugging.
     * 
     * @return provider name (e.g., "OpenAI-ada-002", "local-minilm")
     */
    String getProviderName();
}
