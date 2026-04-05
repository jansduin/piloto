package com.piloto.cdi.kernel.executive;

import com.piloto.cdi.kernel.command.Command;
import com.piloto.cdi.kernel.command.CommandType;
import com.piloto.cdi.kernel.evaluation.EvaluationResult;
import com.piloto.cdi.kernel.evaluation.EvaluationService;
import com.piloto.cdi.kernel.interfaces.ISnapshotStore;
import com.piloto.cdi.kernel.model.DomainEvent;
import com.piloto.cdi.kernel.model.SnapshotState;
import com.piloto.cdi.kernel.policy.DecisionType;
import com.piloto.cdi.kernel.policy.PolicyDecision;
import com.piloto.cdi.kernel.policy.PolicyEngine;
import com.piloto.cdi.kernel.service.EventApplicationService;
import com.piloto.cdi.kernel.types.ActorType;
import com.piloto.cdi.kernel.types.EventType;
import com.piloto.cdi.kernel.types.SeverityLevel;
import com.piloto.cdi.kernel.types.StateID;
import com.piloto.cdi.kernel.types.VersionID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Executive Controller orchestrates the complete command-to-event-to-state pipeline.
 * 
 * <p>This is the central orchestrator that coordinates all Phase 1-3 components to
 * process incoming commands deterministically and safely.</p>
 * 
 * <p><b>Execution Pipeline:</b></p>
 * <pre>
 * Command
 *   ↓
 * 1. EvaluationService (technical validation)
 *   ↓ [if valid]
 * 2. Load current SnapshotState
 *   ↓
 * 3. PolicyEngine (business rules)
 *   ↓ [if ALLOW]
 * 4. Translate Command → DomainEvent
 *   ↓
 * 5. EventApplicationService (apply event, persist)
 *   ↓
 * ExecutionResult (success/failure)
 * </pre>
 * 
 * <p><b>Design Principles:</b></p>
 * <ul>
 *   <li>No implicit logic - all decisions explicit and traceable</li>
 *   <li>No side effects outside defined stores</li>
 *   <li>No hidden heuristics or LLM integration</li>
 *   <li>Deterministic - same inputs → same outputs</li>
 *   <li>Fail-fast - stop at first validation/policy failure</li>
 * </ul>
 * 
 * <p><b>Phase 3 Scope:</b></p>
 * <ul>
 *   <li>Basic command types (CREATE_GOAL, UPDATE_GOAL, COMPLETE_GOAL, UPDATE_STATE)</li>
 *   <li>Deterministic event translation</li>
 *   <li>No retry logic</li>
 *   <li>No async processing</li>
 *   <li>No multi-agent coordination</li>
 * </ul>
 * 
 * @since Phase 3
 */
public class ExecutiveController {
    
    private final EvaluationService evaluationService;
    private final PolicyEngine policyEngine;
    private final EventApplicationService eventApplicationService;
    private final ISnapshotStore snapshotStore;
    
    /**
     * Constructs an ExecutiveController with required dependencies.
     * 
     * @param evaluationService technical validation service
     * @param policyEngine business rule engine
     * @param eventApplicationService event persistence and state transition service
     * @param snapshotStore snapshot storage for loading current state
     */
    public ExecutiveController(
            EvaluationService evaluationService,
            PolicyEngine policyEngine,
            EventApplicationService eventApplicationService,
            ISnapshotStore snapshotStore) {
        
        this.evaluationService = Objects.requireNonNull(evaluationService, "evaluationService cannot be null");
        this.policyEngine = Objects.requireNonNull(policyEngine, "policyEngine cannot be null");
        this.eventApplicationService = Objects.requireNonNull(eventApplicationService, "eventApplicationService cannot be null");
        this.snapshotStore = Objects.requireNonNull(snapshotStore, "snapshotStore cannot be null");
    }
    
