package com.piloto.cdi.gateway.governance.evolution.engine;

/**
 * Statistical fitness calculation using Wilson Score Interval.
 *
 * Why Wilson and not raw conversion rate?
 * - Raw: 1 success / 1 trial = 100% → beats 90/100 = 90% (wrong!)
 * - Wilson lower bound: 1/1 ≈ 0.05, 90/100 ≈ 0.83 (correct!)
 *
 * The lower bound naturally penalizes low-sample variants,
 * solving the explore-exploit problem without separate logic.
 *
 * Reference: Wilson, 1927 — "Probable inference, the law of succession"
 * Formula: (p + z²/2n - z√(p(1-p)/n + z²/4n²)) / (1 + z²/n)
 * where p = successes/trials, n = trials, z = z-score for confidence level
 */
public final class FitnessCalculator {

    private FitnessCalculator() {}

    // z=1.96 for 95% confidence interval
    private static final double Z = 1.96;
    private static final double Z_SQUARED = Z * Z;

    /**
     * Calculates the Wilson Score Interval lower bound.
     *
     * @param successes number of successful outcomes
     * @param trials    total number of outcomes
     * @return lower bound of the 95% confidence interval [0.0, 1.0]
     */
    public static double wilsonLowerBound(int successes, int trials) {
        if (trials == 0) return 0.0;
        if (successes < 0) throw new IllegalArgumentException("successes must be >= 0");
        if (successes > trials) throw new IllegalArgumentException("successes must be <= trials");

        double n = trials;
        double p = (double) successes / n;

        double numerator = p + Z_SQUARED / (2 * n)
                - Z * Math.sqrt((p * (1 - p) + Z_SQUARED / (4 * n)) / n);
        double denominator = 1 + Z_SQUARED / n;

        return Math.max(0.0, numerator / denominator);
    }

    /**
     * Whether a variant has enough statistical significance to challenge a champion.
     * Requires both minimum samples AND that Wilson lower bound > 0.
     */
    public static boolean isStatisticallyReady(int successes, int trials, int minSamples) {
        return trials >= minSamples && wilsonLowerBound(successes, trials) > 0.0;
    }
}
