package com.piloto.cdi.kernel.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.piloto.cdi.kernel.interfaces.IEventStore;
import com.piloto.cdi.kernel.model.DomainEvent;
import com.piloto.cdi.kernel.types.TenantID;
import com.piloto.cdi.kernel.upcaster.EventUpcasterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise-grade JSON File implementation of IEventStore.
 * Persists domain events to local disk segmented by tenant.
 */
public class JsonFileEventStore implements IEventStore {
    private static final Logger logger = LoggerFactory.getLogger(JsonFileEventStore.class);
    private static final String DEFAULT_STORAGE_PATH = ".piloto-data/events";

    private final ObjectMapper objectMapper;
    private final Path storagePath;
    private final Map<TenantID, Object> tenantLocks = new ConcurrentHashMap<>();
    private final EventUpcasterChain upcasterChain;

    public JsonFileEventStore() {
        this(DEFAULT_STORAGE_PATH, new EventUpcasterChain());
    }

    public JsonFileEventStore(String path) {
        this(path, new EventUpcasterChain());
    }

    public JsonFileEventStore(String path, EventUpcasterChain upcasterChain) {
        this.storagePath = Paths.get(path);
        this.upcasterChain = upcasterChain;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .enable(SerializationFeature.INDENT_OUTPUT);

        initializeStorage();
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(storagePath);
            logger.info("JsonFileEventStore initialized at: {}", storagePath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create storage directory: {}", storagePath, e);
            throw new RuntimeException("Storage initialization failed", e);
        }
    }

    @Override
    public void append(DomainEvent event) {
        if (event == null)
            throw new IllegalArgumentException("Event cannot be null");

        TenantID tenantId = event.tenantId();
        synchronized (getTenantLock(tenantId)) {
            List<DomainEvent> events = loadByTenant(tenantId);
            List<DomainEvent> modifiableEvents = new ArrayList<>(events);
            modifiableEvents.add(event);
            saveEvents(tenantId, modifiableEvents);
        }
    }

    @Override
    public List<DomainEvent> loadByTenant(TenantID tenantId) {
        if (tenantId == null)
            throw new IllegalArgumentException("TenantID cannot be null");

        Path filePath = getFilePath(tenantId);
        File file = filePath.toFile();

        if (!file.exists()) {
            return Collections.emptyList();
        }

        try {
            List<DomainEvent> rawEvents = objectMapper.readValue(file, new TypeReference<List<DomainEvent>>() {
            });
            if (upcasterChain != null) {
                return rawEvents.stream().map(upcasterChain::upcast).toList();
            }
            return rawEvents;
        } catch (IOException e) {
            logger.error("Failed to load events for tenant: {}", tenantId, e);
            return Collections.emptyList();
        }
    }

    private void saveEvents(TenantID tenantId, List<DomainEvent> events) {
        Path filePath = getFilePath(tenantId);
        try {
            // Garantizar que el directorio padre existe antes de guardar
            Files.createDirectories(filePath.getParent());
            objectMapper.writeValue(filePath.toFile(), events);
            logger.debug("Successfully saved {} events for tenant {}", events.size(), tenantId);
        } catch (IOException e) {
            logger.error("CRITICAL: Failed to save events for tenant: {}. Error: {}", tenantId, e.getMessage());
            throw new RuntimeException("Persistence failed: " + e.getMessage(), e);
        }
    }

    private Path getFilePath(TenantID tenantId) {
        return storagePath.resolve(tenantId.value() + ".json");
    }

    private Object getTenantLock(TenantID tenantId) {
        return tenantLocks.computeIfAbsent(tenantId, k -> new Object());
    }
}
