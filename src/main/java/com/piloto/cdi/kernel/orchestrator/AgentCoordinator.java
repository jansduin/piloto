package com.piloto.cdi.kernel.orchestrator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AgentCoordinator {
    private final List<BaseAgent> agents;
    
    public AgentCoordinator(List<BaseAgent> agents) {
        if (agents == null) {
            throw new IllegalArgumentException("agents must not be null");
        }
        this.agents = List.copyOf(agents);
    }
    
    public CompletableFuture<Map<String, Object>> dispatch(String task, Map<String, Object> context) {
        if (task == null || task.isBlank()) {
            return CompletableFuture.completedFuture(createErrorResult("Task is empty"));
        }
        
        if (context == null) {
            context = Map.of();
        }
        
        if (agents.isEmpty()) {
            return CompletableFuture.completedFuture(createErrorResult("No agents available"));
        }
        
        BaseAgent selectedAgent = selectAgent(task, context);
        
        if (selectedAgent == null) {
            return CompletableFuture.completedFuture(createErrorResult("No suitable agent found for task"));
        }
        
        Map<String, Object> enrichedContext = new HashMap<>(context);
        enrichedContext.put("task", task);
        enrichedContext.put("agent", selectedAgent.getName());
        
        return selectedAgent.think(enrichedContext)
            .exceptionally(error -> createErrorResult("Agent execution failed: " + error.getMessage()));
    }
    
    private BaseAgent selectAgent(String task, Map<String, Object> context) {
        if (context.containsKey("preferred_role")) {
            String preferredRole = context.get("preferred_role").toString();
            for (BaseAgent agent : agents) {
                if (agent.getRole().equalsIgnoreCase(preferredRole)) {
                    return agent;
                }
            }
        }
        
        String lowerTask = task.toLowerCase();
        for (BaseAgent agent : agents) {
            String role = agent.getRole().toLowerCase();
            if (lowerTask.contains(role) || role.contains("general")) {
                return agent;
            }
        }
        
        return agents.get(0);
    }
    
    public List<BaseAgent> getAgents() {
        return agents;
    }
    
    private Map<String, Object> createErrorResult(String errorMessage) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", errorMessage);
        return result;
    }
}
