package com.piloto.cdi.kernel.diagnostics;

import java.util.HashMap;
import java.util.Map;

public class PerformanceAnalyzer {

    private static final double FAILURE_WEIGHT = 0.4;
    private static final double ITERATION_WEIGHT = 0.3;
    private static final double TIMEOUT_WEIGHT = 0.3;

    private static final double SCORE_WEIGHT = 0.5;
    private static final double SUCCESS_RATE_WEIGHT = 0.3;
    private static final double APPROVAL_WEIGHT = 0.2;

    private static final double CRITICAL_FAILURE_THRESHOLD = 0.3;
    private static final double HIGH_ITERATION_THRESHOLD = 2.0;
    private static final double HIGH_TIMEOUT_THRESHOLD = 0.2;

    public Map<String, Double> analyze(Map<String, Double> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return Map.of(
                    "efficiency_score", 0.5,
                    "stability_score", 0.5,
                    "risk_score", 0.5);
        }

        double efficiencyScore = calculateEfficiencyScore(metrics);
        double stabilityScore = calculateStabilityScore(metrics);
        double riskScore = calculateRiskScore(metrics);

        Map<String, Double> result = new HashMap<>();
        result.put("efficiency_score", clamp(efficiencyScore));
        result.put("stability_score", clamp(stabilityScore));
        result.put("risk_score", clamp(riskScore));

        return result;
    }

    private double calculateEfficiencyScore(Map<String, Double> metrics) {
        double iterationPenalty = metrics.getOrDefault("iteration_average", 0.5) * ITERATION_WEIGHT;
        double timeoutPenalty = metrics.getOrDefault("tool_timeout_rate", 0.0) * TIMEOUT_WEIGHT;
        double failurePenalty = metrics.getOrDefault("failure_rate", 0.0) * FAILURE_WEIGHT;

        double score = 1.0 - (iterationPenalty + timeoutPenalty + failurePenalty);
        return score;
    }

    private double calculateStabilityScore(Map<String, Double> metrics) {
        double successRate = 1.0 - metrics.getOrDefault("failure_rate", 0.0);
        double scoreQuality = metrics.getOrDefault("avg_reasoning_score", 0.5);
        double approvalRate = 1.0 - metrics.getOrDefault("approval_denial_rate", 0.0);

        double score = (successRate * SUCCESS_RATE_WEIGHT) +
                (scoreQuality * SCORE_WEIGHT) +
                (approvalRate * APPROVAL_WEIGHT);

        return score;
    }

    private double calculateRiskScore(Map<String, Double> metrics) {
        double failureRate = metrics.getOrDefault("failure_rate", 0.0);
        double iterationAvg = metrics.getOrDefault("iteration_average", 0.0);
        double timeoutRate = metrics.getOrDefault("tool_timeout_rate", 0.0);
        double denialRate = metrics.getOrDefault("approval_denial_rate", 0.0);
        double lowScore = 1.0 - metrics.getOrDefault("avg_reasoning_score", 0.5);

        double baseRisk = (failureRate * 0.3) +
                (iterationAvg * 0.2) +
                (timeoutRate * 0.2) +
                (denialRate * 0.15) +
                (lowScore * 0.15);

        if (failureRate >= CRITICAL_FAILURE_THRESHOLD) {
            baseRisk = Math.max(baseRisk, 0.8);
        }

        if (iterationAvg >= HIGH_ITERATION_THRESHOLD &&
                metrics.getOrDefault("avg_reasoning_score", 0.5) < 0.6) {
            baseRisk = Math.max(baseRisk, 0.7);
        }

        if (timeoutRate >= HIGH_TIMEOUT_THRESHOLD) {
            baseRisk = Math.max(baseRisk, 0.65);
        }

        return baseRisk;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