    /**
     * Executes a command through the complete pipeline.
     * 
     * <p><b>Execution Steps:</b></p>
     * <ol>
     *   <li>Technical validation via EvaluationService</li>
     *   <li>Load current snapshot from store</li>
     *   <li>Policy evaluation via PolicyEngine</li>
     *   <li>Translate command to domain event (if ALLOW)</li>
     *   <li>Apply event via EventApplicationService</li>
     *   <li>Return ExecutionResult</li>
     * </ol>
     * 
     * <p><b>Failure Modes:</b></p>
     * <ul>
     *   <li>Technical validation failure → immediate failure result</li>
     *   <li>Policy REJECT → failure result with violations</li>
     *   <li>Policy NOOP → success result with no event emitted</li>
     * </ul>
     * 
     * @param command command to execute (non-null)
     * @return ExecutionResult indicating success/failure with details
     * @throws NullPointerException if command is null
     */
    public ExecutionResult execute(Command command) {
        Objects.requireNonNull(command, "command cannot be null");
        
        // Step 1: Technical Validation
        EvaluationResult evaluationResult = evaluationService.validate(command);
        if (!evaluationResult.isValid()) {
            return ExecutionResult.failure(evaluationResult.getIssues());
        }
        
        // Step 2: Load Current Snapshot
        SnapshotState currentSnapshot = snapshotStore.load(command.getTenantId())
            .orElseGet(() -> createInitialSnapshot(command.getTenantId()));
        
        // Step 3: Policy Evaluation
        PolicyDecision policyDecision = policyEngine.evaluate(currentSnapshot, command);
        
        if (policyDecision.getDecisionType() == DecisionType.REJECT) {
            return ExecutionResult.failure(policyDecision.getViolations());
        }
        
        if (policyDecision.getDecisionType() == DecisionType.NOOP) {
            // Command is valid but produces no change
            return ExecutionResult.success(
                Optional.empty(),
                currentSnapshot,
                currentSnapshot.getVersion()
            );
        }
        
        // Step 4: Translate Command → DomainEvent
        DomainEvent event = translateCommandToEvent(command);
        
        // Step 5: Apply Event (this persists event and updates snapshot)
        eventApplicationService.applyEvent(event);
        
        // Step 6: Load Updated Snapshot
        SnapshotState updatedSnapshot = snapshotStore.load(command.getTenantId())
            .orElseThrow(() -> new IllegalStateException(
                "Snapshot not found after event application for tenant: " + command.getTenantId()
            ));
        
        return ExecutionResult.success(
            Optional.of(event),
            updatedSnapshot,
            updatedSnapshot.getVersion()
        );
    }
    
    /**
     * Creates initial empty snapshot when tenant has no existing state.
     * 
     * @param tenantId tenant identifier
     * @return new SnapshotState with initial values
     */
    private SnapshotState createInitialSnapshot(com.piloto.cdi.kernel.types.TenantID tenantId) {
        return SnapshotState.create(
            StateID.generate(),
            tenantId,
            VersionID.of("v0"),
            Map.of(),
            Set.of()
        );
    }
    
    /**
     * Translates a command into a corresponding domain event.
     * 
     * <p><b>Translation Rules:</b></p>
     * <ul>
     *   <li>CREATE_GOAL → GOAL_CREATED event</li>
     *   <li>UPDATE_GOAL → GOAL_UPDATED event</li>
     *   <li>COMPLETE_GOAL → GOAL_COMPLETED event</li>
     *   <li>UPDATE_STATE → STATE_TRANSITION event</li>
     *   <li>SYSTEM_COMMAND → SYSTEM_ERROR event (placeholder)</li>
     * </ul>
     * 
     * @param command command to translate
     * @return DomainEvent representing the command's intent
     */
    private DomainEvent translateCommandToEvent(Command command) {
        EventType eventType = mapCommandTypeToEventType(command.getCommandType());
        
        Map<String, String> metadata = new HashMap<>();
        command.getPayload().forEach((key, value) -> 
            metadata.put(key, value != null ? value.toString() : "null")
        );
        
        // Add command correlation info to metadata
        command.getCorrelationId().ifPresent(corrId -> 
            metadata.put("correlationId", corrId)
        );
        command.getCausalReference().ifPresent(causalRef -> 
            metadata.put("causalCommandId", causalRef.getValue())
        );
        
        return DomainEvent.create(
            eventType,
            ActorType.SYSTEM, // Phase 3: all commands processed by system
            "ExecutiveController",
            SeverityLevel.INFO,
            command.getTenantId(),
            metadata
        );
    }
    
    /**
     * Maps CommandType to corresponding EventType.
     * 
     * @param commandType command type to map
     * @return corresponding EventType
     */
    private EventType mapCommandTypeToEventType(CommandType commandType) {
        switch (commandType) {
            case CREATE_GOAL:
                return EventType.GOAL_CREATED;
            case UPDATE_GOAL:
                return EventType.GOAL_UPDATED;
            case COMPLETE_GOAL:
                return EventType.GOAL_COMPLETED;
            case UPDATE_STATE:
                return EventType.STATE_TRANSITION;
            case SYSTEM_COMMAND:
                return EventType.SYSTEM_ERROR; // Placeholder for Phase 3
            default:
                throw new IllegalArgumentException("Unknown CommandType: " + commandType);
        }
    }
}
