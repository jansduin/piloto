package com.piloto.cdi.kernel.orchestrator;

import com.piloto.cdi.kernel.memory.MemoryManager;
import com.piloto.cdi.kernel.memory.MemoryQuery;
import com.piloto.cdi.kernel.tools.SagaContext;
import com.piloto.cdi.kernel.tools.ToolExecutionEngine;
import com.piloto.cdi.kernel.security.AdversarialSanitizer;
import com.piloto.cdi.kernel.types.TenantID;
import com.piloto.cdi.kernel.diagnostics.TelemetryCollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ReasoningOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(ReasoningOrchestrator.class);
    private final MemoryManager memoryManager;
    private final ToolExecutionEngine toolEngine;
    private final SelfEvaluator evaluator;
    private final TaskDecomposer decomposer;
    private final DeliberationStateMachine deliberationMachine;
    private final AdversarialSanitizer sanitizer;
    private final TelemetryCollector telemetry;

    private static final int MAX_ITERATIONS = 3;
    private static final double SCORE_THRESHOLD = 0.7;

    public ReasoningOrchestrator(
            MemoryManager memoryManager,
            ToolExecutionEngine toolEngine,
            AgentCoordinator coordinator,
            SelfEvaluator evaluator,
            TelemetryCollector telemetry) {

        if (coordinator == null) {
            throw new IllegalArgumentException("coordinator must not be null");
        }
        if (evaluator == null) {
            throw new IllegalArgumentException("evaluator must not be null");
        }

        this.memoryManager = memoryManager;
        this.toolEngine = toolEngine;
        this.evaluator = evaluator;
        this.decomposer = new TaskDecomposer();
        this.deliberationMachine = new DeliberationStateMachine(coordinator);
        this.sanitizer = new AdversarialSanitizer();
        this.telemetry = telemetry;
    }

    public CompletableFuture<Map<String, Object>> run(TenantID tenantId, String goal) {
        // GAP 6: Sanitize Ingress
        String sanitizedGoal = sanitizer.sanitize(goal);
        return runWithContext(tenantId, sanitizedGoal, new HashMap<>());
    }

    public CompletableFuture<Map<String, Object>> runWithContext(TenantID tenantId, String goal,
            Map<String, Object> initialContext) {
        telemetry.record("ORCHESTRATOR", "ORCHESTRATION_STARTED", Map.of("goal", goal, "tenant", tenantId.toString()));
        if (goal == null || goal.isBlank()) {
            return CompletableFuture.completedFuture(createFailureResult("Goal is empty", 0, new ExecutionTrace()));
        }

        ExecutionTrace trace = new ExecutionTrace();
        ReasoningState initialState = ReasoningState.create(goal);

        // Pre-cargar contexto inyectado desde el Gateway (ej. Salud, Diagnósticos)
        if (initialContext != null && !initialContext.isEmpty()) {
            for (Map.Entry<String, Object> entry : initialContext.entrySet()) {
                initialState = initialState.addIntermediateResult(entry.getKey(), entry.getValue());
            }
        }

        return runIteration(tenantId, initialState, trace);
    }

    private CompletableFuture<Map<String, Object>> runIteration(TenantID tenantId, ReasoningState state,
            ExecutionTrace trace) {
        if (state.getIteration() >= MAX_ITERATIONS) {
            return CompletableFuture.completedFuture(
                    createFailureResult("Maximum iterations reached without acceptable score", state.getIteration(),
                            trace));
        }

        SagaContext sagaContext = new SagaContext();

        return buildContext(tenantId, state, trace)
                .thenCompose(contextState -> decomposeTask(tenantId, contextState, trace))
                .thenCompose(decomposedState -> executeTasks(tenantId, decomposedState, trace, sagaContext))
                .thenCompose(executedState -> evaluateResults(tenantId, executedState, trace))
                .thenCompose(evaluatedState -> {
                    Map<String, Object> evalResult = evaluatedState.getIntermediateResults();
                    double score = ((Number) evalResult.getOrDefault("evaluation_score", 0.0)).doubleValue();

                    if (score >= SCORE_THRESHOLD) {
                        Map<String, Object> result = createSuccessResult(evaluatedState, score, trace);
                        telemetry.record("ORCHESTRATOR", "ORCHESTRATION_SUCCESS",
                                Map.of("score", score, "iterations", evaluatedState.getIteration() + 1));
                        return CompletableFuture.completedFuture(result);
                    } else {
                        return toolEngine.rollback(sagaContext).thenCompose(v -> {
                            ReasoningState nextState = evaluatedState.incrementIteration();
                            return runIteration(tenantId, nextState, trace);
                        });
                    }
                })
                .exceptionally(error -> {
                    telemetry.record("ORCHESTRATOR", "ORCHESTRATION_FAILED",
                            Map.of("error", error.getMessage(), "iterations", state.getIteration()));
                    return createFailureResult(error.getMessage(), state.getIteration(), trace);
                });
    }

    private CompletableFuture<ReasoningState> buildContext(TenantID tenantId, ReasoningState state,
            ExecutionTrace trace) {
        ReasoningState contextState = state.transitionTo(ReasoningStage.CONTEXT_BUILDING);

        trace.record(ReasoningStage.CONTEXT_BUILDING, "orchestrator",
                Map.of("goal", state.getCurrentGoal()),
                Map.of());

        if (memoryManager == null) {
            return CompletableFuture.completedFuture(contextState);
        }

        // Si ya hay contexto de memoria y salud inyectado (ej. desde el Gateway),
        // usarlo y no sobreescribir con una consulta redundante de menor TopK.
        if (state.getIntermediateResults() != null &&
                state.getIntermediateResults().containsKey("memory_context") &&
                !state.getIntermediateResults().get("memory_context").toString().contains("[Nueva sesión")) {

            logger.info("Using pre-injected memory context, skipping redundant RAG query.");
            return CompletableFuture.completedFuture(contextState);
        }

        MemoryQuery query = MemoryQuery.create(tenantId, state.getCurrentGoal(), 5);

        return memoryManager.queryMemory(query)
                .thenApply(memoryResult -> {
                    Map<String, Object> context = new HashMap<>();
                    context.put("memory_context", memoryResult.getAggregatedContext());
                    context.put("relevant_entries", memoryResult.size());

                    return contextState.addIntermediateResult("context", context);
                })
                .exceptionally(error -> contextState);
    }

    private CompletableFuture<ReasoningState> decomposeTask(TenantID tenantId, ReasoningState state,
            ExecutionTrace trace) {
        ReasoningState decomposingState = state.transitionTo(ReasoningStage.TASK_DECOMPOSITION);

        return decomposer.decompose(state.getCurrentGoal())
                .thenApply(subTasks -> {
                    trace.record(ReasoningStage.TASK_DECOMPOSITION, "decomposer",
                            Map.of("goal", state.getCurrentGoal()),
                            Map.of("subtasks", subTasks));

                    return decomposingState.withSubTasks(subTasks);
                });
    }

    private CompletableFuture<ReasoningState> executeTasks(TenantID tenantId, ReasoningState state,
            ExecutionTrace trace, SagaContext sagaContext) {
        ReasoningState executingState = state.transitionTo(ReasoningStage.EXECUTION);

        List<String> subTasks = state.getSubTasks();

        if (subTasks.isEmpty()) {
            return CompletableFuture.completedFuture(
                    executingState.addIntermediateResult("execution_result", Map.of("error", "No tasks to execute")));
        }

        CompletableFuture<ReasoningState> resultFuture = CompletableFuture.completedFuture(executingState);

        for (String task : subTasks) {
            resultFuture = resultFuture
                    .thenCompose(currentState -> executeTask(tenantId, task, currentState, trace, sagaContext)
                            .thenApply(taskResult -> {
                                ReasoningState updated = currentState.markTaskCompleted(task);
                                updated = updated.addIntermediateResult("task_" + task.hashCode(), taskResult);
                                return updated;
                            }));
        }

        return resultFuture;
    }

    private CompletableFuture<Map<String, Object>> executeTask(TenantID tenantId, String task, ReasoningState state,
            ExecutionTrace trace, SagaContext sagaContext) {
        Map<String, Object> context = new HashMap<>();
        context.put("goal", state.getCurrentGoal());
        context.put("iteration", state.getIteration());
        context.put("saga_context", sagaContext);

        // Traspasar estado y contexto pre-calculado, incluyendo memoria RAG y estado de
        // Salud (Diagnósticos)
        if (state.getIntermediateResults() != null) {
            context.putAll(state.getIntermediateResults());
        }

        return deliberationMachine.runDeliberation(task, context, trace)
                .thenApply(deliberationResult -> {
                    trace.record(ReasoningStage.EXECUTION, "deliberation_machine",
                            Map.of("task", task),
                            deliberationResult);

                    return deliberationResult;
                });
    }

    private CompletableFuture<ReasoningState> evaluateResults(TenantID tenantId, ReasoningState state,
            ExecutionTrace trace) {
        ReasoningState evaluatingState = state.transitionTo(ReasoningStage.SELF_EVALUATION);

        Map<String, Object> results = new HashMap<>();
        results.put("completed_tasks", state.getCompletedTasks());
        results.put("output", state.getIntermediateResults());
        results.put("iteration", state.getIteration());

        return evaluator.evaluate(state.getCurrentGoal(), results)
                .thenApply(evalResult -> {
                    trace.record(ReasoningStage.SELF_EVALUATION, "evaluator",
                            Map.of("goal", state.getCurrentGoal()),
                            evalResult);

                    double score = ((Number) evalResult.getOrDefault("score", 0.0)).doubleValue();

                    ReasoningState updated = evaluatingState
                            .addIntermediateResult("evaluation_score", score)
                            .addIntermediateResult("evaluation_issues", evalResult.get("issues"))
                            .addIntermediateResult("evaluation_improvements", evalResult.get("improvements"));

                    return updated;
                });
    }

    private Map<String, Object> createSuccessResult(ReasoningState state, double score, ExecutionTrace trace) {
        ReasoningState finalState = state.transitionTo(ReasoningStage.FINALIZATION);

        trace.record(ReasoningStage.FINALIZATION, "orchestrator",
                Map.of("score", score),
                Map.of("success", true));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("goal", finalState.getCurrentGoal());
        result.put("score", score);
        result.put("iterations", finalState.getIteration() + 1);
        result.put("completed_tasks", finalState.getCompletedTasks());
        result.put("output", finalState.getIntermediateResults());
        result.put("trace", trace.getFullTrace());

        return result;
    }

    private Map<String, Object> createFailureResult(String error, int iterations, ExecutionTrace trace) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", error);
        result.put("iterations", iterations);
        result.put("trace", trace.getFullTrace());

        return result;
    }
}
