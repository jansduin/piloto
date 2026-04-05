package com.piloto.cdi.kernel.tools;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SandboxPolicy {
    private final Set<ToolCapability> allowedCapabilities;
    private final long maxExecutionTimeMs;
    
    public SandboxPolicy(List<ToolCapability> allowedCapabilities, long maxExecutionTimeMs) {
        if (allowedCapabilities == null) {
            throw new IllegalArgumentException("allowedCapabilities must not be null");
        }
        if (maxExecutionTimeMs <= 0) {
            throw new IllegalArgumentException("maxExecutionTimeMs must be positive");
        }
        
        this.allowedCapabilities = Set.copyOf(allowedCapabilities);
        this.maxExecutionTimeMs = maxExecutionTimeMs;
    }
    
    public void validate(BaseTool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("tool must not be null");
        }
        
        ToolDefinition def = tool.getDefinition();
        List<ToolCapability> required = def.getCapabilities();
        
        for (ToolCapability capability : required) {
            if (!allowedCapabilities.contains(capability)) {
                throw new SecurityException(
                    "Tool '" + def.getName() + "' requires capability " + capability + 
                    " which is not allowed by this sandbox policy"
                );
            }
        }
    }
    
    public <T> CompletableFuture<T> enforceTimeout(CompletableFuture<T> future) {
        if (future == null) {
            throw new IllegalArgumentException("future must not be null");
        }
        
        CompletableFuture<T> timeout = new CompletableFuture<>();
        
        future.whenComplete((result, error) -> {
            if (error != null) {
                timeout.completeExceptionally(error);
            } else {
                timeout.complete(result);
            }
        });
        
        CompletableFuture.delayedExecutor(maxExecutionTimeMs, TimeUnit.MILLISECONDS)
            .execute(() -> {
                if (!timeout.isDone()) {
                    timeout.completeExceptionally(
                        new TimeoutException("Execution exceeded timeout of " + maxExecutionTimeMs + "ms")
                    );
                }
            });
        
        return timeout;
    }
    
    public long getMaxExecutionTimeMs() {
        return maxExecutionTimeMs;
    }
    
    public Set<ToolCapability> getAllowedCapabilities() {
        return allowedCapabilities;
    }
}
