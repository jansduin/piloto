package com.piloto.cdi.kernel.tools;

import java.util.Map;
import java.util.Objects;

public final class ToolExecutionRequest {
    private final String toolName;
    private final Map<String, Object> payload;
    private final boolean dryRun;
    
    private ToolExecutionRequest(String toolName, Map<String, Object> payload, boolean dryRun) {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must not be empty");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        
        this.toolName = toolName;
        this.payload = Map.copyOf(payload);
        this.dryRun = dryRun;
    }
    
    public static ToolExecutionRequest create(String toolName, Map<String, Object> payload, boolean dryRun) {
        return new ToolExecutionRequest(toolName, payload, dryRun);
    }
    
    public static ToolExecutionRequest create(String toolName, Map<String, Object> payload) {
        return new ToolExecutionRequest(toolName, payload, false);
    }
    
    public String getToolName() {
        return toolName;
    }
    
    public Map<String, Object> getPayload() {
        return payload;
    }
    
    public boolean isDryRun() {
        return dryRun;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ToolExecutionRequest)) return false;
        ToolExecutionRequest that = (ToolExecutionRequest) o;
        return dryRun == that.dryRun &&
                toolName.equals(that.toolName) &&
                payload.equals(that.payload);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(toolName, payload, dryRun);
    }
    
    @Override
    public String toString() {
        return "ToolExecutionRequest{" +
                "toolName='" + toolName + '\'' +
                ", dryRun=" + dryRun +
                ", payload=" + payload +
                '}';
    }
}
