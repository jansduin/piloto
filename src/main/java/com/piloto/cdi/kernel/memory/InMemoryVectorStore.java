package com.piloto.cdi.kernel.memory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.piloto.cdi.kernel.types.TenantID;

/**
 * In-memory vector store implementation for testing and prototyping.
 * 
 * <p>
 * Uses cosine similarity for vector search with naive O(N) scan.
 * Suitable for small datasets (<10K vectors). For production, use
 * specialized vector DB (Pinecone, FAISS, etc.).
 * </p>
 * 
 * <p>
 * <b>Thread Safety:</b>
 * </p>
 * <ul>
 * <li>ConcurrentHashMap for storage</li>
 * <li>Safe for concurrent operations</li>
 * </ul>
 * 
 * @since Phase 4
 */
public class InMemoryVectorStore implements VectorStore {

    private final Map<TenantID, Map<UUID, VectorEntry>> storage;
    private final int dimensions;

    /**
     * Entry storing vector and metadata.
     */
    private static class VectorEntry {
        final List<Double> vector;
        final Map<String, String> metadata;

        VectorEntry(List<Double> vector, Map<String, String> metadata) {
            this.vector = List.copyOf(vector);
            this.metadata = Map.copyOf(metadata);
        }
    }

    /**
     * Constructs in-memory vector store with fixed dimensions.
     * 
     * @param dimensions embedding vector dimensions (e.g., 384, 768, 1536)
     */
    public InMemoryVectorStore(int dimensions) {
        if (dimensions <= 0) {
            throw new IllegalArgumentException("dimensions must be > 0, got: " + dimensions);
        }
        this.dimensions = dimensions;
        this.storage = new ConcurrentHashMap<>();
    }

    @Override
    public CompletableFuture<Void> addEmbedding(TenantID tenantId, UUID id, List<Double> vector,
            Map<String, String> metadata) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(vector, "vector cannot be null");
        Objects.requireNonNull(metadata, "metadata cannot be null");

        if (vector.size() != dimensions) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException(
                            String.format("Vector dimensions mismatch: expected %d, got %d", dimensions,
                                    vector.size())));
        }

        Map<UUID, VectorEntry> tenantStorage = storage.computeIfAbsent(tenantId,
                k -> new java.util.concurrent.ConcurrentHashMap<>());
        if (tenantStorage.containsKey(id)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Vector with ID already exists: " + id));
        }

        tenantStorage.put(id, new VectorEntry(vector, metadata));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<UUID>> search(TenantID tenantId, List<Double> queryVector, int topK,
            Map<String, String> filters) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(queryVector, "queryVector cannot be null");

        if (topK <= 0) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("topK must be > 0, got: " + topK));
        }

        if (queryVector.size() != dimensions) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException(
                            String.format("Query vector dimensions mismatch: expected %d, got %d", dimensions,
                                    queryVector.size())));
        }

        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, VectorEntry> tenantStorage = storage.getOrDefault(tenantId, Collections.emptyMap());
            // Compute similarities for all vectors
            List<ScoredResult> results = new ArrayList<>();

            for (Map.Entry<UUID, VectorEntry> entry : tenantStorage.entrySet()) {
                // Apply metadata filters
                if (filters != null && !matchesFilters(entry.getValue().metadata, filters)) {
                    continue;
                }

                double similarity = cosineSimilarity(queryVector, entry.getValue().vector);
                results.add(new ScoredResult(entry.getKey(), similarity));
            }

            // Sort by similarity descending and take top-K
            return results.stream()
                    .sorted(Comparator.comparingDouble(ScoredResult::score).reversed())
                    .limit(topK)
                    .map(ScoredResult::id)
                    .collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<Void> delete(TenantID tenantId, UUID id) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(id, "id cannot be null");
        Map<UUID, VectorEntry> tenantStorage = storage.get(tenantId);
        if (tenantStorage != null) {
            tenantStorage.remove(id);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> update(TenantID tenantId, UUID id, List<Double> vector,
            Map<String, String> metadata) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(vector, "vector cannot be null");
        Objects.requireNonNull(metadata, "metadata cannot be null");

        Map<UUID, VectorEntry> tenantStorage = storage.get(tenantId);
        if (tenantStorage == null || !tenantStorage.containsKey(id)) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Vector not found: " + id));
        }

        if (vector.size() != dimensions) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException(
                            String.format("Vector dimensions mismatch: expected %d, got %d", dimensions,
                                    vector.size())));
        }

        tenantStorage.put(id, new VectorEntry(vector, metadata));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Long> count(TenantID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Map<UUID, VectorEntry> tenantStorage = storage.get(tenantId);
        return CompletableFuture.completedFuture(tenantStorage == null ? 0L : (long) tenantStorage.size());
    }

    @Override
    public CompletableFuture<Boolean> exists(TenantID tenantId, UUID id) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(id, "id cannot be null");
        Map<UUID, VectorEntry> tenantStorage = storage.get(tenantId);
        return CompletableFuture.completedFuture(tenantStorage != null && tenantStorage.containsKey(id));
    }

    @Override
    public CompletableFuture<Map<TenantID, Map<UUID, Map<String, String>>>> getAllMetadata() {
        return CompletableFuture.supplyAsync(() -> {
            Map<TenantID, Map<UUID, Map<String, String>>> result = new HashMap<>();
            storage.forEach((tenantId, tenantMap) -> {
                Map<UUID, Map<String, String>> uuidMap = new HashMap<>();
                tenantMap.forEach((id, entry) -> uuidMap.put(id, entry.metadata));
                result.put(tenantId, uuidMap);
            });
            return result;
        });
    }

    /**
     * Computes cosine similarity between two vectors.
     * 
     * @param v1 first vector
     * @param v2 second vector
     * @return similarity score (0.0 = orthogonal, 1.0 = identical, -1.0 = opposite)
     */
    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            norm1 += v1.get(i) * v1.get(i);
            norm2 += v2.get(i) * v2.get(i);
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Checks if metadata matches all filter criteria.
     * 
     * @param metadata entry metadata
     * @param filters  required key-value pairs
     * @return true if all filters match
     */
    private boolean matchesFilters(Map<String, String> metadata, Map<String, String> filters) {
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            String value = metadata.get(filter.getKey());
            if (value == null || !value.equals(filter.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Result with score for sorting.
     */
    private record ScoredResult(UUID id, double score) {
    }
}
