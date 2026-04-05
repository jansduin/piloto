package com.piloto.cdi.kernel.orchestrator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReasoningState {
    private final ReasoningStage stage;
    private final String currentGoal;
    private final List<String> subTasks;
    private final List<String> completedTasks;
    private final Map<String, Object> intermediateResults;
    private int iteration;
    
    private ReasoningState(
            ReasoningStage stage,
            String currentGoal,
            List<String> subTasks,
            List<String> completedTasks,
            Map<String, Object> intermediateResults,
            int iteration) {
        
        if (currentGoal == null || currentGoal.isBlank()) {
            throw new IllegalArgumentException("currentGoal must not be empty");
        }
        if (iteration < 0) {
            throw new IllegalArgumentException("iteration must be >= 0");
        }
        
        this.stage = stage != null ? stage : ReasoningStage.CONTEXT_BUILDING;
        this.currentGoal = currentGoal;
        this.subTasks = new ArrayList<>(subTasks != null ? subTasks : List.of());
        this.completedTasks = new ArrayList<>(completedTasks != null ? completedTasks : List.of());
        this.intermediateResults = new HashMap<>(intermediateResults != null ? intermediateResults : Map.of());
        this.iteration = iteration;
    }
    
    public static ReasoningState create(String currentGoal) {
        return new ReasoningState(
            ReasoningStage.CONTEXT_BUILDING,
            currentGoal,
            List.of(),
            List.of(),
            Map.of(),
            0
        );
    }
    
    public ReasoningState transitionTo(ReasoningStage newStage) {
        return new ReasoningState(
            newStage,
            this.currentGoal,
            this.subTasks,
            this.completedTasks,
            this.intermediateResults,
            this.iteration
        );
    }
    
    public ReasoningState withSubTasks(List<String> subTasks) {
        return new ReasoningState(
            this.stage,
            this.currentGoal,
            subTasks,
            this.completedTasks,
            this.intermediateResults,
            this.iteration
        );
    }
    
    public ReasoningState markTaskCompleted(String task) {
        List<String> updated = new ArrayList<>(this.completedTasks);
        updated.add(task);
        return new ReasoningState(
            this.stage,
            this.currentGoal,
            this.subTasks,
            updated,
            this.intermediateResults,
            this.iteration
        );
    }
    
    public ReasoningState addIntermediateResult(String key, Object value) {
        Map<String, Object> updated = new HashMap<>(this.intermediateResults);
        updated.put(key, value);
        return new ReasoningState(
            this.stage,
            this.currentGoal,
            this.subTasks,
            this.completedTasks,
            updated,
            this.iteration
        );
    }
    
    public ReasoningState incrementIteration() {
        return new ReasoningState(
            this.stage,
            this.currentGoal,
            this.subTasks,
            this.completedTasks,
            this.intermediateResults,
            this.iteration + 1
        );
    }
    
    public ReasoningStage getStage() {
        return stage;
    }
    
    public String getCurrentGoal() {
        return currentGoal;
    }
    
    public List<String> getSubTasks() {
        return new ArrayList<>(subTasks);
    }
    
    public List<String> getCompletedTasks() {
        return new ArrayList<>(completedTasks);
    }
    
    public Map<String, Object> getIntermediateResults() {
        return new HashMap<>(intermediateResults);
    }
    
    public int getIteration() {
        return iteration;
    }
    
    @Override
    public String toString() {
        return "ReasoningState{" +
                "stage=" + stage +
                ", goal='" + currentGoal + '\'' +
                ", iteration=" + iteration +
                ", subTasks=" + subTasks.size() +
                ", completed=" + completedTasks.size() +
                '}';
    }
}
