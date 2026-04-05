package com.piloto.cdi.kernel.diagnostics;

import com.piloto.cdi.kernel.orchestrator.ExecutionTrace;

import java.util.*;

public class ConsistencyValidator {

    private static final List<String> VALID_STAGE_TRANSITIONS = Arrays.asList(
            "CONTEXT_BUILDING->TASK_DECOMPOSITION",
            "TASK_DECOMPOSITION->EXECUTION",
            "EXECUTION->SELF_EVALUATION",
            "SELF_EVALUATION->FINALIZATION",
            "SELF_EVALUATION->CONTEXT_BUILDING",
            "SELF_EVALUATION->TASK_DECOMPOSITION");

    public Map<String, Object> validate(List<ExecutionTrace.TraceEntry> trace) {
        if (trace == null || trace.isEmpty()) {
            return Map.of("consistent", true, "issues", List.of());
        }

        List<String> issues = new ArrayList<>();

        issues.addAll(detectInvalidStageTransitions(trace));
        issues.addAll(detectGoalChanges(trace));
        issues.addAll(detectOrphanedResults(trace));
        issues.addAll(detectSimpleContradictions(trace));

        return Map.of(
                "consistent", issues.isEmpty(),
                "issues", List.copyOf(issues));
    }

    private List<String> detectInvalidStageTransitions(List<ExecutionTrace.TraceEntry> trace) {
        List<String> issues = new ArrayList<>();

        for (int i = 0; i < trace.size() - 1; i++) {
            String currentStage = extractStage(trace.get(i));
            String nextStage = extractStage(trace.get(i + 1));

            if (currentStage != null && nextStage != null && !currentStage.equals(nextStage)) {
                String transition = currentStage + "->" + nextStage;
                if (!VALID_STAGE_TRANSITIONS.contains(transition)) {
                    issues.add("Invalid stage transition at entry " + i + ": " + transition);
                }
            }
        }

        return issues;
    }

    private List<String> detectGoalChanges(List<ExecutionTrace.TraceEntry> trace) {
        List<String> issues = new ArrayList<>();
        String initialGoal = extractGoal(trace.get(0));

        if (initialGoal == null) {
            issues.add("Missing initial goal in trace");
            return issues;
        }

        for (int i = 1; i < trace.size(); i++) {
            String currentGoal = extractGoal(trace.get(i));
            if (currentGoal != null && !currentGoal.equals(initialGoal)) {
                issues.add("Unexpected goal change at entry " + i + ": '" +
                        initialGoal + "' -> '" + currentGoal + "'");
            }
        }

        return issues;
    }

    private List<String> detectOrphanedResults(List<ExecutionTrace.TraceEntry> trace) {
        List<String> issues = new ArrayList<>();
        Set<String> declaredTasks = new HashSet<>();
        Set<String> executedTasks = new HashSet<>();

        for (ExecutionTrace.TraceEntry entry : trace) {
            String stage = extractStage(entry);

            if ("TASK_DECOMPOSITION".equals(stage)) {
                Map<String, Object> output = entry.getOutputSnapshot();
                Object tasks = output.get("sub_tasks");
                if (tasks instanceof List) {
                    ((List<?>) tasks).forEach(t -> declaredTasks.add(t.toString()));
                }
            }

            if ("EXECUTION".equals(stage)) {
                Map<String, Object> output = entry.getOutputSnapshot();
                Object completedTasks = output.get("completed_tasks");
                if (completedTasks instanceof List) {
                    ((List<?>) completedTasks).forEach(t -> executedTasks.add(t.toString()));
                }
            }
        }

        for (String executed : executedTasks) {
            if (!declaredTasks.contains(executed)) {
                issues.add("Orphaned result: task '" + executed +
                        "' executed but never declared");
            }
        }

        return issues;
    }

    private List<String> detectSimpleContradictions(List<ExecutionTrace.TraceEntry> trace) {
        List<String> issues = new ArrayList<>();
        Map<String, String> factStore = new HashMap<>();

        for (int i = 0; i < trace.size(); i++) {
            ExecutionTrace.TraceEntry entry = trace.get(i);
            Map<String, Object> output = entry.getOutputSnapshot();

            final int entryIndex = i; // Make effectively final for lambda
            final List<String> issuesRef = issues; // Make effectively final for lambda

            output.forEach((key, value) -> {
                String valueStr = String.valueOf(value);

                if (factStore.containsKey(key)) {
                    String previousValue = factStore.get(key);
                    if (!previousValue.equals(valueStr) &&
                            isContradiction(key, previousValue, valueStr)) {
                        issuesRef.add("Contradiction at entry " + entryIndex + ": " +
                                key + " = '" + previousValue + "' vs '" + valueStr + "'");
                    }
                }

                factStore.put(key, valueStr);
            });
        }

        return issues;
    }

    private boolean isContradiction(String key, String value1, String value2) {
        if (key.equals("iteration") || key.equals("stage") ||
                key.contains("timestamp") || key.contains("score")) {
            return false;
        }

        if (value1.length() > 100 || value2.length() > 100) {
            return false;
        }

        return !value1.equals(value2);
    }

    private String extractStage(ExecutionTrace.TraceEntry entry) {
        Map<String, Object> input = entry.getInputSnapshot();
        Object stage = input.get("stage");
        if (stage != null) {
            String stageStr = stage.toString();
            return stageStr.replace("ReasoningStage.", "");
        }
        return null;
    }

    private String extractGoal(ExecutionTrace.TraceEntry entry) {
        Map<String, Object> input = entry.getInputSnapshot();
        Object goal = input.get("current_goal");
        return goal != null ? goal.toString() : null;
    }
}
