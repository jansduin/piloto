package com.piloto.cdi.kernel.tools;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class BaseTool {
    private final ToolDefinition definition;

    protected BaseTool(ToolDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null");
        }
        this.definition = definition;
    }

    public ToolDefinition getDefinition() {
        return definition;
    }

    /**
     * Compensates (undoes) a previous execution of this tool.
     * By default, it returns a completed future.
     */
    public CompletableFuture<Void> compensate(Map<String, Object> payload) {
        return CompletableFuture.completedFuture(null);
    }

    public abstract CompletableFuture<Map<String, Object>> execute(Map<String, Object> payload);
}
