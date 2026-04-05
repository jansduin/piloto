package com.piloto.cdi.kernel.tools;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ToolDefinition {
    private final String name;
    private final String version;
    private final String description;
    private final List<ToolCapability> capabilities;
    private final Map<String, Object> inputSchema;
    private final Map<String, Object> outputSchema;
    private final boolean requiresApproval;
    
    private ToolDefinition(
            String name,
            String version,
            String description,
            List<ToolCapability> capabilities,
            Map<String, Object> inputSchema,
            Map<String, Object> outputSchema,
            boolean requiresApproval) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version must not be empty");
        }
        if (description == null) {
            throw new IllegalArgumentException("description must not be null");
        }
        if (capabilities == null) {
            throw new IllegalArgumentException("capabilities must not be null");
        }
        if (inputSchema == null) {
            throw new IllegalArgumentException("inputSchema must not be null");
        }
        if (outputSchema == null) {
            throw new IllegalArgumentException("outputSchema must not be null");
        }
        
        this.name = name;
        this.version = version;
        this.description = description;
        this.capabilities = List.copyOf(capabilities);
        this.inputSchema = Map.copyOf(inputSchema);
        this.outputSchema = Map.copyOf(outputSchema);
        this.requiresApproval = requiresApproval;
    }
    
    public static ToolDefinition create(
            String name,
            String version,
            String description,
            List<ToolCapability> capabilities,
            Map<String, Object> inputSchema,
            Map<String, Object> outputSchema,
            boolean requiresApproval) {
        return new ToolDefinition(name, version, description, capabilities, inputSchema, outputSchema, requiresApproval);
    }
    
    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<ToolCapability> getCapabilities() {
        return capabilities;
    }
    
    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }
    
    public Map<String, Object> getOutputSchema() {
        return outputSchema;
    }
    
    public boolean requiresApproval() {
        return requiresApproval;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ToolDefinition)) return false;
        ToolDefinition that = (ToolDefinition) o;
        return requiresApproval == that.requiresApproval &&
                name.equals(that.name) &&
                version.equals(that.version) &&
                description.equals(that.description) &&
                capabilities.equals(that.capabilities) &&
                inputSchema.equals(that.inputSchema) &&
                outputSchema.equals(that.outputSchema);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, version, description, capabilities, inputSchema, outputSchema, requiresApproval);
    }
    
    @Override
    public String toString() {
        return "ToolDefinition{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", requiresApproval=" + requiresApproval +
                ", capabilities=" + capabilities +
                '}';
    }
}
