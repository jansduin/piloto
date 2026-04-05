package com.piloto.cdi.kernel.tools;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Context object per iteration/saga that maintains a LIFO queue of executed
 * tools.
 * Used by the ToolExecutionEngine to perform rollbacks.
 */
public class SagaContext {
    private final ConcurrentLinkedDeque<ToolExecutionRequest> executionLog;

    public SagaContext() {
        this.executionLog = new ConcurrentLinkedDeque<>();
    }

    public void addSuccessfulExecution(ToolExecutionRequest request) {
        if (request != null) {
            executionLog.push(request);
        }
    }

    public ConcurrentLinkedDeque<ToolExecutionRequest> getExecutionLog() {
        return executionLog;
    }
}
