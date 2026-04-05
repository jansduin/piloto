package com.piloto.cdi.kernel.memory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.piloto.cdi.kernel.types.TenantID;

/**
 * Query specification for memory retrieval.
 * 
 * <p>
 * Immutable value object defining search parameters for semantic memory lookup.
 * </p>
 * 
 * @since Phase 4
 */
public final class MemoryQuery {

    private final TenantID tenantId;
    private final String query;
    private final int topK;
    private final Optional<List<MemoryType>> memoryTypes;

    private MemoryQuery(TenantID tenantId, String query, int topK, Optional<List<MemoryType>> memoryTypes) {
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be > 0, got: " + topK);
        }

        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.query = Objects.requireNonNull(query, "query cannot be null");
        this.topK = topK;
        this.memoryTypes = Objects.requireNonNull(memoryTypes, "memoryTypes Optional cannot be null");
    }

    /**
     * Creates query with all memory types.
     * 
     * @param tenantId tenant identifier
     * @param query    search text
     * @param topK     maximum results to return
     * @return new MemoryQuery
     */
    public static MemoryQuery create(TenantID tenantId, String query, int topK) {
        return new MemoryQuery(tenantId, query, topK, Optional.empty());
    }

    /**
     * Creates query filtered by specific memory types.
     * 
     * @param tenantId    tenant identifier
     * @param query       search text
     * @param topK        maximum results
     * @param memoryTypes types to search (null = all types)
     * @return new MemoryQuery
     */
    public static MemoryQuery createFiltered(TenantID tenantId, String query, int topK, List<MemoryType> memoryTypes) {
        return new MemoryQuery(tenantId, query, topK, Optional.ofNullable(memoryTypes));
    }

    public TenantID getTenantId() {
        return tenantId;
    }

    public String getQuery() {
        return query;
    }

    public int getTopK() {
        return topK;
    }

    public Optional<List<MemoryType>> getMemoryTypes() {
        return memoryTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MemoryQuery that = (MemoryQuery) o;
        return topK == that.topK &&
                tenantId.equals(that.tenantId) &&
                query.equals(that.query) &&
                memoryTypes.equals(that.memoryTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, query, topK, memoryTypes);
    }

    @Override
    public String toString() {
        return "MemoryQuery{" +
                "tenantId=" + tenantId +
                ", query='" + query + '\'' +
                ", topK=" + topK +
                ", memoryTypes=" + memoryTypes +
                '}';
    }
}
