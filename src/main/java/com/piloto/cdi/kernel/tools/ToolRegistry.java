package com.piloto.cdi.kernel.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ToolRegistry {
    private final Map<String, BaseTool> tools = new ConcurrentHashMap<>();
    
    public void register(BaseTool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("tool must not be null");
        }
        
        String name = tool.getDefinition().getName();
        
        BaseTool existing = tools.putIfAbsent(name, tool);
        if (existing != null) {
            throw new IllegalStateException("Tool '" + name + "' is already registered");
        }
    }
    
    public BaseTool get(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must not be empty");
        }
        
        BaseTool tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Tool '" + toolName + "' not found");
        }
        
        return tool;
    }
    
    public List<String> listTools() {
        return new ArrayList<>(tools.keySet());
    }
    
    public void unregister(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must not be empty");
        }
        
        BaseTool removed = tools.remove(toolName);
        if (removed == null) {
            throw new IllegalArgumentException("Tool '" + toolName + "' not found");
        }
    }
    
    public int size() {
        return tools.size();
    }
}
