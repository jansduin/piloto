package com.piloto.cdi.kernel.distributed;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IdempotencyManager {

    private final Map<String, Instant> executionRegistry = new ConcurrentHashMap<>();
    private final long retentionMinutes;

    public IdempotencyManager(long retentionMinutes) {
        if (retentionMinutes < 0) {
            throw new IllegalArgumentException("Retention minutes must be positive");
        }
        this.retentionMinutes = retentionMinutes;
    }

    public IdempotencyManager() {
        this(60);
    }

    public synchronized boolean register(String executionId) {
        if (executionId == null || executionId.isEmpty()) {
            throw new IllegalArgumentException("ExecutionId cannot be null or empty");
        }

        if (isDuplicate(executionId)) {
            return false;
        }

        executionRegistry.put(executionId, Instant.now());
        return true;
    }

    public boolean isDuplicate(String executionId) {
        if (executionId == null || executionId.isEmpty()) {
            return false;
        }

        return executionRegistry.containsKey(executionId);
    }

    public synchronized void clearOld() {
        Instant cutoff = Instant.now().minus(retentionMinutes, ChronoUnit.MINUTES);

        executionRegistry.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }

    public Map<String, Object> getStatistics() {
        return Map.of(
                "total_executions", executionRegistry.size(),
                "retention_minutes", retentionMinutes);
    }

    public void clear() {
        executionRegistry.clear();
    }

    public int size() {
        return executionRegistry.size();
    }
}
