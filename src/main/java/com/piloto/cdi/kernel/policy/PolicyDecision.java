package com.piloto.cdi.kernel.policy;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of policy evaluation by PolicyEngine.
 * 
 * <p>Immutable value object encapsulating the decision, violations, and contextual metadata
 * from policy rule execution.</p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>
 * PolicyDecision decision = policyEngine.evaluate(snapshot, command);
 * if (decision.getDecisionType() == DecisionType.REJECT) {
 *     log.warn("Command rejected: {}", decision.getViolations());
 *     return;
 * }
 * // Proceed with execution
 * </pre>
 * 
 * @since Phase 3
 */
public final class PolicyDecision {
    
    private final boolean allowed;
    private final List<String> violations;
    private final Map<String, Object> metadata;
    private final DecisionType decisionType;
    
    private PolicyDecision(
            boolean allowed,
            List<String> violations,
            Map<String, Object> metadata,
            DecisionType decisionType) {
        
        this.allowed = allowed;
        this.violations = List.copyOf(Objects.requireNonNull(violations, "violations cannot be null"));
        this.metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata cannot be null"));
        this.decisionType = Objects.requireNonNull(decisionType, "decisionType cannot be null");
    }
    
    /**
     * Creates an ALLOW decision.
     * 
     * @return PolicyDecision with ALLOW type
     */
    public static PolicyDecision allow() {
        return new PolicyDecision(true, List.of(), Map.of(), DecisionType.ALLOW);
    }
    
    /**
     * Creates an ALLOW decision with contextual metadata.
     * 
     * @param metadata additional context (e.g., applied rule names)
     * @return PolicyDecision with ALLOW type
     */
    public static PolicyDecision allowWithMetadata(Map<String, Object> metadata) {
        return new PolicyDecision(true, List.of(), metadata, DecisionType.ALLOW);
    }
    
    /**
     * Creates a REJECT decision with violation reasons.
     * 
     * @param violations list of policy violation descriptions
     * @return PolicyDecision with REJECT type
     */
    public static PolicyDecision reject(List<String> violations) {
        if (violations.isEmpty()) {
            throw new IllegalArgumentException("REJECT decision must have at least one violation");
        }
        return new PolicyDecision(false, violations, Map.of(), DecisionType.REJECT);
    }
    
    /**
     * Creates a REJECT decision with violations and metadata.
     * 
     * @param violations list of policy violation descriptions
     * @param metadata additional context (e.g., violated rule IDs)
     * @return PolicyDecision with REJECT type
     */
    public static PolicyDecision rejectWithMetadata(List<String> violations, Map<String, Object> metadata) {
        if (violations.isEmpty()) {
            throw new IllegalArgumentException("REJECT decision must have at least one violation");
        }
        return new PolicyDecision(false, violations, metadata, DecisionType.REJECT);
    }
    
    /**
     * Creates a NOOP decision (command valid but produces no change).
     * 
     * @param reason explanation for no-op
     * @return PolicyDecision with NOOP type
     */
    public static PolicyDecision noop(String reason) {
        return new PolicyDecision(
            true,
            List.of(),
            Map.of("reason", Objects.requireNonNull(reason, "NOOP reason cannot be null")),
            DecisionType.NOOP
        );
    }
    
    /**
     * Whether the command is allowed to execute.
     * 
     * @return true for ALLOW/NOOP, false for REJECT
     */
    public boolean isAllowed() {
        return allowed;
    }
    
    /**
     * List of policy violation reasons (empty for ALLOW/NOOP).
     * 
     * @return immutable list of violation strings
     */
    public List<String> getViolations() {
        return violations;
    }
    
    /**
     * Contextual metadata from policy evaluation.
     * 
     * <p>May contain:</p>
     * <ul>
     *   <li>"appliedRules" - List of rule IDs that were evaluated</li>
     *   <li>"reason" - Explanation for NOOP decision</li>
     *   <li>"violatedRules" - List of rule IDs that failed</li>
     * </ul>
     * 
     * @return immutable metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Classification of the decision outcome.
     * 
     * @return ALLOW, REJECT, or NOOP
     */
    public DecisionType getDecisionType() {
        return decisionType;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolicyDecision that = (PolicyDecision) o;
        return allowed == that.allowed &&
               violations.equals(that.violations) &&
               metadata.equals(that.metadata) &&
               decisionType == that.decisionType;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(allowed, violations, metadata, decisionType);
    }
    
    @Override
    public String toString() {
        return "PolicyDecision{" +
               "allowed=" + allowed +
               ", violations=" + violations +
               ", metadata=" + metadata +
               ", decisionType=" + decisionType +
               '}';
    }
}
