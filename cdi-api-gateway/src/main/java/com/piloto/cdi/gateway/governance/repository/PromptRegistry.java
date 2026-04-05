package com.piloto.cdi.gateway.governance.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.piloto.cdi.kernel.governance.model.PromptEntity;
import com.piloto.cdi.kernel.governance.type.PromptLayer;
import com.piloto.cdi.kernel.governance.type.PromptRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory registry for PromptEntities.
 * Acts as the single source of truth for prompt versions.
 */
@Repository
public class PromptRegistry {

    private static final Logger logger = LoggerFactory.getLogger(PromptRegistry.class);
    private static final String STORAGE_FILE = ".piloto-data/governance/prompts.json";

    private final Map<UUID, PromptEntity> storage = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    public PromptRegistry() {
        loadFromDisk();
    }

    private synchronized void loadFromDisk() {
        File file = new File(STORAGE_FILE);
        if (!file.exists())
            return;
        try {
            Map<UUID, PromptEntity> data = objectMapper.readValue(file, new TypeReference<Map<UUID, PromptEntity>>() {
            });
            storage.putAll(data);
            logger.info("Loaded {} prompts from persistence", storage.size());
        } catch (IOException e) {
            logger.error("Failed to load prompts from disk", e);
        }
    }

    private synchronized void saveToDisk() {
        try {
            File file = new File(STORAGE_FILE);
            file.getParentFile().mkdirs();
            objectMapper.writeValue(file, storage);
        } catch (IOException e) {
            logger.error("Failed to save prompts to disk", e);
        }
    }

    public void save(PromptEntity prompt) {
        storage.put(prompt.getId(), prompt);
        saveToDisk();
    }

    public Optional<PromptEntity> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Optional<PromptEntity> findActive(PromptLayer layer, PromptRole role, String domain, String tenantId) {
        return storage.values().stream()
                .filter(p -> p.getLayer() == layer)
                .filter(p -> Objects.equals(p.getRole(), role))
                .filter(p -> Objects.equals(p.getDomain(), domain))
                .filter(p -> Objects.equals(p.getTenantId(), tenantId))
                .filter(PromptEntity::isActive)
                .max(Comparator.comparingInt(PromptEntity::getVersion));
    }

    public List<PromptEntity> findAll() {
        return new ArrayList<>(storage.values());
    }

    /**
     * Updates an existing PromptEntity by ID.
     * Creates a new immutable instance with incremented version and current
     * timestamp.
     * This is the CQRS-compliant mutation path for governance updates.
     *
     * @param id         UUID of the prompt to update
     * @param newContent new prompt content
     * @return the updated PromptEntity
     * @throws IllegalArgumentException if the prompt ID does not exist
     */
    public PromptEntity update(UUID id, String newContent) {
        PromptEntity existing = Optional.ofNullable(storage.get(id))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prompt not found for ID: " + id));

        PromptEntity updated = PromptEntity.builder()
                .id(existing.getId())
                .name(existing.getName())
                .layer(existing.getLayer())
                .role(existing.getRole())
                .domain(existing.getDomain())
                .tenantId(existing.getTenantId())
                .content(newContent)
                .version(existing.getVersion() + 1)
                .isActive(existing.isActive())
                .createdAt(existing.getCreatedAt())
                .updatedAt(java.time.Instant.now())
                .checksum(existing.getChecksum())
                .build();

        storage.put(updated.getId(), updated);
        saveToDisk();
        logger.info("Prompt updated: id={}, version={}", id, updated.getVersion());
        return updated;
    }
}
