package com.piloto.cdi.kernel.orchestrator;

public enum ReasoningStage {
    CONTEXT_BUILDING,
    TASK_DECOMPOSITION,
    STRATEGY_PLANNING,

    // Deliberation Protocol States
    PROPOSAL_GENERATION,
    CRITIQUE,
    FACT_CHECK,
    ARBITRATION,

    // Execution & Evaluation
    EXECUTION,
    SELF_EVALUATION,
    FINALIZATION
}
