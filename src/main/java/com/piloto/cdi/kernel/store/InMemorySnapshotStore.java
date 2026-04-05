package com.piloto.cdi.kernel.store;

import com.piloto.cdi.kernel.interfaces.ISnapshotStore;
import com.piloto.cdi.kernel.model.SnapshotState;
import com.piloto.cdi.kernel.types.TenantID;
import com.piloto.cdi.kernel.types.VersionID;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySnapshotStore implements ISnapshotStore {
    private final Map<TenantID, SnapshotState> storage;

    public InMemorySnapshotStore() {
        this.storage = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<SnapshotState> load(TenantID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId cannot be null");
        }

        return Optional.ofNullable(storage.get(tenantId));
    }

    @Override
    public void save(SnapshotState snapshot, VersionID expectedVersion) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot cannot be null");
        }

        storage.compute(snapshot.tenantId(), (id, current) -> {
            if (expectedVersion == null) {
                if (current != null) {
                    throw new com.piloto.cdi.kernel.exceptions.ConcurrencyConflictException(
                            "Expected no snapshot but found one for tenant " + id);
                }
            } else {
                if (current == null || !current.version().equals(expectedVersion)) {
                    throw new com.piloto.cdi.kernel.exceptions.ConcurrencyConflictException(
                            "Version conflict for tenant " + id + ". Expected " + expectedVersion +
                                    ", found " + (current == null ? "nothing" : current.version()));
                }
            }
            return snapshot;
        });
    }
}
