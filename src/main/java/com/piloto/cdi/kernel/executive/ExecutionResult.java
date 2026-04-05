package com.piloto.cdi.kernel.executive;

import com.piloto.cdi.kernel.model.DomainEvent;
import com.piloto.cdi.kernel.model.SnapshotState;
import com.piloto.cdi.kernel.types.VersionID;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of command execution by ExecutiveController.
 * 
 * <p>Immutable value object capturing the complete outcome of command processing,
 * including success/failure, any generated events, resulting state, and error messages.</p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>
 * ExecutionResult result = executiveController.execute(command);
 * if (result.isSuccess()) {
 *     SnapshotState newState = result.getResultingState();
 *     // Process success
 * } else {
 *     log.error("Execution failed: {}", result.getErrors());
 * }
 * </pre>
 * 
 * @since Phase 3
 */
public final class ExecutionResult {
    
    private final boolean success;
    private final List<String> errors;
    private final Optional<DomainEvent> emittedEvent;
    private final SnapshotState resultingState;
    private final VersionID newVersion;
    
    private ExecutionResult(
            boolean success,
            List<String> errors,
            Optional<DomainEvent> emittedEvent,
            SnapshotState resultingState,
            VersionID newVersion) {
        
        this.success = success;
        this.errors = List.copyOf(Objects.requireNonNull(errors, "errors cannot be null"));
        this.emittedEvent = Objects.requireNonNull(emittedEvent, "emittedEvent Optional cannot be null");
        this.resultingState = resultingState; // may be null on failure
        this.newVersion = newVersion; // may be null on failure
    }
    
    /**
     * Creates a successful execution result.
     * 
     * @param emittedEvent domain event that was generated (may be empty for NOOP)
     * @param resultingState final snapshot state after execution
     * @param newVersion new version ID of the state
     * @return ExecutionResult indicating success
     */
    public static ExecutionResult success(
            Optional<DomainEvent> emittedEvent,
            SnapshotState resultingState,
            VersionID newVersion) {
        
        Objects.requireNonNull(resultingState, "resultingState cannot be null for success");
        Objects.requireNonNull(newVersion, "newVersion cannot be null for success");
        
        return new ExecutionResult(true, List.of(), emittedEvent, resultingState, newVersion);
    }
    
    /**
     * Creates a failed execution result.
     * 
     * @param errors list of error messages explaining failure
     * @return ExecutionResult indicating failure
     */
    public static ExecutionResult failure(List<String> errors) {
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("Failure must have at least one error");
        }
        return new ExecutionResult(false, errors, Optional.empty(), null, null);
    }
    
    /**
     * Whether the command executed successfully.
     * 
     * @return true if success, false if failure
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * List of error messages (empty if success).
     * 
     * @return immutable list of errors
     */
    public List<String> getErrors() {
        return errors;
    }
    
    /**
     * Domain event emitted during execution (empty for NOOP or failure).
     * 
     * @return Optional containing event if one was generated
     */
    public Optional<DomainEvent> getEmittedEvent() {
        return emittedEvent;
    }
    
    /**
     * Resulting snapshot state after execution (null on failure).
     * 
     * @return SnapshotState or null
     */
    public SnapshotState getResultingState() {
        return resultingState;
    }
    
    /**
     * New version ID after execution (null on failure).
     * 
     * @return VersionID or null
     */
    public VersionID getNewVersion() {
        return newVersion;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionResult that = (ExecutionResult) o;
        return success == that.success &&
               errors.equals(that.errors) &&
               emittedEvent.equals(that.emittedEvent) &&
               Objects.equals(resultingState, that.resultingState) &&
               Objects.equals(newVersion, that.newVersion);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(success, errors, emittedEvent, resultingState, newVersion);
    }
    
    @Override
    public String toString() {
        return "ExecutionResult{" +
               "success=" + success +
               ", errors=" + errors +
               ", emittedEvent=" + emittedEvent +
               ", resultingState=" + resultingState +
               ", newVersion=" + newVersion +
               '}';
    }
}
