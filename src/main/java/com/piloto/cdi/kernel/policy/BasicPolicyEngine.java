package com.piloto.cdi.kernel.policy;

import com.piloto.cdi.kernel.command.Command;
import com.piloto.cdi.kernel.command.CommandType;
import com.piloto.cdi.kernel.model.SnapshotState;
import com.piloto.cdi.kernel.types.GoalID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Basic deterministic policy engine for Phase 3.
 * 
 * <p>Implements explicit, traceable business rules without LLM or heuristics.
 * All decisions are based on structural validation and simple state checks.</p>
 * 
 * <p><b>Implemented Rules:</b></p>
 * <ol>
 *   <li><b>Tenant Isolation</b>: Command and snapshot must belong to same tenant</li>
 *   <li><b>Version Validity</b>: Snapshot must have valid (non-null) version</li>
 *   <li><b>Payload Presence</b>: Command payload must not be empty for state-changing operations</li>
 *   <li><b>Command Type Recognition</b>: Command type must be recognized (enum value)</li>
 *   <li><b>Goal Existence for Updates</b>: UPDATE_GOAL/COMPLETE_GOAL require existing goal</li>
 * </ol>
 * 
 * <p><b>Stateless Design:</b></p>
 * <ul>
 *   <li>No instance state - safe for concurrent use</li>
 *   <li>No side effects - pure function evaluation</li>
 *   <li>Deterministic - same inputs → same output</li>
 * </ul>
 * 
 * @since Phase 3
 */
public class BasicPolicyEngine implements PolicyEngine {
    
    @Override
    public PolicyDecision evaluate(SnapshotState snapshot, Command command) {
        Objects.requireNonNull(snapshot, "snapshot cannot be null");
        Objects.requireNonNull(command, "command cannot be null");
        
        List<String> violations = new ArrayList<>();
        
        // Rule 1: Tenant Isolation
        if (!snapshot.getTenantId().equals(command.getTenantId())) {
            violations.add(String.format(
                "Tenant mismatch: snapshot has %s but command has %s",
                snapshot.getTenantId().getValue(),
                command.getTenantId().getValue()
            ));
        }
        
        // Rule 2: Version Validity
        if (snapshot.getVersion() == null) {
            violations.add("Snapshot has null version");
        }
        
        // Rule 3: Payload Presence for State-Changing Operations
        if (isStateChangingCommand(command.getCommandType()) && command.getPayload().isEmpty()) {
            violations.add(String.format(
                "Command type %s requires non-empty payload",
                command.getCommandType()
            ));
        }
        
        // Rule 4: Command Type Recognition (already guaranteed by enum, but explicit check)
        if (command.getCommandType() == null) {
            violations.add("Command type is null");
        }
        
        // Rule 5: Goal Existence for Goal Updates
        if (command.getCommandType() == CommandType.UPDATE_GOAL || 
            command.getCommandType() == CommandType.COMPLETE_GOAL) {
            
            String goalIdStr = (String) command.getPayload().get("goalId");
            if (goalIdStr == null) {
                violations.add(String.format(
                    "Command type %s requires 'goalId' in payload",
                    command.getCommandType()
                ));
            } else {
                GoalID goalId = GoalID.of(goalIdStr);
                if (!snapshot.getActiveGoals().contains(goalId)) {
                    violations.add(String.format(
                        "Goal %s not found in active goals",
                        goalIdStr
                    ));
                }
            }
        }
        
        // Return decision
        if (!violations.isEmpty()) {
            return PolicyDecision.reject(violations);
        }
        
        return PolicyDecision.allowWithMetadata(Map.of(
            "appliedRules", List.of(
                "TenantIsolation",
                "VersionValidity",
                "PayloadPresence",
                "CommandTypeRecognition",
                "GoalExistence"
            )
        ));
    }
    
    /**
     * Determines if a command type mutates state (vs read-only or no-op).
     * 
     * @param type command type to check
     * @return true if command changes state
     */
    private boolean isStateChangingCommand(CommandType type) {
        return type == CommandType.CREATE_GOAL ||
               type == CommandType.UPDATE_GOAL ||
               type == CommandType.COMPLETE_GOAL ||
               type == CommandType.UPDATE_STATE;
    }
}
