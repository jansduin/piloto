package com.piloto.cdi.kernel.controller;

import com.piloto.cdi.kernel.model.DomainEvent;
import com.piloto.cdi.kernel.model.SnapshotState;
import com.piloto.cdi.kernel.types.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Controller for deterministic state transitions based on domain events.
 * 
 * <p>
 * <b>Determinism Guarantee:</b>
 * </p>
 * <p>
 * This controller is purely functional and deterministic. Given the same
 * (current state, event) pair, it will always produce the same new state.
 * Timestamps are derived from the event, not from wall-clock time.
 * </p>
 * 
 * <p>
 * <b>Critical Design Decision:</b>
 * </p>
 * <p>
 * updatedAt timestamp is set to event.timestamp() to ensure deterministic
 * replay. This preserves event sourcing semantics where identical event
 * sequences produce identical snapshots regardless of when replay occurs.
 * </p>
 * 
 * @since Phase 2
 */
public class StateTransitionController {

    /**
     * Applies a domain event to current state, producing new immutable state.
     * 
     * <p>
     * <b>Deterministic Behavior:</b>
     * </p>
     * <ul>
     * <li>Same (state, event) inputs → same output state</li>
     * <li>updatedAt = event.timestamp() (not Instant.now())</li>
     * <li>No side effects, no external dependencies</li>
     * </ul>
     * 
     * @param current current snapshot state (non-null)
     * @param event   domain event to apply (non-null)
     * @return new SnapshotState after applying event
     * @throws IllegalArgumentException if current or event is null, or tenant
     *                                  mismatch
     */
    public SnapshotState applyEvent(SnapshotState current, DomainEvent event) {
        if (current == null) {
            throw new IllegalArgumentException("current state cannot be null");
        }
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }

        if (!current.tenantId().equals(event.tenantId())) {
            throw new IllegalArgumentException(
                    "TenantID mismatch: state=" + current.tenantId() + ", event=" + event.tenantId());
        }

        Map<String, String> newProperties = new HashMap<>(current.stateProperties());
        Set<GoalID> newActiveGoals = new HashSet<>(current.activeGoals());

        // Use event timestamp for deterministic replay (not Instant.now())
        Instant updatedAt = event.timestamp();

        switch (event.type()) {
            case STATE_TRANSITION:
                // 1. Support explicit key/value pair (from ExecutiveController UPDATE_STATE)
                String key = event.metadata().get("key");
                String value = event.metadata().get("value");
                boolean usedExplicit = false;
                if (key != null && value != null) {
                    newProperties.put(key, value);
                    usedExplicit = true;
                }

                // 2. Also support generic metadata properties
                // Only exclude "key"/"value" if they were actually used for the explicit
                // mapping above
                boolean finalUsedExplicit = usedExplicit;
                event.metadata().forEach((k, v) -> {
                    boolean isReserved = "key".equals(k) || "value".equals(k);
                    if (!isReserved || !finalUsedExplicit) {
                        newProperties.put(k, v);
                    }
                });
                break;

            case GOAL_CREATED:
                String goalIdStr = event.metadata().get("goalId");
                if (goalIdStr != null && !goalIdStr.isBlank()) {
                    newActiveGoals.add(GoalID.of(goalIdStr));
                }
                // Also update properties if any relevant metadata exists
                event.metadata().forEach((k, v) -> {
                    if (!"goalId".equals(k)) {
                        newProperties.put(k, v);
                    }
                });
                break;

            case GOAL_COMPLETED:
                String completedGoalIdStr = event.metadata().get("goalId");
                if (completedGoalIdStr != null && !completedGoalIdStr.isBlank()) {
                    newActiveGoals.remove(GoalID.of(completedGoalIdStr));
                }
                break;

            case GOAL_UPDATED:
            case SYSTEM_ERROR:
            case DELIBERATION_APPROVED:
            case DELIBERATION_REJECTED:
                // For these events, we might want to capture metadata as state properties
                // e.g. "status", "error_code", "rejection_reason"
                newProperties.putAll(event.metadata());
                break;

            default:
                break;
        }

        return SnapshotState.create(
                StateID.generate(),
                current.tenantId(),
                VersionID.generate(),
                newProperties,
                newActiveGoals,
                current.createdAt(),
                updatedAt // Deterministic: uses event timestamp
        );
    }
}
