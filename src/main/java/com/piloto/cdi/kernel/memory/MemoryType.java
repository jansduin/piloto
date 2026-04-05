package com.piloto.cdi.kernel.memory;

/**
 * Memory type classification for hybrid memory system.
 * 
 * <p>Four-tier memory architecture:</p>
 * <ul>
 *   <li><b>SHORT_TERM</b>: Session context, recent events (N last operations)</li>
 *   <li><b>EPISODIC</b>: Decisions, completed goals, detected errors, evaluations</li>
 *   <li><b>SEMANTIC</b>: Documentation, architecture, business rules, policies, specs</li>
 *   <li><b>TECHNICAL</b>: Logs, errors, metrics, execution history</li>
 * </ul>
 * 
 * @since Phase 4
 */
public enum MemoryType {
    SHORT_TERM,
    EPISODIC,
    SEMANTIC,
    TECHNICAL
}
