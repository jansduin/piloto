package com.piloto.cdi.kernel.governance.type;

/**
 * Defines the hierarchical layers of prompt composition.
 * Order matters: CORE -> ROLE -> DOMAIN -> TENANT -> RUNTIME.
 */
public enum PromptLayer {
    SYSTEM_CORE(1),
    ROLE(2),
    DOMAIN(3),
    TENANT(4),
    RUNTIME(5);

    private final int priority;

    PromptLayer(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
