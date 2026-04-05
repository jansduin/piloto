package com.piloto.cdi.kernel.tools;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ToolExecutionResult {
    private final boolean success;
    private final Map<String, Object> output;
    private final String error;
    private final long executionTimeMs;
    
    private ToolExecutionResult(boolean success, Map<String, Object> output, String error, long executionTimeMs) {
        this.success = success;
        this.output = output != null ? Map.copyOf(output) : Map.of();
        this.error = error;
        this.executionTimeMs = executionTimeMs;
    }
    
    public static ToolExecutionResult success(Map<String, Object> output, long executionTimeMs) {
        if (output == null) {
            throw new IllegalArgumentException("output must not be null");
        }
        return new ToolExecutionResult(true, output, null, executionTimeMs);
    }
    
    public static ToolExecutionResult failure(String error, long executionTimeMs) {
        if (error == null || error.isBlank()) {
            throw new IllegalArgumentException("error must not be empty");
        }
        return new ToolExecutionResult(false, Map.of(), error, executionTimeMs);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Map<String, Object> getOutput() {
        return output;
    }
    
    public Optional<String> getError() {
        return Optional.ofNullable(error);
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ToolExecutionResult)) return false;
        ToolExecutionResult that = (ToolExecutionResult) o;
        return success == that.success &&
                executionTimeMs == that.executionTimeMs &&
                output.equals(that.output) &&
                Objects.equals(error, that.error);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(success, output, error, executionTimeMs);
    }
    
    @Override
    public String toString() {
        return "ToolExecutionResult{" +
                "success=" + success +
                ", executionTimeMs=" + executionTimeMs +
                (error != null ? ", error='" + error + '\'' : ", output=" + output) +
                '}';
    }
}
