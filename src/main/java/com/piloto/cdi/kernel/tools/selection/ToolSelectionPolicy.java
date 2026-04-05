package com.piloto.cdi.kernel.tools.selection;

import java.util.List;

/**
 * Strategy interface for selecting among candidate tools.
 * Allows pluggable selection policies (UCB1, random, round-robin, etc.)
 * without modifying ToolExecutionEngine.
 *
 * Implementing classes live in the kernel and require no Spring dependency.
 */
public interface ToolSelectionPolicy {

    /**
     * Select the best tool from the candidate list based on the policy.
     *
     * @param candidates list of tool candidates with execution statistics
     * @return the name of the selected tool
     * @throws IllegalArgumentException if candidates is empty
     */
    String select(List<ToolCandidate> candidates);

    /**
     * Immutable snapshot of a tool's execution statistics for selection.
     */
    record ToolCandidate(String toolName, long executions, long failures) {
        public ToolCandidate {
            if (toolName == null || toolName.isBlank())
                throw new IllegalArgumentException("toolName must not be blank");
            if (executions < 0)
                throw new IllegalArgumentException("executions must be >= 0");
            if (failures < 0)
                throw new IllegalArgumentException("failures must be >= 0");
            if (failures > executions)
                throw new IllegalArgumentException("failures must be <= executions");
        }

        public double successRate() {
            return executions > 0 ? (double) (executions - failures) / executions : 0.0;
        }
    }
}
