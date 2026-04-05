package com.piloto.cdi.kernel.orchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskDecomposer {
    
    private static final Pattern SENTENCE_PATTERN = Pattern.compile(
        "(?<![A-Z]\\w)\\.(?!\\d)|[!?;]"
    );
    
    public CompletableFuture<List<String>> decompose(String goal) {
        if (goal == null || goal.isBlank()) {
            return CompletableFuture.completedFuture(List.of());
        }
        
        List<String> subTasks = new ArrayList<>();
        
        String[] sentences = SENTENCE_PATTERN.split(goal);
        
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (!trimmed.isEmpty() && isValidTask(trimmed)) {
                subTasks.add("Process: " + trimmed);
            }
        }
        
        if (subTasks.isEmpty()) {
            subTasks.add("Analyze goal: " + goal);
            subTasks.add("Execute goal: " + goal);
            subTasks.add("Validate result");
        }
        
        return CompletableFuture.completedFuture(subTasks);
    }
    
    private boolean isValidTask(String task) {
        if (task.length() < 3) {
            return false;
        }
        
        String lower = task.toLowerCase();
        if (lower.matches("^(dr|mr|mrs|ms|st|ave|etc|e\\.g|i\\.e)\\.?$")) {
            return false;
        }
        
        return true;
    }
}
