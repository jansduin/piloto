package com.piloto.cdi.kernel.command;

import com.piloto.cdi.kernel.types.TenantID;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable base implementation of Command interface.
 * 
 * <p>This class provides defensive copying and immutability guarantees required
 * for enterprise-grade command handling. All collections are deeply immutable.</p>
 * 
 * <p><b>Immutability Strategy:</b></p>
 * <ul>
 *   <li>Constructor uses Map.copyOf() for defensive copy of payload structure</li>
 *   <li>All fields are final</li>
 *   <li>No setters exposed</li>
 *   <li>Getters return immutable views</li>
 * </ul>
 * 
 * <p><b>⚠️ CRITICAL IMMUTABILITY REQUIREMENT:</b></p>
 * <p>Payload values MUST be immutable types. Supported types:</p>
 * <ul>
 *   <li>Primitives and their wrappers (Integer, Long, Boolean, etc.)</li>
 *   <li>String</li>
 *   <li>Immutable value objects (e.g., LocalDate, UUID)</li>
 * </ul>
 * 
 * <p><b>⚠️ DO NOT USE mutable types as payload values:</b></p>
 * <ul>
 *   <li>ArrayList, HashMap, or other mutable collections</li>
 *   <li>Custom mutable objects (POJOs with setters)</li>
 *   <li>Arrays (use immutable collections instead)</li>
 * </ul>
 * 
 * <p>Rationale: Map.copyOf() performs shallow copy. If values are mutable,
 * external code can mutate command state after construction, violating
 * immutability guarantees and Phase 1 requirements.</p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>
 * // ✅ CORRECT - immutable values
 * Command cmd = BaseCommand.create(
 *     TenantID.of("tenant-123"),
 *     CommandType.CREATE_GOAL,
 *     Map.of("description", "Implement feature X", "autonomyLevel", 2)
 * );
 * 
 * // ❌ INCORRECT - mutable value (List)
 * Command badCmd = BaseCommand.create(
 *     TenantID.of("tenant-123"),
 *     CommandType.CREATE_GOAL,
 *     Map.of("tags", new ArrayList&lt;&gt;(List.of("tag1", "tag2"))) // MUTABLE!
 * );
 * </pre>
 * 
 * @since Phase 3
 */
public final class BaseCommand implements Command {
    
    private final CommandID commandId;
    private final TenantID tenantId;
    private final Instant timestamp;
    private final CommandType commandType;
    private final Map<String, Object> payload;
    private final Optional<String> correlationId;
    private final Optional<CommandID> causalReference;
    
    private BaseCommand(
            CommandID commandId,
            TenantID tenantId,
            Instant timestamp,
            CommandType commandType,
            Map<String, Object> payload,
            Optional<String> correlationId,
            Optional<CommandID> causalReference) {
        
        this.commandId = Objects.requireNonNull(commandId, "commandId cannot be null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
        this.commandType = Objects.requireNonNull(commandType, "commandType cannot be null");
        
        // Defensive copy with Map.copyOf() (Phase 1 immutability requirement)
        this.payload = Map.copyOf(Objects.requireNonNull(payload, "payload cannot be null"));
        
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId Optional cannot be null");
        this.causalReference = Objects.requireNonNull(causalReference, "causalReference Optional cannot be null");
    }
    
    /**
     * Creates a command with auto-generated ID and current timestamp.
     * 
     * @param tenantId tenant context
     * @param commandType operation type
     * @param payload command data
     * @return new BaseCommand instance
     */
    public static BaseCommand create(
            TenantID tenantId,
            CommandType commandType,
            Map<String, Object> payload) {
        return new BaseCommand(
            CommandID.generate(),
            tenantId,
            Instant.now(),
            commandType,
            payload,
            Optional.empty(),
            Optional.empty()
        );
    }
    
    /**
     * Creates a command with explicit causal linking.
     * 
     * @param tenantId tenant context
     * @param commandType operation type
     * @param payload command data
     * @param correlationId workflow correlation ID
     * @param causalReference previous command in causal chain
     * @return new BaseCommand instance
     */
    public static BaseCommand createWithCausality(
            TenantID tenantId,
            CommandType commandType,
            Map<String, Object> payload,
            String correlationId,
            CommandID causalReference) {
        return new BaseCommand(
            CommandID.generate(),
            tenantId,
            Instant.now(),
            commandType,
            payload,
            Optional.ofNullable(correlationId),
            Optional.ofNullable(causalReference)
        );
    }
    
    /**
     * Creates a command with full control over all fields (for testing).
     * 
     * @param commandId explicit command ID
     * @param tenantId tenant context
     * @param timestamp command timestamp
     * @param commandType operation type
     * @param payload command data
     * @param correlationId workflow correlation ID
     * @param causalReference previous command in causal chain
     * @return new BaseCommand instance
     */
    public static BaseCommand createFull(
            CommandID commandId,
            TenantID tenantId,
            Instant timestamp,
            CommandType commandType,
            Map<String, Object> payload,
            Optional<String> correlationId,
            Optional<CommandID> causalReference) {
        return new BaseCommand(
            commandId,
            tenantId,
            timestamp,
            commandType,
            payload,
            correlationId,
            causalReference
        );
    }
    
    @Override
    public CommandID getCommandId() {
        return commandId;
    }
    
    @Override
    public TenantID getTenantId() {
        return tenantId;
    }
    
    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public CommandType getCommandType() {
        return commandType;
    }
    
    @Override
    public Map<String, Object> getPayload() {
        // Already immutable via Map.copyOf() in constructor
        return payload;
    }
    
    @Override
    public Optional<String> getCorrelationId() {
        return correlationId;
    }
    
    @Override
    public Optional<CommandID> getCausalReference() {
        return causalReference;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseCommand that = (BaseCommand) o;
        return commandId.equals(that.commandId) &&
               tenantId.equals(that.tenantId) &&
               timestamp.equals(that.timestamp) &&
               commandType == that.commandType &&
               payload.equals(that.payload) &&
               correlationId.equals(that.correlationId) &&
               causalReference.equals(that.causalReference);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(commandId, tenantId, timestamp, commandType, 
                           payload, correlationId, causalReference);
    }
    
    @Override
    public String toString() {
        return "BaseCommand{" +
               "commandId=" + commandId +
               ", tenantId=" + tenantId +
               ", timestamp=" + timestamp +
               ", commandType=" + commandType +
               ", payload=" + payload +
               ", correlationId=" + correlationId +
               ", causalReference=" + causalReference +
               '}';
    }
}
