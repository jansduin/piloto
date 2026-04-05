package com.piloto.cdi.kernel.orchestrator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SelfEvaluator {
    
    public CompletableFuture<Map<String, Object>> evaluate(String goal, Map<String, Object> results) {
        if (goal == null || goal.isBlank()) {
            return CompletableFuture.completedFuture(createEvaluationResult(0.0, 
                List.of("Goal is empty"), 
                List.of("Provide clear goal")));
        }
        
        if (results == null || results.isEmpty()) {
            return CompletableFuture.completedFuture(createEvaluationResult(0.2, 
                List.of("No results produced"), 
                List.of("Execute tasks to generate results")));
        }
        
        double score = calculateScore(goal, results);
        List<String> issues = identifyIssues(results);
        List<String> improvements = suggestImprovements(results, score);
        
        return CompletableFuture.completedFuture(createEvaluationResult(score, issues, improvements));
    }
    
    private double calculateScore(String goal, Map<String, Object> results) {
        double score = 0.5;
        
        if (results.containsKey("completed_tasks")) {
            Object completedObj = results.get("completed_tasks");
            if (completedObj instanceof List) {
                List<?> completed = (List<?>) completedObj;
                if (!completed.isEmpty()) {
                    score += 0.2;
                }
            }
        }
        
        if (results.containsKey("output") && results.get("output") != null) {
            score += 0.2;
        }
        
        if (results.containsKey("error")) {
            score -= 0.3;
        }
        
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    private List<String> identifyIssues(Map<String, Object> results) {
        List<String> issues = new ArrayList<>();
        
        if (results.containsKey("error")) {
            issues.add("Error encountered: " + results.get("error"));
        }
        
        if (!results.containsKey("output")) {
            issues.add("No output generated");
        }
        
        if (results.containsKey("completed_tasks")) {
            Object completedObj = results.get("completed_tasks");
            if (completedObj instanceof List) {
                List<?> completed = (List<?>) completedObj;
                if (completed.isEmpty()) {
                    issues.add("No tasks completed");
                }
            }
        }
        
        return issues;
    }
    
    private List<String> suggestImprovements(Map<String, Object> results, double score) {
        List<String> improvements = new ArrayList<>();
        
        if (score < 0.5) {
            improvements.add("Review task decomposition strategy");
            improvements.add("Verify agent coordination");
        }
        
        if (score < 0.7) {
            improvements.add("Add more context to tasks");
        }
        
        if (!results.containsKey("validation")) {
            improvements.add("Add result validation step");
        }
        
        return improvements;
    }
    
    private Map<String, Object> createEvaluationResult(double score, List<String> issues, List<String> improvements) {
        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("issues", new ArrayList<>(issues));
        result.put("improvements", new ArrayList<>(improvements));
        return result;
    }
}
