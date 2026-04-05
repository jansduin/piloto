package com.piloto.cdi.kernel.tools.selection;

import java.util.List;

/**
 * UCB1 (Upper Confidence Bound) selection policy for tool candidates.
 *
 * Formula: UCB1(i) = successRate_i + c * sqrt(ln(N) / n_i)
 *   successRate_i = (executions - failures) / executions
 *   N = total executions across all tools
 *   n_i = executions of tool i
 *   c = exploration constant (default: sqrt(2))
 *
 * Tools with 0 executions get priority (explore-first guarantee).
 *
 * Reference: Auer et al., 2002 — "Finite-time Analysis of the Multiarmed Bandit Problem"
 */
public class UCB1SelectionPolicy implements ToolSelectionPolicy {

    private final double explorationConstant;

    public UCB1SelectionPolicy() {
        this.explorationConstant = Math.sqrt(2);
    }

    public UCB1SelectionPolicy(double explorationConstant) {
        if (explorationConstant <= 0)
            throw new IllegalArgumentException("explorationConstant must be > 0");
        this.explorationConstant = explorationConstant;
    }

    @Override
    public String select(List<ToolCandidate> candidates) {
        if (candidates == null || candidates.isEmpty())
            throw new IllegalArgumentException("Candidate list must not be empty");

        if (candidates.size() == 1)
            return candidates.get(0).toolName();

        long totalN = candidates.stream().mapToLong(ToolCandidate::executions).sum();

        // Explore-first: any tool never executed gets priority
        for (ToolCandidate tc : candidates) {
            if (tc.executions() == 0) {
                return tc.toolName();
            }
        }

        // All tools have been executed at least once — apply UCB1 formula
        String bestTool = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (ToolCandidate tc : candidates) {
            double score = ucb1Score(tc, totalN);
            if (score > bestScore) {
                bestScore = score;
                bestTool = tc.toolName();
            }
        }

        return bestTool;
    }

    /**
     * UCB1 score for a single candidate.
     */
    private double ucb1Score(ToolCandidate tc, long totalN) {
        long n = tc.executions();
        if (n == 0) return Double.MAX_VALUE;

        double successRate = tc.successRate();
        double explorationBonus = explorationConstant * Math.sqrt(Math.log(totalN) / n);

        return successRate + explorationBonus;
    }
}
