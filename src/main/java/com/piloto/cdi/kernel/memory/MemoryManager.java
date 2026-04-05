package com.piloto.cdi.kernel.memory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.piloto.cdi.kernel.types.TenantID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central orchestrator for hybrid memory system.
 * 
 * <p>
 * Coordinates embedding generation and vector storage to provide
 * semantic memory retrieval capabilities.
 * </p>
 * 
 * <p>
 * <b>Architecture:</b>
 * </p>
 * 
 * <pre>
 * MemoryManager
 *   ├─→ EmbeddingProvider (text → vector)
 *   ├─→ VectorStore (vector storage + search)
 *   └─→ In-memory metadata cache (UUID → MemoryEntry)
 * </pre>
 * 
 * <p>
 * <b>Thread Safety:</b>
 * </p>
 * <ul>
 * <li>ConcurrentHashMap for metadata cache</li>
 * <li>All operations are async (non-blocking)</li>
 * <li>Safe for concurrent use from multiple threads/agents</li>
 * </ul>
 * 
 * @since Phase 4
 */
public class MemoryManager {

    private static final Logger logger = LoggerFactory.getLogger(MemoryManager.class);

    private final EmbeddingProvider embeddingProvider;
    private final VectorStore vectorStore;
    private final Map<TenantID, Map<UUID, MemoryEntry>> metadataCache;

    /**
     * Constructs MemoryManager with dependency injection.
     * 
     * @param embeddingProvider provider for text embeddings
     * @param vectorStore       vector database backend
     */
    public MemoryManager(EmbeddingProvider embeddingProvider, VectorStore vectorStore) {
        this.embeddingProvider = Objects.requireNonNull(embeddingProvider, "embeddingProvider cannot be null");
        this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore cannot be null");
        this.metadataCache = new ConcurrentHashMap<>();

        // Carga asíncrona de caché desde el backend persistente
        this.vectorStore.getAllMetadata().thenAccept(allMetadata -> {
            allMetadata.forEach((tenantId, tenantMetadata) -> {
                Map<UUID, MemoryEntry> tenantCache = metadataCache.computeIfAbsent(tenantId,
                        k -> new ConcurrentHashMap<>());
                tenantMetadata.forEach((id, metadata) -> {
                    try {
                        String content = metadata.getOrDefault("content", "[No Content]");
                        MemoryType type = MemoryType.valueOf(metadata.getOrDefault("memoryType", "SHORT_TERM"));
                        String sourceAgent = metadata.getOrDefault("sourceAgent", "System");
                        int version = Integer.parseInt(metadata.getOrDefault("version", "1"));
                        java.time.Instant createdAt = java.time.Instant
                                .parse(metadata.getOrDefault("createdAt", java.time.Instant.now().toString()));

                        Map<String, String> pureMetadata = new HashMap<>(metadata);
                        pureMetadata.remove("content");
                        pureMetadata.remove("memoryType");
                        pureMetadata.remove("sourceAgent");
                        pureMetadata.remove("version");
                        pureMetadata.remove("createdAt");

                        MemoryEntry restored = MemoryEntry.restore(id, tenantId, type, content, pureMetadata, createdAt,
                                version,
                                sourceAgent);
                        tenantCache.put(id, restored);
                    } catch (Exception e) {
                        logger.error("Error al restaurar MemoryEntry con ID {} para Tenant {}: {}", id, tenantId,
                                e.getMessage());
                        // Ignore malformed entries during restore
                    }
                });
            });
            logger.info("Restored metadata entries from vector store to partitioned cache");
        });
    }

