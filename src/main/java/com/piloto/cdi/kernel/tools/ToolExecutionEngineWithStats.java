package com.piloto.cdi.kernel.tools;

import java.util.concurrent.CompletableFuture;

public class ToolExecutionEngineWithStats {
    private final ToolExecutionEngine engine;
    private final ToolAuditLogger auditLogger;
    private final ToolStats stats;

    public ToolExecutionEngineWithStats(
            ToolRegistry registry,
            SandboxPolicy sandbox,
            ApprovalGateway approvalGateway,
            com.piloto.cdi.kernel.diagnostics.TelemetryCollector telemetry,
            ToolAuditLogger auditLogger,
            ToolStats stats) {

        this.engine = new ToolExecutionEngine(registry, sandbox, approvalGateway, telemetry);
        this.auditLogger = auditLogger != null ? auditLogger : new ToolAuditLogger();
        this.stats = stats != null ? stats : new ToolStats();
    }

    public CompletableFuture<ToolExecutionResult> execute(ToolExecutionRequest request) {
        return engine.execute(request)
                .thenApply(result -> {
                    boolean approvalDenied = !result.isSuccess() &&
                            result.getError().orElse("").contains("Approval denied");

                    stats.recordExecution(result, approvalDenied);

                    auditLogger.record(request, result, null);

                    return result;
                });
    }

    public ToolStats getStats() {
        return stats;
    }

    public ToolAuditLogger getAuditLogger() {
        return auditLogger;
    }
}
