package com.piloto.cdi.kernel.command;

import com.piloto.cdi.kernel.types.TenantID;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Base interface for all commands entering the CDI system.
 * 
 * <p>Commands represent imperative requests from external actors (humans, agents, systems)
 * that must be validated, evaluated against policies, and potentially translated into
 * domain events that mutate system state.</p>
 * 
 * <p><b>Design Principles:</b></p>
 * <ul>
 *   <li>Commands are immutable value objects</li>
 *   <li>Commands carry intent, not state changes directly</li>
 *   <li>Commands must be validated before execution</li>
 *   <li>Commands are causally linked via correlationId and causalReference</li>
 * </ul>
 * 
 * <p><b>Lifecycle:</b></p>
 * <pre>
 * Command → EvaluationService (technical validation)
 *         → PolicyEngine (business rules)
 *         → ExecutiveController (translate to DomainEvent)
 *         → EventApplicationService (apply to state)
 * </pre>
 * 
 * @since Phase 3
 */
public interface Command {
    
    /**
     * Unique identifier for this command instance.
     * 
     * @return non-null CommandID
     */
    CommandID getCommandId();
    
    /**
     * Tenant context for multi-tenancy isolation.
     * 
     * @return non-null TenantID
     */
    TenantID getTenantId();
    
    /**
     * Timestamp when the command was issued.
     * 
     * @return non-null Instant
     */
    Instant getTimestamp();
    
    /**
     * Classification of the command operation.
     * 
     * @return non-null CommandType
     */
    CommandType getCommandType();
    
    /**
     * Command-specific data as key-value pairs.
     * 
     * <p>Payload structure is validated by EvaluationService based on CommandType.
     * Common keys:</p>
     * <ul>
     *   <li>"goalId" - GoalID as string</li>
     *   <li>"description" - Goal description</li>
     *   <li>"key" - State property key</li>
     *   <li>"value" - State property value</li>
     * </ul>
     * 
     * @return immutable non-null Map (may be empty)
     */
    Map<String, Object> getPayload();
    
    /**
     * Correlation identifier linking related commands across a workflow.
     * 
     * <p>Used for tracing multi-step operations (e.g., goal decomposition).</p>
     * 
     * @return Optional correlation ID
     */
    Optional<String> getCorrelationId();
    
    /**
     * Reference to another command that causally precedes this one.
     * 
     * <p>Enables causal chain reconstruction for debugging and audit.</p>
     * 
     * @return Optional causal CommandID
     */
    Optional<CommandID> getCausalReference();
}
