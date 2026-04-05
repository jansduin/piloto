package com.piloto.cdi.gateway.governance.evolution.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.piloto.cdi.gateway.governance.evolution.model.BehavioralCell;
import com.piloto.cdi.gateway.governance.evolution.model.EvolutionSnapshot;
import com.piloto.cdi.gateway.governance.evolution.model.EvolutionSnapshot.EvolutionVariantDTO;
import com.piloto.cdi.gateway.governance.evolution.model.EvolutionVariant;
import com.piloto.cdi.kernel.governance.type.PromptLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON file persistence for evolution state.
 *
 * Follows the exact pattern of:
 *   JsonFileEventStore  — tenant-segmented, ObjectMapper with JavaTimeModule
 *   PromptRegistry      — loadFromDisk/saveToDisk with ConcurrentHashMap
 *
 * Stores per-tenant:
 *   - All EvolutionVariants (champions + challengers)
 *   - Processed sessionIds (idempotency set)
 *   - Meta-prompt evolution state (PromptBreeder)
 *
 * Thread-safety: per-tenant lock striping (same as JsonFileEventStore).
 */
@Repository
public class EvolutionStore {

    private static final Logger logger = LoggerFactory.getLogger(EvolutionStore.class);
    private static final String STORAGE_PATH = ".piloto-data/evolution";

    private final ObjectMapper objectMapper;
    private final Path storagePath;
    private final Map<String, Object> tenantLocks = new ConcurrentHashMap<>();

    // In-memory snapshots (loaded from disk on startup)
    private final Map<String, EvolutionSnapshot> snapshots = new ConcurrentHashMap<>();

    public EvolutionStore() {
        this(STORAGE_PATH);
    }

    /**
     * Constructor with custom storage path (used for testing with @TempDir).
     */
    public EvolutionStore(String storagePath) {
        this.storagePath = Paths.get(storagePath);
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .enable(SerializationFeature.INDENT_OUTPUT);
        initializeStorage();
        loadAllFromDisk();
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(storagePath);
            logger.info("EvolutionStore initialized at: {}", storagePath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create evolution storage directory", e);
            throw new RuntimeException("Evolution storage initialization failed", e);
        }
    }

    public EvolutionSnapshot getOrCreate(String tenantId) {
        return snapshots.computeIfAbsent(tenantId, k -> EvolutionSnapshot.empty(tenantId));
    }

    public void save(String tenantId, EvolutionSnapshot snapshot) {
        synchronized (getTenantLock(tenantId)) {
            snapshots.put(tenantId, snapshot);
            saveToDisk(tenantId, snapshot);
        }
    }

    public boolean isSessionProcessed(String tenantId, String sessionId) {
        EvolutionSnapshot snap = snapshots.get(tenantId);
        return snap != null && snap.processedSessionIds().contains(sessionId);
    }

    /**
     * Serialize grid state to a full snapshot for persistence.
     */
    public EvolutionSnapshot buildSnapshot(String tenantId,
                                            List<EvolutionVariant> allVariants,
                                            Map<String, UUID> championMap,
                                            Set<String> processedSessions,
                                            String metaPrompt,
                                            int metaPromptGeneration) {

        List<EvolutionVariantDTO> dtos = allVariants.stream()
                .filter(v -> tenantId.equals(v.getTenantId()) || v.getTenantId() == null)
                .map(this::toDTO)
                .toList();

        Map<String, String> champKeys = new HashMap<>();
        championMap.forEach((key, id) -> champKeys.put(key, id.toString()));

        return new EvolutionSnapshot(tenantId, new ArrayList<>(dtos), champKeys,
                new HashSet<>(processedSessions), metaPrompt, metaPromptGeneration);
    }

    /**
     * Reconstruct EvolutionVariant objects from a persisted snapshot.
     */
    public List<EvolutionVariant> restoreVariants(EvolutionSnapshot snapshot) {
        if (snapshot.variants() == null) return new ArrayList<>();

        List<EvolutionVariant> result = new ArrayList<>();
        for (EvolutionVariantDTO dto : snapshot.variants()) {
            try {
                EvolutionVariant variant = EvolutionVariant.builder()
                        .id(UUID.fromString(dto.id()))
                        .promptEntityId(dto.promptEntityId() != null
                                ? UUID.fromString(dto.promptEntityId()) : null)
                        .cell(new BehavioralCell(dto.cellDimensions()))
                        .layer(dto.layer() != null ? PromptLayer.valueOf(dto.layer()) : null)
                        .tenantId(dto.tenantId())
                        .promptContentSnapshot(dto.promptContentSnapshot())
                        .totalSeen(dto.totalSeen())
                        .totalConverted(dto.totalConverted())
                        .createdAt(dto.createdAt() != null ? Instant.parse(dto.createdAt()) : null)
                        .lastOutcomeAt(dto.lastOutcomeAt() != null ? Instant.parse(dto.lastOutcomeAt()) : null)
                        .build();
                result.add(variant);
            } catch (Exception e) {
                logger.warn("Failed to restore variant {}: {}", dto.id(), e.getMessage());
            }
        }
        return result;
    }

    private EvolutionVariantDTO toDTO(EvolutionVariant v) {
        return new EvolutionVariantDTO(
                v.getId().toString(),
                v.getPromptEntityId() != null ? v.getPromptEntityId().toString() : null,
                v.getCell().dimensions(),
                v.getCell().key(),
                v.getLayer() != null ? v.getLayer().name() : null,
                v.getTenantId(),
                v.getPromptContentSnapshot(),
                v.getTotalSeen(),
                v.getTotalConverted(),
                v.getCreatedAt() != null ? v.getCreatedAt().toString() : null,
                v.getLastOutcomeAt() != null ? v.getLastOutcomeAt().toString() : null);
    }

    private void saveToDisk(String tenantId, EvolutionSnapshot snapshot) {
        Path filePath = storagePath.resolve(tenantId + ".json");
        try {
            Files.createDirectories(filePath.getParent());
            objectMapper.writeValue(filePath.toFile(), snapshot);
            logger.debug("Evolution snapshot saved for tenant {}", tenantId);
        } catch (IOException e) {
            logger.error("CRITICAL: Failed to save evolution snapshot for tenant {}", tenantId, e);
        }
    }

    private void loadAllFromDisk() {
        File dir = storagePath.toFile();
        if (!dir.exists()) return;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;
        for (File file : files) {
            try {
                EvolutionSnapshot snapshot = objectMapper.readValue(file, EvolutionSnapshot.class);
                String tenantId = file.getName().replace(".json", "");
                snapshots.put(tenantId, snapshot);
                logger.info("Loaded evolution snapshot for tenant {} ({} variants)",
                        tenantId, snapshot.variants() != null ? snapshot.variants().size() : 0);
            } catch (IOException e) {
                logger.error("Failed to load evolution snapshot from {}", file.getName(), e);
            }
        }
    }

    private Object getTenantLock(String tenantId) {
        return tenantLocks.computeIfAbsent(tenantId, k -> new Object());
    }
}
