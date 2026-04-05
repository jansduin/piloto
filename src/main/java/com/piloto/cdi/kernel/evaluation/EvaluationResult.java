package com.piloto.cdi.kernel.evaluation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of technical validation by EvaluationService.
 * 
 * <p>Immutable value object capturing structural and technical validation outcomes.
 * Separate from PolicyDecision (business rules) - this is purely technical correctness.</p>
 * 
 * <p><b>Distinction from PolicyDecision:</b></p>
 * <ul>
 *   <li><b>EvaluationResult</b>: Technical validation (types, nulls, timestamps, structure)</li>
 *   <li><b>PolicyDecision</b>: Business rules (tenant isolation, goal existence, constraints)</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>
 * EvaluationResult result = evaluationService.validate(command);
 * if (!result.isValid()) {
 *     log.error("Command validation failed: {}", result.getIssues());
 *     return ExecutionResult.failure(result.getIssues());
 * }
 * </pre>
 * 
 * @since Phase 3
 */
public final class EvaluationResult {
    
    private final boolean valid;
    private final List<String> issues;
    private final Map<String, Object> metrics;
    
    private EvaluationResult(boolean valid, List<String> issues, Map<String, Object> metrics) {
        this.valid = valid;
        this.issues = List.copyOf(Objects.requireNonNull(issues, "issues cannot be null"));
        this.metrics = Map.copyOf(Objects.requireNonNull(metrics, "metrics cannot be null"));
    }
    
    /**
     * Creates a valid result with no issues.
     * 
     * @return EvaluationResult indicating success
     */
    public static EvaluationResult valid() {
        return new EvaluationResult(true, List.of(), Map.of());
    }
    
    /**
     * Creates a valid result with validation metrics.
     * 
     * @param metrics contextual data (e.g., validation time, checks performed)
     * @return EvaluationResult indicating success with metrics
     */
    public static EvaluationResult validWithMetrics(Map<String, Object> metrics) {
        return new EvaluationResult(true, List.of(), metrics);
    }
    
    /**
     * Creates an invalid result with issue descriptions.
     * 
     * @param issues list of validation failure reasons
     * @return EvaluationResult indicating failure
     */
    public static EvaluationResult invalid(List<String> issues) {
        if (issues.isEmpty()) {
            throw new IllegalArgumentException("Invalid result must have at least one issue");
        }
        return new EvaluationResult(false, issues, Map.of());
    }
    
    /**
     * Creates an invalid result with issues and metrics.
     * 
     * @param issues list of validation failure reasons
     * @param metrics contextual data
     * @return EvaluationResult indicating failure with metrics
     */
    public static EvaluationResult invalidWithMetrics(List<String> issues, Map<String, Object> metrics) {
        if (issues.isEmpty()) {
            throw new IllegalArgumentException("Invalid result must have at least one issue");
        }
        return new EvaluationResult(false, issues, metrics);
    }
    
    /**
     * Whether the command passed technical validation.
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * List of validation issues (empty if valid).
     * 
     * @return immutable list of issue descriptions
     */
    public List<String> getIssues() {
        return issues;
    }
    
    /**
     * Validation metrics and contextual data.
     * 
     * <p>May contain:</p>
     * <ul>
     *   <li>"checksPerformed" - List of validation check names</li>
     *   <li>"validationTimeMs" - Time spent validating</li>
     *   <li>"payloadSize" - Number of payload entries</li>
     * </ul>
     * 
     * @return immutable metrics map
     */
    public Map<String, Object> getMetrics() {
        return metrics;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvaluationResult that = (EvaluationResult) o;
        return valid == that.valid &&
               issues.equals(that.issues) &&
               metrics.equals(that.metrics);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(valid, issues, metrics);
    }
    
    @Override
    public String toString() {
        return "EvaluationResult{" +
               "valid=" + valid +
               ", issues=" + issues +
               ", metrics=" + metrics +
               '}';
    }
}
