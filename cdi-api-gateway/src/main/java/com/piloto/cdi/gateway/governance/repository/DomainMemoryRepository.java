package com.piloto.cdi.gateway.governance.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.piloto.cdi.kernel.governance.model.DomainMemoryItem;
import com.piloto.cdi.kernel.governance.type.MemoryCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for Domain Memory Items.
 * Handles persistence and retrieval of business rules and knowledge.
 */
@Repository
public class DomainMemoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(DomainMemoryRepository.class);
    private static final String STORAGE_FILE = ".piloto-data/governance/domain_memory.json";

    private final Map<UUID, DomainMemoryItem> storage = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    public DomainMemoryRepository() {
        loadFromDisk();
    }

    private synchronized void loadFromDisk() {
        File file = new File(STORAGE_FILE);
        if (!file.exists())
            return;
        try {
            Map<UUID, DomainMemoryItem> data = objectMapper.readValue(file,
                    new TypeReference<Map<UUID, DomainMemoryItem>>() {
                    });
            storage.putAll(data);
            logger.info("Loaded {} domain memory items from persistence", storage.size());
        } catch (IOException e) {
            logger.error("Failed to load domain memory from disk", e);
        }
    }

    private synchronized void saveToDisk() {
        try {
            File file = new File(STORAGE_FILE);
            file.getParentFile().mkdirs();
            objectMapper.writeValue(file, storage);
        } catch (IOException e) {
            logger.error("Failed to save domain memory to disk", e);
        }
    }

    public void save(DomainMemoryItem item) {
        if (storage.containsKey(item.getId())) {
            throw new IllegalStateException(
                    "CQRS Immutability Violation: Updates must be dispatched as Commands, not direct Repository mutations.");
        }
        storage.put(item.getId(), item);
        saveToDisk();
    }

    public Optional<DomainMemoryItem> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    public List<DomainMemoryItem> findByTenant(String tenantId) {
        return storage.values().stream()
                .filter(item -> Objects.equals(item.getTenantId(), tenantId))
                .filter(DomainMemoryItem::isActive)
                .collect(Collectors.toList());
    }

    public List<DomainMemoryItem> findByTenantAndCategory(String tenantId, MemoryCategory category) {
        return storage.values().stream()
                .filter(item -> Objects.equals(item.getTenantId(), tenantId))
                .filter(item -> item.getCategory() == category)
                .filter(DomainMemoryItem::isActive)
                .sorted(Comparator.comparingInt(DomainMemoryItem::getPriority).reversed()) // High priority first
                .collect(Collectors.toList());
    }

    public List<DomainMemoryItem> findAll() {
        return new ArrayList<>(storage.values());
    }

    /**
     * Updates an existing DomainMemoryItem by ID.
     * This is the CQRS-compliant path for mutations — separate from save() which
     * enforces append-only immutability for new items.
     * Rebuilds the item with incremented version and current updatedAt.
     *
     * @param id         UUID of the item to update
     * @param newContent new content for the memory item
     * @return the updated DomainMemoryItem
     * @throws IllegalArgumentException if the item ID does not exist
     */
    public DomainMemoryItem update(UUID id, String newContent) {
        DomainMemoryItem existing = Optional.ofNullable(storage.get(id))
                .orElseThrow(() -> new IllegalArgumentException(
                        "DomainMemoryItem not found for ID: " + id));

        DomainMemoryItem updated = DomainMemoryItem.builder()
                .id(existing.getId())
                .tenantId(existing.getTenantId())
                .category(existing.getCategory())
                .title(existing.getTitle())
                .content(newContent)
                .priority(existing.getPriority())
                .version(existing.getVersion() + 1)
                .isActive(existing.isActive())
                .createdAt(existing.getCreatedAt())
                .updatedAt(java.time.Instant.now())
                .createdBy(existing.getCreatedBy())
                .build();

        // Direct put bypasses the immutability guard in save() — this is intentional
        // for the CQRS update command path.
        storage.put(updated.getId(), updated);
        saveToDisk();
        logger.info("DomainMemoryItem updated: id={}, version={}", id, updated.getVersion());
        return updated;
    }
}
