package com.piloto.cdi.kernel.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.piloto.cdi.kernel.interfaces.ISnapshotStore;
import com.piloto.cdi.kernel.model.SnapshotState;
import com.piloto.cdi.kernel.types.TenantID;
import com.piloto.cdi.kernel.types.VersionID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Enterprise-grade JSON File implementation of ISnapshotStore.
 * Saves the latest state of aggregates for fast recovery.
 */
public class JsonFileSnapshotStore implements ISnapshotStore {
    private static final Logger logger = LoggerFactory.getLogger(JsonFileSnapshotStore.class);
    private static final String DEFAULT_STORAGE_PATH = ".piloto-data/snapshots";

    private final ObjectMapper objectMapper;
    private final Path storagePath;

    public JsonFileSnapshotStore() {
        this(DEFAULT_STORAGE_PATH);
    }

    public JsonFileSnapshotStore(String path) {
        this.storagePath = Paths.get(path);
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .enable(SerializationFeature.INDENT_OUTPUT);

        initializeStorage();
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(storagePath);
        } catch (IOException e) {
            logger.error("Failed to create snapshot directory: {}", storagePath, e);
            throw new RuntimeException("Snapshot storage initialization failed", e);
        }
    }

    @Override
    public Optional<SnapshotState> load(TenantID tenantId) {
        if (tenantId == null)
            throw new IllegalArgumentException("TenantID cannot be null");

        Path filePath = getFilePath(tenantId);
        File file = filePath.toFile();

        if (!file.exists()) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(file, SnapshotState.class));
        } catch (IOException e) {
            logger.error("Failed to load snapshot for tenant: {}", tenantId, e);
            return Optional.empty();
        }
    }

    @Override
    public synchronized void save(SnapshotState snapshot, VersionID expectedVersion) {
        if (snapshot == null)
            throw new IllegalArgumentException("Snapshot cannot be null");

        TenantID tenantId = snapshot.tenantId();

        Optional<SnapshotState> currentOpt = load(tenantId);
        if (expectedVersion == null) {
            if (currentOpt.isPresent()) {
                throw new com.piloto.cdi.kernel.exceptions.ConcurrencyConflictException(
                        "Expected no snapshot, but one exists for tenant: " + tenantId);
            }
        } else {
            if (currentOpt.isEmpty() || !currentOpt.get().version().equals(expectedVersion)) {
                throw new com.piloto.cdi.kernel.exceptions.ConcurrencyConflictException(
                        "Optimistic concurrency conflict for tenant: " + tenantId +
                                ". Expected " + expectedVersion + ", found " +
                                (currentOpt.isPresent() ? currentOpt.get().version() : "none"));
            }
        }

        Path filePath = getFilePath(tenantId);

        try {
            objectMapper.writeValue(filePath.toFile(), snapshot);
            logger.debug("Successfully saved snapshot for tenant {}", tenantId);
        } catch (IOException e) {
            logger.error("Failed to save snapshot for tenant: {}", tenantId, e);
            throw new RuntimeException("Snapshot persistence failed", e);
        }
    }

    private Path getFilePath(TenantID tenantId) {
        return storagePath.resolve(tenantId.value() + ".json");
    }
}
