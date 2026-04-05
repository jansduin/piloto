package com.piloto.cdi.kernel.diagnostics;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SystemStats {
    private final Map<String, Object> memoryStats;
    private final Map<String, Object> toolStats;
    private final Map<String, Object> reasoningStats;
    private final Map<String, Object> diagnosticStats;

    private SystemStats(
        Map<String, Object> memoryStats,
        Map<String, Object> toolStats,
        Map<String, Object> reasoningStats,
        Map<String, Object> diagnosticStats
    ) {
        this.memoryStats = Map.copyOf(memoryStats);
        this.toolStats = Map.copyOf(toolStats);
        this.reasoningStats = Map.copyOf(reasoningStats);
        this.diagnosticStats = Map.copyOf(diagnosticStats);
    }

    public static SystemStats create() {
        return new SystemStats(
            createEmptyMemoryStats(),
            createEmptyToolStats(),
            createEmptyReasoningStats(),
            createEmptyDiagnosticStats()
        );
    }

    public static SystemStats withStats(
        Map<String, Object> memoryStats,
        Map<String, Object> toolStats,
        Map<String, Object> reasoningStats,
        Map<String, Object> diagnosticStats
    ) {
        return new SystemStats(
            memoryStats != null ? memoryStats : createEmptyMemoryStats(),
            toolStats != null ? toolStats : createEmptyToolStats(),
            reasoningStats != null ? reasoningStats : createEmptyReasoningStats(),
            diagnosticStats != null ? diagnosticStats : createEmptyDiagnosticStats()
        );
    }

    public SystemStats updateMemoryStats(Map<String, Object> newStats) {
        return new SystemStats(newStats, toolStats, reasoningStats, diagnosticStats);
    }

    public SystemStats updateToolStats(Map<String, Object> newStats) {
        return new SystemStats(memoryStats, newStats, reasoningStats, diagnosticStats);
    }

    public SystemStats updateReasoningStats(Map<String, Object> newStats) {
        return new SystemStats(memoryStats, toolStats, newStats, diagnosticStats);
    }

    public SystemStats updateDiagnosticStats(Map<String, Object> newStats) {
        return new SystemStats(memoryStats, toolStats, reasoningStats, newStats);
    }

    public Map<String, Object> getMemoryStats() {
        return memoryStats;
    }

    public Map<String, Object> getToolStats() {
        return toolStats;
    }

    public Map<String, Object> getReasoningStats() {
        return reasoningStats;
    }

    public Map<String, Object> getDiagnosticStats() {
        return diagnosticStats;
    }

    public Map<String, Object> getAllStats() {
        Map<String, Object> all = new HashMap<>();
        all.put("memory", memoryStats);
        all.put("tool", toolStats);
        all.put("reasoning", reasoningStats);
        all.put("diagnostic", diagnosticStats);
        return Map.copyOf(all);
    }

    private static Map<String, Object> createEmptyMemoryStats() {
        return Map.of(
            "total_entries", 0,
            "last_query_time", 0L,
            "memory_health_score", 1.0
        );
    }

    private static Map<String, Object> createEmptyToolStats() {
        return Map.of(
            "total_executions", 0,
            "failed_executions", 0,
            "approval_denials", 0,
            "avg_execution_time_ms", 0.0
        );
    }

    private static Map<String, Object> createEmptyReasoningStats() {
        return Map.of(
            "total_runs", 0,
            "total_iterations", 0,
            "avg_score", 0.0,
            "failed_runs", 0
        );
    }

    private static Map<String, Object> createEmptyDiagnosticStats() {
        return Map.of(
            "last_health_score", 1.0,
            "anomaly_count", 0,
            "last_diagnostic_timestamp", 0L,
            "optimization_pending", false
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemStats that = (SystemStats) o;
        return memoryStats.equals(that.memoryStats) &&
               toolStats.equals(that.toolStats) &&
               reasoningStats.equals(that.reasoningStats) &&
               diagnosticStats.equals(that.diagnosticStats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memoryStats, toolStats, reasoningStats, diagnosticStats);
    }

    @Override
    public String toString() {
        return "SystemStats{" +
               "memory=" + memoryStats +
               ", tool=" + toolStats +
               ", reasoning=" + reasoningStats +
               ", diagnostic=" + diagnosticStats +
               '}';
    }
}
