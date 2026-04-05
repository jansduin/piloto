package com.piloto.cdi.kernel.tools;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.piloto.cdi.kernel.diagnostics.TelemetryCollector;

public class ToolExecutionEngine {
    private final ToolRegistry registry;
    private final SandboxPolicy sandbox;
    private final ApprovalGateway approvalGateway;
    private final TelemetryCollector telemetry;

    public ToolExecutionEngine(ToolRegistry registry, SandboxPolicy sandbox, ApprovalGateway approvalGateway,
            TelemetryCollector telemetry) {
        if (registry == null) {
            throw new IllegalArgumentException("registry must not be null");
        }
        if (sandbox == null) {
            throw new IllegalArgumentException("sandbox must not be null");
        }
        if (approvalGateway == null) {
            throw new IllegalArgumentException("approvalGateway must not be null");
        }

        this.registry = registry;
        this.sandbox = sandbox;
        this.approvalGateway = approvalGateway;
        this.telemetry = telemetry;
    }

    public CompletableFuture<ToolExecutionResult> execute(ToolExecutionRequest request) {
        return execute(request, null);
    }

    public CompletableFuture<ToolExecutionResult> execute(ToolExecutionRequest request, SagaContext sagaContext) {
        if (request == null) {
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.failure("request must not be null", 0));
        }

        long startTime = System.currentTimeMillis();

        try {
            BaseTool tool = registry.get(request.getToolName());

            sandbox.validate(tool);

            if (tool.getDefinition().requiresApproval()) {
                return approvalGateway.requestApproval(tool, request.getPayload())
                        .thenCompose(decision -> {
                            if (!decision.isApproved()) {
                                long elapsed = System.currentTimeMillis() - startTime;
                                return CompletableFuture.completedFuture(
                                        ToolExecutionResult.failure("Approval denied: " + decision.getRationale(),
                                                elapsed));
                            }

                            return executeInternal(tool, request, startTime, sagaContext);
                        });
            }

            return executeInternal(tool, request, startTime, sagaContext);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.failure(e.getMessage(), elapsed));
        }
    }

    private CompletableFuture<ToolExecutionResult> executeInternal(BaseTool tool, ToolExecutionRequest request,
            long startTime, SagaContext sagaContext) {
        if (request.isDryRun()) {
            long elapsed = System.currentTimeMillis() - startTime;
            telemetry.record("TOOL", "DRY_RUN", Map.of("tool", tool.getDefinition().getName()));
            return CompletableFuture.completedFuture(
                    ToolExecutionResult.success(Map.of("dry_run", true, "tool", tool.getDefinition().getName()),
                            elapsed));
        }

        telemetry.record("TOOL", "EXECUTION_STARTED",
                Map.of("tool", tool.getDefinition().getName(), "payload", request.getPayload()));

        CompletableFuture<Map<String, Object>> execution = tool.execute(request.getPayload());

        CompletableFuture<Map<String, Object>> timeoutEnforced = sandbox.enforceTimeout(execution);

        return timeoutEnforced
                .thenApply(output -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (sagaContext != null) {
                        sagaContext.addSuccessfulExecution(request);
                    }
                    telemetry.record("TOOL", "EXECUTION_SUCCESS",
                            Map.of("tool", tool.getDefinition().getName(), "elapsed", elapsed));
                    return ToolExecutionResult.success(output, elapsed);
                })
                .exceptionally(error -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    telemetry.record("TOOL", "EXECUTION_FAILED",
                            Map.of("tool", tool.getDefinition().getName(), "error", error.getMessage(), "elapsed",
                                    elapsed));
                    return ToolExecutionResult.failure(error.getMessage(), elapsed);
                });
    }

    public CompletableFuture<Void> rollback(SagaContext sagaContext) {
        if (sagaContext == null || sagaContext.getExecutionLog().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> rollbackFuture = CompletableFuture.completedFuture(null);

        for (ToolExecutionRequest request : sagaContext.getExecutionLog()) {
            rollbackFuture = rollbackFuture.thenCompose(v -> {
                BaseTool tool = registry.get(request.getToolName());
                if (tool != null) {
                    try {
                        return tool.compensate(request.getPayload())
                                .exceptionally(err -> {
                                    System.err.println("Compensation failed for tool " + request.getToolName() + ": "
                                            + err.getMessage());
                                    return null;
                                });
                    } catch (Exception e) {
                        System.err.println(
                                "Compensation exception for tool " + request.getToolName() + ": " + e.getMessage());
                        return CompletableFuture.completedFuture(null);
                    }
                }
                return CompletableFuture.completedFuture(null);
            });
        }

        return rollbackFuture;
    }
}
