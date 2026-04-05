package com.piloto.cdi.kernel.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.piloto.cdi.kernel.types.TenantID;

/**
 * Persistent implementation of VectorStore using JSON files.
 * Designed for local development and persistence.
 */
public class JsonFileVectorStore implements VectorStore {
    private static final Logger logger = LoggerFactory.getLogger(JsonFileVectorStore.class);
    private static final String DEFAULT_PATH = ".piloto-data/memory/vector_store.json";

    private final Map<TenantID, Map<UUID, VectorEntry>> storage = new ConcurrentHashMap<>();
    private final int dimensions;
    private final ObjectMapper objectMapper;
    private final Path filePath;

    public static record VectorEntry(List<Double> vector, Map<String, String> metadata) {
    }

    public JsonFileVectorStore(int dimensions) {
        this(dimensions, DEFAULT_PATH);
    }

    public JsonFileVectorStore(int dimensions, String pathStr) {
        this.dimensions = dimensions;
        this.filePath = Paths.get(pathStr);
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        initializeStorage();
        loadFromDisk();
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(filePath.getParent());
        } catch (IOException e) {
            logger.error("Failed to create memory directory", e);
        }
    }

    private synchronized void loadFromDisk() {
        File file = filePath.toFile();
        if (!file.exists())
            return;

        try {
            Map<TenantID, Map<UUID, VectorEntry>> data = objectMapper.readValue(file,
                    new TypeReference<Map<TenantID, Map<UUID, VectorEntry>>>() {
                    });
            storage.putAll(data);
            logger.info("Loaded {} vectors from {}", storage.size(), filePath);
        } catch (IOException e) {
            logger.error("Failed to load vector store from disk", e);
        }
    }

    private synchronized void saveToDisk() {
        try {
            objectMapper.writeValue(filePath.toFile(), storage);
        } catch (IOException e) {
            logger.error("Failed to save vector store to disk", e);
        }
    }

    @Override
    public CompletableFuture<Void> addEmbedding(TenantID tenantId, UUID id, List<Double> vector,
            Map<String, String> metadata) {
        if (vector.size() != dimensions) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Dimension mismatch"));
        }
        storage.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>()).put(id, new VectorEntry(vector, metadata));
        saveToDisk();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<UUID>> search(TenantID tenantId, List<Double> queryVector, int topK,
            Map<String, String> filters) {
        return CompletableFuture.supplyAsync(() -> {
            List<ScoredResult> results = new ArrayList<>();
            Map<UUID, VectorEntry> tenantStorage = storage.getOrDefault(tenantId, Collections.emptyMap());
            for (Map.Entry<UUID, VectorEntry> entry : tenantStorage.entrySet()) {
                if (filters != null && !matchesFilters(entry.getValue().metadata, filters))
                    continue;
                double sim = cosineSimilarity(queryVector, entry.getValue().vector);
                results.add(new ScoredResult(entry.getKey(), sim));
            }
            return results.stream()
                    .sorted(Comparator.comparingDouble(ScoredResult::score).reversed())
                    .limit(topK)
                    .map(ScoredResult::id)
                    .collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<Void> delete(TenantID tenantId, UUID id) {
        Map<UUID, VectorEntry> tenantStorage = storage.get(tenantId);
        if (tenantStorage != null) {
            tenantStorage.remove(id);
            saveToDisk();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> update(TenantID tenantId, UUID id, List<Double> vector,
            Map<String, String> metadata) {
        Map<UUID, VectorEntry> tenantStorage = storage.get(tenantId);
        if (tenantStorage == null || !tenantStorage.containsKey(id))
            return CompletableFuture.failedFuture(new NoSuchElementException());
        tenantStorage.put(id, new VectorEntry(vector, metadata));
        saveToDisk();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Long> count(TenantID tenantId) {
        Map<UUID, VectorEntry> tenantStorage = storage.get(tenantId);
        return CompletableFuture.completedFuture(tenantStorage == null ? 0L : (long) tenantStorage.size());
    }

    @Override
    public CompletableFuture<Boolean> exists(TenantID tenantId, UUID id) {
        Map<UUID, VectorEntry> tenantStorage = storage.get(tenantId);
        return CompletableFuture.completedFuture(tenantStorage != null && tenantStorage.containsKey(id));
    }

    @Override
    public CompletableFuture<Map<TenantID, Map<UUID, Map<String, String>>>> getAllMetadata() {
        return CompletableFuture.supplyAsync(() -> {
            Map<TenantID, Map<UUID, Map<String, String>>> result = new HashMap<>();
            storage.forEach((tenantId, tenantMap) -> {
                Map<UUID, Map<String, String>> uuidMap = new HashMap<>();
                tenantMap.forEach((id, entry) -> uuidMap.put(id, entry.metadata()));
                result.put(tenantId, uuidMap);
            });
            return result;
        });
    }

    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        double dot = 0, n1 = 0, n2 = 0;
        for (int i = 0; i < v1.size(); i++) {
            dot += v1.get(i) * v2.get(i);
            n1 += v1.get(i) * v1.get(i);
            n2 += v2.get(i) * v2.get(i);
        }
        return (n1 == 0 || n2 == 0) ? 0 : dot / (Math.sqrt(n1) * Math.sqrt(n2));
    }

    private boolean matchesFilters(Map<String, String> meta, Map<String, String> filters) {
        for (Map.Entry<String, String> f : filters.entrySet()) {
            if (!Objects.equals(meta.get(f.getKey()), f.getValue()))
                return false;
        }
        return true;
    }

    private record ScoredResult(UUID id, double score) {
    }
}
