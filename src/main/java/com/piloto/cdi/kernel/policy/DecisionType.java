package com.piloto.cdi.kernel.policy;

/**
 * Decision types returned by PolicyEngine evaluation.
 * 
 * <p>Represents the outcome of policy evaluation against a command and current state.</p>
 * 
 * <ul>
 *   <li><b>ALLOW</b> - Command passes all policy checks, may proceed to execution</li>
 *   <li><b>REJECT</b> - Command violates one or more policies, must not execute</li>
 *   <li><b>NOOP</b> - Command is valid but produces no state change (idempotent duplicate)</li>
 * </ul>
 * 
 * @since Phase 3
 */
public enum DecisionType {
    ALLOW,
    REJECT,
    NOOP
}
