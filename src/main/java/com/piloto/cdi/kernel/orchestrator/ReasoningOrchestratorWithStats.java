package com.piloto.cdi.kernel.orchestrator;

import com.piloto.cdi.kernel.memory.MemoryManager;
import com.piloto.cdi.kernel.tools.ToolExecutionEngine;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.piloto.cdi.kernel.types.TenantID;

public class ReasoningOrchestratorWithStats {
    private final ReasoningOrchestrator orchestrator;
    private final ReasoningStats stats;

    public ReasoningOrchestratorWithStats(
            MemoryManager memoryManager,
            ToolExecutionEngine toolEngine,
            AgentCoordinator coordinator,
            SelfEvaluator evaluator,
            com.piloto.cdi.kernel.diagnostics.TelemetryCollector telemetry,
            ReasoningStats stats) {

        this.orchestrator = new ReasoningOrchestrator(memoryManager, toolEngine, coordinator, evaluator, telemetry);
        this.stats = stats != null ? stats : new ReasoningStats();
    }

    public CompletableFuture<Map<String, Object>> run(TenantID tenantId, String goal) {
        return orchestrator.run(tenantId, goal)
                .thenApply(result -> {
                    boolean success = (Boolean) result.getOrDefault("success", false);
                    int iterations = (Integer) result.getOrDefault("iterations", 0);
                    double score = ((Number) result.getOrDefault("score", 0.0)).doubleValue();

                    stats.recordRun(success, iterations, score);

                    return result;
                });
    }

    public ReasoningStats getStats() {
        return stats;
    }
}
