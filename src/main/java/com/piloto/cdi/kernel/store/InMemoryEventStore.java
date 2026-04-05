package com.piloto.cdi.kernel.store;

import com.piloto.cdi.kernel.interfaces.IEventStore;
import com.piloto.cdi.kernel.model.DomainEvent;
import com.piloto.cdi.kernel.types.TenantID;
import com.piloto.cdi.kernel.upcaster.EventUpcasterChain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of IEventStore with thread-safe event storage per
 * tenant.
 * 
 * <p>
 * <b>Thread Safety:</b> Uses {@code ConcurrentHashMap} with
 * {@code Collections.synchronizedList}
 * for thread-safe append and load operations.
 * 
 * <p>
 * <b>Atomicity of computeIfAbsent:</b> The {@code computeIfAbsent} method is
 * atomic in
 * ConcurrentHashMap. Only one thread will execute the lambda for a given key,
 * preventing
 * duplicate list creation. Once computed, the list is safely published to all
 * threads.
 * 
 * <p>
 * <b>Performance Considerations:</b>
 * <ul>
 * <li>Append: O(1) amortized, synchronized per tenant's list</li>
 * <li>Load: O(N) where N is event count, blocks appends during copy</li>
 * <li>For tenants with >100K events, consider CopyOnWriteArrayList if reads >>
 * writes</li>
 * </ul>
 * 
 * <p>
 * <b>Scalability Limits:</b> Suitable for Fase 2 in-memory prototype. For
 * production:
 * <ul>
 * <li>Migrate to append-log persistent storage (e.g., Kafka, EventStoreDB)</li>
 * <li>Implement event archival for old events</li>
 * <li>Use read-optimized indexes for queries</li>
 * </ul>
 */
public class InMemoryEventStore implements IEventStore {
    private final Map<TenantID, List<DomainEvent>> storage;
    private final EventUpcasterChain upcasterChain;

    public InMemoryEventStore() {
        this(new EventUpcasterChain());
    }

    public InMemoryEventStore(EventUpcasterChain upcasterChain) {
        this.storage = new ConcurrentHashMap<>();
        this.upcasterChain = upcasterChain;
    }

    /**
     * Appends an event to the store for its tenant.
     * 
     * <p>
     * Thread-safe operation that uses atomic list creation and synchronized append.
     * 
     * @param event the event to append (must not be null)
     * @throws IllegalArgumentException if event is null
     */
    @Override
    public void append(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }

        storage.computeIfAbsent(event.tenantId(), k -> Collections.synchronizedList(new ArrayList<>())).add(event);
    }

    /**
     * Loads all events for a tenant in append order.
     * 
     * <p>
     * <b>Thread Safety:</b> Synchronizes on the event list during copy to prevent
     * inconsistent reads. This blocks concurrent appends for the same tenant during
     * the copy operation.
     * 
     * <p>
     * <b>Performance Warning:</b> For tenants with large event histories, this
     * operation can be slow (~several hundred ms for 100K events) and blocks writes
     * during the copy. Consider pagination or streaming for large result sets in
     * future phases.
     * 
     * @param tenantId the tenant ID (must not be null)
     * @return immutable copy of events for this tenant, or empty list if tenant
     *         unknown
     * @throws IllegalArgumentException if tenantId is null
     */
    @Override
    public List<DomainEvent> loadByTenant(TenantID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId cannot be null");
        }

        List<DomainEvent> events = storage.get(tenantId);
        if (events == null) {
            return List.of();
        }

        synchronized (events) {
            if (upcasterChain != null) {
                return events.stream().map(upcasterChain::upcast).toList();
            }
            return List.copyOf(events);
        }
    }
}
