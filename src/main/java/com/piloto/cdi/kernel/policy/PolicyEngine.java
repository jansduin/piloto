package com.piloto.cdi.kernel.policy;

import com.piloto.cdi.kernel.command.Command;
import com.piloto.cdi.kernel.model.SnapshotState;

/**
 * Policy engine interface for evaluating commands against business rules.
 * 
 * <p>The PolicyEngine is responsible for separating decision logic from execution logic.
 * It evaluates whether a command should be allowed based on current system state and
 * explicit policy rules.</p>
 * 
 * <p><b>Design Principles:</b></p>
 * <ul>
 *   <li>Stateless - no internal state, purely functional evaluation</li>
 *   <li>Deterministic - same inputs always produce same decision</li>
 *   <li>Explicit rules - no hidden heuristics or implicit logic</li>
 *   <li>Traceable - decisions include violation reasons and metadata</li>
 * </ul>
 * 
 * <p><b>Phase 3 Scope:</b></p>
 * <ul>
 *   <li>Structural validation (tenant match, version validity)</li>
 *   <li>Basic business rules (no LLM, no dynamic policies)</li>
 *   <li>Command type recognition</li>
 * </ul>
 * 
 * <p><b>Not in Phase 3:</b></p>
 * <ul>
 *   <li>LLM-based policy inference</li>
 *   <li>Dynamic rule loading from external sources</li>
 *   <li>Complex constraint satisfaction solving</li>
 *   <li>Probabilistic decision making</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>
 * PolicyEngine engine = new BasicPolicyEngine();
 * PolicyDecision decision = engine.evaluate(currentSnapshot, incomingCommand);
 * 
 * switch (decision.getDecisionType()) {
 *     case ALLOW:
 *         // Proceed to ExecutiveController
 *         break;
 *     case REJECT:
 *         // Log violations and abort
 *         break;
 *     case NOOP:
 *         // Command is duplicate, skip execution
 *         break;
 * }
 * </pre>
 * 
 * @since Phase 3
 */
public interface PolicyEngine {
    
    /**
     * Evaluates a command against current state and policy rules.
     * 
     * @param snapshot current system snapshot (non-null)
     * @param command incoming command to evaluate (non-null)
     * @return PolicyDecision indicating ALLOW/REJECT/NOOP with reasons
     * @throws NullPointerException if snapshot or command is null
     */
    PolicyDecision evaluate(SnapshotState snapshot, Command command);
}
