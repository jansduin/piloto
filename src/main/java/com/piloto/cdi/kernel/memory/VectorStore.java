package com.piloto.cdi.kernel.memory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.piloto.cdi.kernel.types.TenantID;

/**
 * Abstract interface for vector storage and similarity search.
 * 
 * <p>
 * Decouples memory system from specific vector database implementations
 * (Pinecone, Weaviate, ChromaDB, FAISS, etc.).
 * </p>
 * 
 * <p>
 * <b>Design Principles:</b>
 * </p>
 * <ul>
 * <li>Database-agnostic: Works with any vector DB</li>
 * <li>Async-ready: All operations return CompletableFuture</li>
 * <li>Metadata support: Stores arbitrary key-value pairs with vectors</li>
 * <li>Filtering: Supports metadata-based filtering in search</li>
 * </ul>
 * 
 * <p>
 * <b>Example Implementations:</b>
 * </p>
 * <ul>
 * <li>PineconeVectorStore: Cloud-hosted vector DB</li>
 * <li>InMemoryVectorStore: Simple map-based storage for testing</li>
 * <li>FAISSVectorStore: Facebook AI Similarity Search (local)</li>
 * </ul>
 * 
 * @since Phase 4
 */
public interface VectorStore {

    /**
     * Adds embedding vector with metadata to the store.
     * 
     * @param tenantId namespace strictly isolating tenants
     * @param id       unique identifier for this vector
     * @param vector   embedding vector (dimensions must match store config)
     * @param metadata arbitrary key-value metadata (immutable)
     * @return CompletableFuture completing when add operation finishes
     * @throws IllegalArgumentException if id exists, vector dimensions invalid, or
     *                                  metadata null
     */
    CompletableFuture<Void> addEmbedding(TenantID tenantId, UUID id, List<Double> vector, Map<String, String> metadata);

    /**
     * Searches for nearest neighbors by vector similarity.
     * 
     * <p>
     * Returns up to topK results sorted by similarity (descending).
     * Optional filters apply AND logic on metadata.
     * </p>
     * 
     * <p>
     * <b>Filter Examples:</b>
     * </p>
     * 
     * <pre>
     * filters = Map.of("memoryType", "SEMANTIC")  // Only semantic memories
     * filters = Map.of("sourceAgent", "agent-1")   // Only from specific agent
     * filters = null                                // No filtering
     * </pre>
     * 
     * @param tenantId namespace isolating vectors
     * @param vector   query embedding vector
     * @param topK     maximum results to return (must be > 0)
     * @param filters  metadata filters (null = no filtering)
     * @return CompletableFuture resolving to list of UUIDs sorted by similarity
     * @throws IllegalArgumentException if topK <= 0 or vector dimensions invalid
     */
    CompletableFuture<List<UUID>> search(TenantID tenantId, List<Double> vector, int topK, Map<String, String> filters);

    /**
     * Deletes embedding and metadata by ID.
     * 
     * @param tenantId namespace strictly isolating tenants
     * @param id       identifier of vector to delete
     * @return CompletableFuture completing when delete finishes (no-op if ID not
     *         found)
     */
    CompletableFuture<Void> delete(TenantID tenantId, UUID id);

    /**
     * Updates existing embedding and metadata.
     * 
     * <p>
     * Replaces both vector and metadata atomically.
     * </p>
     * 
     * @param tenantId namespace strictly isolating tenants
     * @param id       identifier of vector to update
     * @param vector   new embedding vector
     * @param metadata new metadata
     * @return CompletableFuture completing when update finishes
     * @throws IllegalArgumentException if id not found, vector dimensions invalid,
     *                                  or metadata null
     */
    CompletableFuture<Void> update(TenantID tenantId, UUID id, List<Double> vector, Map<String, String> metadata);

    /**
     * Returns total number of vectors in the store.
     * 
     * @param tenantId strictly isolate tenant
     * @return CompletableFuture resolving to count
     */
    CompletableFuture<Long> count(TenantID tenantId);

    /**
     * Checks if vector exists by ID.
     * 
     * @param tenantId namespace isolating instances
     * @param id       identifier to check
     * @return CompletableFuture resolving to true if exists
     */
    CompletableFuture<Boolean> exists(TenantID tenantId, UUID id);

    /**
     * Retrieves all metadata stored in the vector store.
     * Useful for rebuilding in-memory caches.
     * 
     * @return CompletableFuture resolving to a map of all metadata partitioned by
     *         tenant
     */
    CompletableFuture<Map<TenantID, Map<UUID, Map<String, String>>>> getAllMetadata();
}