    /**
     * Adds memory entry to the system.
     * 
     * <p>
     * <b>Workflow:</b>
     * </p>
     * <ol>
     * <li>Generate embedding from entry content</li>
     * <li>Store vector with metadata in vector store</li>
     * <li>Cache MemoryEntry in RAM for fast metadata lookup</li>
     * </ol>
     * 
     * @param entry memory entry to add (non-null)
     * @return CompletableFuture completing when add finishes
     * @throws IllegalArgumentException if entry with same ID already exists
     */
    public CompletableFuture<Void> addMemory(MemoryEntry entry) {
        Objects.requireNonNull(entry, "entry cannot be null");
        TenantID tenantId = entry.getTenantId();

        Map<UUID, MemoryEntry> tenantCache = metadataCache.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>());
        if (tenantCache.containsKey(entry.getId())) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Memory entry already exists: " + entry.getId()));
        }

        return embeddingProvider.embed(entry.getContent())
                .thenCompose(embedding -> {
                    // Prepare metadata for vector store
                    Map<String, String> vectorMetadata = new HashMap<>(entry.getMetadata());
                    vectorMetadata.put("content", entry.getContent());
                    vectorMetadata.put("memoryType", entry.getType().name());
                    vectorMetadata.put("sourceAgent", entry.getSourceAgent());
                    vectorMetadata.put("version", String.valueOf(entry.getVersion()));
                    vectorMetadata.put("createdAt", entry.getCreatedAt().toString());

                    return vectorStore.addEmbedding(tenantId, entry.getId(), embedding, vectorMetadata);
                })
                .thenAccept(unused -> {
                    // Cache entry for metadata lookups
                    tenantCache.put(entry.getId(), entry);
                });
    }

    /**
     * Queries memory system with semantic search.
     * 
     * <p>
     * <b>Workflow:</b>
     * </p>
     * <ol>
     * <li>Generate embedding from query text</li>
     * <li>Search vector store for top-K nearest neighbors</li>
     * <li>Apply memory type filters if specified</li>
     * <li>Retrieve full MemoryEntry from cache</li>
     * <li>Construct aggregated context string</li>
     * </ol>
     * 
     * @param query query specification (non-null)
     * @return CompletableFuture resolving to MemoryResult
     */
    public CompletableFuture<MemoryResult> queryMemory(MemoryQuery query) {
        Objects.requireNonNull(query, "query cannot be null");
        TenantID tenantId = query.getTenantId();

        logger.info("Initializing queryMemory for text: {}", query.getQuery());
        return embeddingProvider.embed(query.getQuery())
                .thenCompose(queryEmbedding -> {
                    // Prepare metadata filters
                    Map<String, String> filters = null;
                    if (query.getMemoryTypes().isPresent() && !query.getMemoryTypes().get().isEmpty()) {
                        // Vector stores typically support single-value filters, so we search without
                        // type filter
                        // and filter in-memory afterward for simplicity in Phase 4
                        filters = null;
                    }

                    return vectorStore.search(tenantId, queryEmbedding, query.getTopK(), filters)
                            .thenApply(resultIds -> {
                                logger.info("Semantic Search returned {} raw vector IDs: {}", resultIds.size(),
                                        resultIds);
                                // Retrieve entries from cache
                                Map<UUID, MemoryEntry> tenantCache = metadataCache.getOrDefault(tenantId,
                                        Collections.emptyMap());
                                List<MemoryEntry> entries = resultIds.stream()
                                        .map(tenantCache::get)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());

                                logger.info("Cache reconstruction yielded {} entries out of {} IDs", entries.size(),
                                        resultIds.size());
                                if (entries.isEmpty() && !resultIds.isEmpty()) {
                                    logger.warn("Cache MISS for all returned vector IDs. Is cache fully populated?");
                                }

                                // Apply memory type filter if specified
                                if (query.getMemoryTypes().isPresent() && !query.getMemoryTypes().get().isEmpty()) {
                                    Set<MemoryType> allowedTypes = new HashSet<>(query.getMemoryTypes().get());
                                    entries = entries.stream()
                                            .filter(entry -> allowedTypes.contains(entry.getType()))
                                            .collect(Collectors.toList());
                                    logger.info("After type filtering, {} entries remain", entries.size());
                                }

                                // Construct aggregated context
                                String aggregatedContext = entries.stream()
                                        .map(MemoryEntry::getContent)
                                        .collect(Collectors.joining("\n\n---\n\n"));

                                logger.debug("Final long-term RAG context created with length: {}",
                                        aggregatedContext.length());

                                return MemoryResult.create(entries, aggregatedContext);
                            });
                });
    }

    /**
     * Retrieves recent memory entries specifically matching a session ID.
     * Performs exact metadata filtering without semantic embeddings.
     * 
     * @param sessionId the session identifier
     * @param limit     maximum number of messages to retrieve
     * @return CompletableFuture resolving to MemoryResult
     */
    public CompletableFuture<MemoryResult> getRecentSessionMemory(TenantID tenantId, String sessionId, int limit) {
        Map<UUID, MemoryEntry> tenantCache = metadataCache.getOrDefault(tenantId, Collections.emptyMap());
        List<MemoryEntry> entries = tenantCache.values().stream()
                .filter(entry -> entry.getType() == MemoryType.SHORT_TERM)
                .filter(entry -> sessionId.equals(entry.getMetadata().get("sessionId")))
                .sorted(Comparator.comparing(MemoryEntry::getCreatedAt))
                .collect(Collectors.toList());

        if (entries.size() > limit) {
            entries = entries.subList(entries.size() - limit, entries.size());
        }

        String aggregatedContext = entries.stream()
                .map(MemoryEntry::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));

        return CompletableFuture.completedFuture(MemoryResult.create(entries, aggregatedContext));
    }

    /**
     * Updates existing memory entry.
     * 
     * <p>
     * Creates new version with incremented version number and re-indexes.
     * </p>
     * 
     * @param updatedEntry updated memory entry (must exist)
     * @return CompletableFuture completing when update finishes
     * @throws IllegalArgumentException if entry does not exist
     */
    public CompletableFuture<Void> updateMemory(MemoryEntry updatedEntry) {
        Objects.requireNonNull(updatedEntry, "updatedEntry cannot be null");
        TenantID tenantId = updatedEntry.getTenantId();
        Map<UUID, MemoryEntry> tenantCache = metadataCache.get(tenantId);

        if (tenantCache == null || !tenantCache.containsKey(updatedEntry.getId())) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Memory entry not found: " + updatedEntry.getId()));
        }

        return embeddingProvider.embed(updatedEntry.getContent())
                .thenCompose(embedding -> {
                    Map<String, String> vectorMetadata = new HashMap<>(updatedEntry.getMetadata());
                    vectorMetadata.put("content", updatedEntry.getContent());
                    vectorMetadata.put("memoryType", updatedEntry.getType().name());
                    vectorMetadata.put("sourceAgent", updatedEntry.getSourceAgent());
                    vectorMetadata.put("version", String.valueOf(updatedEntry.getVersion()));
                    vectorMetadata.put("createdAt", updatedEntry.getCreatedAt().toString());

                    return vectorStore.update(tenantId, updatedEntry.getId(), embedding, vectorMetadata);
                })
                .thenAccept(unused -> {
                    tenantCache.put(updatedEntry.getId(), updatedEntry);
                });
    }

    /**
     * Deletes memory entry by ID.
     * 
     * @param id identifier of entry to delete
     * @return CompletableFuture completing when delete finishes
     */
    public CompletableFuture<Void> deleteMemory(TenantID tenantId, UUID id) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(id, "id cannot be null");

        return vectorStore.delete(tenantId, id)
                .thenAccept(unused -> {
                    Map<UUID, MemoryEntry> tenantCache = metadataCache.get(tenantId);
                    if (tenantCache != null)
                        tenantCache.remove(id);
                });
    }

    /**
     * Returns total number of memory entries.
     * 
     * @return CompletableFuture resolving to count
     */
    public CompletableFuture<Long> getMemoryCount(TenantID tenantId) {
        return vectorStore.count(tenantId);
    }

    /**
     * Checks if memory entry exists.
     * 
     * @param id identifier to check
     * @return CompletableFuture resolving to true if exists
     */
    public CompletableFuture<Boolean> memoryExists(TenantID tenantId, UUID id) {
        Map<UUID, MemoryEntry> tenantCache = metadataCache.get(tenantId);
        return CompletableFuture.completedFuture(tenantCache != null && tenantCache.containsKey(id));
    }

    /**
     * Retrieves memory entry by ID (from cache).
     * 
     * @param id identifier
     * @return Optional containing entry if found
     */
    public Optional<MemoryEntry> getMemory(TenantID tenantId, UUID id) {
        Map<UUID, MemoryEntry> tenantCache = metadataCache.get(tenantId);
        return tenantCache != null ? Optional.ofNullable(tenantCache.get(id)) : Optional.empty();
    }
}
