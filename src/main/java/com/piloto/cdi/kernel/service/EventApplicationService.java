package com.piloto.cdi.kernel.service;

import com.piloto.cdi.kernel.controller.StateTransitionController;
import com.piloto.cdi.kernel.interfaces.IEventStore;
import com.piloto.cdi.kernel.interfaces.ISnapshotStore;
import com.piloto.cdi.kernel.model.DomainEvent;
import com.piloto.cdi.kernel.model.SnapshotState;
import com.piloto.cdi.kernel.types.StateID;
import com.piloto.cdi.kernel.types.TenantID;
import com.piloto.cdi.kernel.types.VersionID;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service that coordinates event application to snapshots with per-tenant
 * serialization.
 * 
 * <p>
 * <b>Thread Safety:</b> This service uses lock striping to serialize event
 * processing
 * per tenant while allowing concurrent processing across different tenants.
 * 
 * <p>
 * <b>Scalability Limits:</b> The lock striping implementation uses 256 fixed
 * locks
 * (LOCK_STRIPES constant). This design is appropriate for:
 * <ul>
 * <li>Up to 100K active tenants with good distribution</li>
 * <li>Systems where tenant access patterns are relatively uniform</li>
 * <li>In-memory implementations of Fase 2</li>
 * </ul>
 * 
 * <p>
 * <b>Not suitable for:</b>
 * <ul>
 * <li>Millions of tenants with high churn rate</li>
 * <li>Extremely hot tenants requiring dedicated resources</li>
 * <li>Distributed systems requiring global coordination</li>
 * </ul>
 * 
 * <p>
 * <b>Failure Handling:</b> If {@code snapshotStore.save()} fails after
 * {@code eventStore.append()} succeeds, the event is persisted but the snapshot
 * becomes stale. Future phases should implement event replay recovery.
 * 
 * <p>
 * <b>Lock Fairness:</b> Uses fair locks ({@code ReentrantLock(true)}) to
 * prevent
 * starvation under high contention at the cost of ~10-15% throughput reduction.
 */
public final class EventApplicationService {
    private static final int LOCK_STRIPES = 256;

    private final IEventStore eventStore;
    private final ISnapshotStore snapshotStore;
    private final StateTransitionController transitionController;
    private final Lock[] locks;

    public EventApplicationService(
            IEventStore eventStore,
            ISnapshotStore snapshotStore,
            StateTransitionController transitionController) {

        if (eventStore == null) {
            throw new IllegalArgumentException("eventStore cannot be null");
        }
        if (snapshotStore == null) {
            throw new IllegalArgumentException("snapshotStore cannot be null");
        }
        if (transitionController == null) {
            throw new IllegalArgumentException("transitionController cannot be null");
        }

        this.eventStore = eventStore;
        this.snapshotStore = snapshotStore;
        this.transitionController = transitionController;
        this.locks = new Lock[LOCK_STRIPES];
        for (int i = 0; i < LOCK_STRIPES; i++) {
            this.locks[i] = new ReentrantLock(true);
        }
    }

    /**
     * Applies an event to the current snapshot state.
     * 
     * <p>
     * This method serializes event processing per tenant using lock striping
     * to prevent lost updates while allowing parallelism across tenants.
     * 
     * <p>
     * <b>Execution Flow:</b>
     * <ol>
     * <li>Acquire tenant-specific lock (via hash-based striping)</li>
     * <li>Load current snapshot (or create initial if not exists)</li>
     * <li>Apply state transition</li>
     * <li>Append event to event store</li>
     * <li>Save new snapshot</li>
     * <li>Release lock (always, even on failure)</li>
     * </ol>
     * 
     * <p>
     * <b>Consistency Warning:</b> If {@code snapshotStore.save()} fails after
     * {@code eventStore.append()} succeeds, the snapshot will be stale. The event
     * is persisted but the state transition is not reflected in the snapshot.
     * Future implementations should add event replay recovery.
     * 
     * @param event the event to apply (must not be null)
     * @throws IllegalArgumentException if event is null
     * @throws RuntimeException         if state transition, event storage, or
     *                                  snapshot save fails
     */
    public void applyEvent(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }

        TenantID tenantId = event.tenantId();
        Lock lock = getLockForTenant(tenantId);

        lock.lock();
        try {
            Optional<SnapshotState> currentSnapshotOpt = snapshotStore.load(tenantId);

            SnapshotState currentSnapshot;
            VersionID expectedVersion = null;
            if (currentSnapshotOpt.isPresent()) {
                currentSnapshot = currentSnapshotOpt.get();
                expectedVersion = currentSnapshot.version();
            } else {
                currentSnapshot = createInitialSnapshot(tenantId, event.timestamp());
            }

            SnapshotState newSnapshot = transitionController.applyEvent(currentSnapshot, event);

            eventStore.append(event);

            snapshotStore.save(newSnapshot, expectedVersion);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the lock for a specific tenant using hash-based striping.
     * 
     * <p>
     * Maps tenant IDs to a fixed set of locks (256) using hash code modulo.
     * This ensures bounded memory usage while providing good lock distribution.
     * 
     * @param tenantId the tenant ID
     * @return the lock assigned to this tenant
     */
    private Lock getLockForTenant(TenantID tenantId) {
        int hash = tenantId.hashCode();
        int index = (hash & 0x7FFFFFFF) % LOCK_STRIPES;
        return locks[index];
    }

    private SnapshotState createInitialSnapshot(TenantID tenantId, Instant creationTime) {
        return SnapshotState.create(
                StateID.generate(),
                tenantId,
                VersionID.generate(),
                new HashMap<>(),
                new HashSet<>(),
                creationTime,
                creationTime);
    }
}
