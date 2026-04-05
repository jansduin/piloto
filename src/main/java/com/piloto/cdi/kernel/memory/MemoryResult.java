package com.piloto.cdi.kernel.memory;

import java.util.List;
import java.util.Objects;

/**
 * Result of memory query with aggregated context.
 * 
 * <p>Contains retrieved entries and a synthesized context string
 * suitable for LLM consumption.</p>
 * 
 * @since Phase 4
 */
public final class MemoryResult {
    
    private final List<MemoryEntry> entries;
    private final String aggregatedContext;
    
    private MemoryResult(List<MemoryEntry> entries, String aggregatedContext) {
        this.entries = List.copyOf(Objects.requireNonNull(entries, "entries cannot be null"));
        this.aggregatedContext = Objects.requireNonNull(aggregatedContext, "aggregatedContext cannot be null");
    }
    
    /**
     * Creates result with retrieved entries and aggregated context.
     * 
     * @param entries list of matching memory entries
     * @param aggregatedContext synthesized context string
     * @return new MemoryResult
     */
    public static MemoryResult create(List<MemoryEntry> entries, String aggregatedContext) {
        return new MemoryResult(entries, aggregatedContext);
    }
    
    /**
     * Creates empty result (no matches).
     * 
     * @return MemoryResult with empty entries and context
     */
    public static MemoryResult empty() {
        return new MemoryResult(List.of(), "");
    }
    
    public List<MemoryEntry> getEntries() {
        return entries;
    }
    
    public String getAggregatedContext() {
        return aggregatedContext;
    }
    
    public boolean isEmpty() {
        return entries.isEmpty();
    }
    
    public int size() {
        return entries.size();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemoryResult that = (MemoryResult) o;
        return entries.equals(that.entries) &&
               aggregatedContext.equals(that.aggregatedContext);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(entries, aggregatedContext);
    }
    
    @Override
    public String toString() {
        return "MemoryResult{" +
               "entries=" + entries.size() + " items" +
               ", aggregatedContext=" + (aggregatedContext.length() > 100 ? 
                   aggregatedContext.substring(0, 100) + "..." : aggregatedContext) +
               '}';
    }
}
