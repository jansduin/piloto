package com.piloto.cdi.kernel.interfaces;

import com.piloto.cdi.kernel.model.SnapshotState;
import com.piloto.cdi.kernel.types.TenantID;
import com.piloto.cdi.kernel.types.VersionID;

import java.util.Optional;

public interface ISnapshotStore {
    Optional<SnapshotState> load(TenantID tenantId);

    /**
     * Saves the snapshot with Optimistic Concurrency Control.
     * 
     * @param snapshot        the new snapshot state
     * @param expectedVersion the version ID expected to be currently in the store.
     *                        Null if anticipating no prior snapshot.
     * @throws com.piloto.cdi.kernel.exceptions.ConcurrencyConflictException if the
     *                                                                       current
     *                                                                       version
     *                                                                       does
     *                                                                       not
     *                                                                       match
     *                                                                       expectedVersion
     */
    void save(SnapshotState snapshot, VersionID expectedVersion);
}
