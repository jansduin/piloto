package com.piloto.cdi.kernel.evaluation;

import com.piloto.cdi.kernel.command.Command;
import com.piloto.cdi.kernel.command.CommandType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service for technical validation of commands.
 * 
 * <p>Performs structural and type-correctness checks without business logic.
 * Complements PolicyEngine which handles business rules.</p>
 * 
 * <p><b>Validation Checks Performed:</b></p>
 * <ol>
 *   <li><b>Structural completeness</b>: All required fields present and non-null</li>
 *   <li><b>Type correctness</b>: Payload values match expected types for command type</li>
 *   <li><b>Timestamp validity</b>: Timestamp not in future (with 1-minute tolerance)</li>
 *   <li><b>ID non-emptiness</b>: CommandID, TenantID have non-empty values</li>
 *   <li><b>Payload schema</b>: Required keys present for each CommandType</li>
 * </ol>
 * 
 * <p><b>Stateless Design:</b></p>
 * <ul>
 *   <li>No instance state - safe for concurrent use</li>
 *   <li>No side effects - pure function validation</li>
 *   <li>Deterministic - same input → same result</li>
 * </ul>
 * 
 * <p><b>Not Validated Here:</b></p>
 * <ul>
 *   <li>Business rules (handled by PolicyEngine)</li>
 *   <li>State consistency (handled by StateTransitionController)</li>
 *   <li>Goal existence (handled by PolicyEngine)</li>
 * </ul>
 * 
 * @since Phase 3
 */
public class EvaluationService {
    
    // Timestamp tolerance: allow commands up to 1 minute in future (clock skew)
    private static final long TIMESTAMP_TOLERANCE_MS = 60_000;
    
    /**
     * Validates a command for technical correctness.
     * 
     * @param command command to validate (non-null)
     * @return EvaluationResult indicating valid/invalid with issues
     * @throws NullPointerException if command is null
     */
    public EvaluationResult validate(Command command) {
        Objects.requireNonNull(command, "command cannot be null");
        
        List<String> issues = new ArrayList<>();
        
        // Check 1: Structural Completeness
        if (command.getCommandId() == null) {
            issues.add("CommandID is null");
        }
        if (command.getTenantId() == null) {
            issues.add("TenantID is null");
        }
        if (command.getTimestamp() == null) {
            issues.add("Timestamp is null");
        }
        if (command.getCommandType() == null) {
            issues.add("CommandType is null");
        }
        if (command.getPayload() == null) {
            issues.add("Payload is null");
        }
        
        // If basic structure invalid, stop here
        if (!issues.isEmpty()) {
            return EvaluationResult.invalid(issues);
        }
        
        // Check 2: ID Non-Emptiness
        if (command.getCommandId().getValue() == null || 
            command.getCommandId().getValue().trim().isEmpty()) {
            issues.add("CommandID value is empty");
        }
        if (command.getTenantId().getValue() == null || 
            command.getTenantId().getValue().trim().isEmpty()) {
            issues.add("TenantID value is empty");
        }
        
        // Check 3: Timestamp Validity
        Instant now = Instant.now();
        Instant commandTimestamp = command.getTimestamp();
        if (commandTimestamp.isAfter(now.plusMillis(TIMESTAMP_TOLERANCE_MS))) {
            issues.add(String.format(
                "Timestamp is in future: %s (current: %s, tolerance: %dms)",
                commandTimestamp,
                now,
                TIMESTAMP_TOLERANCE_MS
            ));
        }
        
        // Check 4: Payload Schema Validation
        validatePayloadSchema(command.getCommandType(), command.getPayload(), issues);
        
        // Return result
        if (!issues.isEmpty()) {
            return EvaluationResult.invalidWithMetrics(issues, Map.of(
                "checksPerformed", List.of(
                    "StructuralCompleteness",
                    "IDNonEmptiness",
                    "TimestampValidity",
                    "PayloadSchema"
                )
            ));
        }
        
        return EvaluationResult.validWithMetrics(Map.of(
            "checksPerformed", List.of(
                "StructuralCompleteness",
                "IDNonEmptiness",
                "TimestampValidity",
                "PayloadSchema"
            ),
            "payloadSize", command.getPayload().size()
        ));
    }
    
    /**
     * Validates payload contains required keys for the command type.
     * 
     * @param commandType type of command
     * @param payload command payload
     * @param issues list to append validation errors to
     */
    private void validatePayloadSchema(
            CommandType commandType,
            Map<String, Object> payload,
            List<String> issues) {
        
        switch (commandType) {
            case CREATE_GOAL:
                if (!payload.containsKey("description")) {
                    issues.add("CREATE_GOAL requires 'description' in payload");
                }
                if (payload.containsKey("description") && 
                    !(payload.get("description") instanceof String)) {
                    issues.add("CREATE_GOAL 'description' must be a String");
                }
                // Critical: goalId is required for StateTransitionController to add goal to activeGoals
                if (!payload.containsKey("goalId")) {
                    issues.add("CREATE_GOAL requires 'goalId' in payload");
                }
                if (payload.containsKey("goalId") && 
                    !(payload.get("goalId") instanceof String)) {
                    issues.add("CREATE_GOAL 'goalId' must be a String");
                }
                break;
                
            case UPDATE_GOAL:
                if (!payload.containsKey("goalId")) {
                    issues.add("UPDATE_GOAL requires 'goalId' in payload");
                }
                if (payload.containsKey("goalId") && 
                    !(payload.get("goalId") instanceof String)) {
                    issues.add("UPDATE_GOAL 'goalId' must be a String");
                }
                break;
                
            case COMPLETE_GOAL:
                if (!payload.containsKey("goalId")) {
                    issues.add("COMPLETE_GOAL requires 'goalId' in payload");
                }
                if (payload.containsKey("goalId") && 
                    !(payload.get("goalId") instanceof String)) {
                    issues.add("COMPLETE_GOAL 'goalId' must be a String");
                }
                break;
                
            case UPDATE_STATE:
                if (!payload.containsKey("key")) {
                    issues.add("UPDATE_STATE requires 'key' in payload");
                }
                if (!payload.containsKey("value")) {
                    issues.add("UPDATE_STATE requires 'value' in payload");
                }
                if (payload.containsKey("key") && 
                    !(payload.get("key") instanceof String)) {
                    issues.add("UPDATE_STATE 'key' must be a String");
                }
                break;
                
            case SYSTEM_COMMAND:
                // SYSTEM_COMMAND has flexible schema
                break;
                
            default:
                issues.add("Unknown CommandType: " + commandType);
        }
    }
}
