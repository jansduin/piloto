package com.piloto.cdi.kernel.orchestrator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;

public class DeliberationStateMachine {
    private final AgentCoordinator coordinator;

    public DeliberationStateMachine(AgentCoordinator coordinator) {
        if (coordinator == null) {
            throw new IllegalArgumentException("coordinator must not be null");
        }
        this.coordinator = coordinator;
    }

    public CompletableFuture<Map<String, Object>> runDeliberation(String task, Map<String, Object> baseContext,
            ExecutionTrace trace) {
        Map<String, Object> finalResult = new HashMap<>();
        List<Map<String, Object>> deliberationHistory = new ArrayList<>();

        // 1. Proposal
        return runStage("Proposer", ReasoningStage.PROPOSAL_GENERATION, task, baseContext, deliberationHistory, trace)
                .thenCompose(proposal -> {
                    deliberationHistory.add(Map.of("stage", "PROPOSAL", "content", proposal));
                    // 2. Critique
                    return runStage("Critic", ReasoningStage.CRITIQUE, task, baseContext, deliberationHistory, trace);
                })
                .thenCompose(critique -> {
                    deliberationHistory.add(Map.of("stage", "CRITIQUE", "content", critique));
                    // 3. Fact Check
                    return runStage("Verifier", ReasoningStage.FACT_CHECK, task, baseContext, deliberationHistory,
                            trace);
                })
                .thenCompose(factCheck -> {
                    deliberationHistory.add(Map.of("stage", "FACT_CHECK", "content", factCheck));
                    // 4. Arbitration
                    return runStage("Arbiter", ReasoningStage.ARBITRATION, task, baseContext, deliberationHistory,
                            trace);
                })
                .thenApply(arbitration -> {
                    deliberationHistory.add(Map.of("stage", "ARBITRATION", "content", arbitration));

                    finalResult.put("deliberation_history", deliberationHistory);
                    finalResult.put("final_decision", arbitration);
                    finalResult.put("success", arbitration.getOrDefault("success", true));
                    return finalResult;
                });
    }

    private CompletableFuture<Map<String, Object>> runStage(String expectedRole, ReasoningStage stage, String task,
            Map<String, Object> baseContext, List<Map<String, Object>> history, ExecutionTrace trace) {
        Map<String, Object> context = new HashMap<>(baseContext);
        context.put("preferred_role", expectedRole);
        context.put("deliberation_history", history);

        return coordinator.dispatch(task, context)
                .thenApply(result -> {
                    if (trace != null) {
                        trace.record(stage, "coordinator[" + expectedRole + "]", Map.of("task", task), result);
                    }
                    return result;
                });
    }
}
