package com.piloto.cdi.kernel.orchestrator;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class BaseAgent {
    private final String name;
    private final String role;
    private final ModelProvider modelProvider;
    
    protected BaseAgent(String name, String role, ModelProvider modelProvider) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role must not be empty");
        }
        if (modelProvider == null) {
            throw new IllegalArgumentException("modelProvider must not be null");
        }
        
        this.name = name;
        this.role = role;
        this.modelProvider = modelProvider;
    }
    
    public String getName() {
        return name;
    }
    
    public String getRole() {
        return role;
    }
    
    protected ModelProvider getModelProvider() {
        return modelProvider;
    }
    
    public abstract CompletableFuture<Map<String, Object>> think(Map<String, Object> context);
}
