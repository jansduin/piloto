package com.piloto.cdi.kernel.tools.selection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-tool statistics registry.
 *
 * Unlike ToolStats (global singleton), this tracks execution/failure counts
 * per individual tool — required for UCB1 and other bandit algorithms.
 *
 * Thread-safe via ConcurrentHashMap + AtomicLong.
 */
public class PerToolStatsRegistry {

    private final Map<String, ToolRecord> records = new ConcurrentHashMap<>();

    /**
     * Record an execution result for a specific tool.
     */
    public void recordExecution(String toolName, boolean success) {
        if (toolName == null || toolName.isBlank())
            throw new IllegalArgumentException("toolName must not be blank");
        records.computeIfAbsent(toolName, k -> new ToolRecord())
                .record(success);
    }

    /**
     * Build a candidate list for the selection policy.
     * Tools not yet in the registry get 0 executions (ensures explore-first).
     */
    public List<ToolSelectionPolicy.ToolCandidate> getCandidates(List<String> toolNames) {
        List<ToolSelectionPolicy.ToolCandidate> candidates = new ArrayList<>();
        for (String name : toolNames) {
            ToolRecord rec = records.getOrDefault(name, new ToolRecord());
            candidates.add(new ToolSelectionPolicy.ToolCandidate(
                    name, rec.executions.get(), rec.failures.get()));
        }
        return candidates;
    }

    /**
     * Get stats for a specific tool, or null if never executed.
     */
    public ToolSelectionPolicy.ToolCandidate getStats(String toolName) {
        ToolRecord rec = records.get(toolName);
        if (rec == null) return null;
        return new ToolSelectionPolicy.ToolCandidate(
                toolName, rec.executions.get(), rec.failures.get());
    }

    /**
     * Reset all stats. Use with caution.
     */
    public void reset() {
        records.clear();
    }

    private static class ToolRecord {
        final AtomicLong executions = new AtomicLong(0);
        final AtomicLong failures = new AtomicLong(0);

        void record(boolean success) {
            executions.incrementAndGet();
            if (!success) failures.incrementAndGet();
        }
    }
}
