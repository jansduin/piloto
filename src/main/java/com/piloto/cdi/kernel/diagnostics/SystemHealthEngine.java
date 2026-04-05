package com.piloto.cdi.kernel.diagnostics;

import java.util.Map;

public class SystemHealthEngine {

    private static final double CONSISTENCY_WEIGHT = 0.3;
    private static final double STABILITY_WEIGHT = 0.3;
    private static final double EFFICIENCY_WEIGHT = 0.3;
    private static final double RISK_PENALTY_WEIGHT = 0.2;

    private static final double CRITICAL_ANOMALY_PENALTY = 0.3;
    private static final double MAJOR_ANOMALY_PENALTY = 0.15;
    private static final double MINOR_ANOMALY_PENALTY = 0.05;

    public double computeHealth(
            Map<String, Double> metrics,
            Map<String, Double> performance,
            Map<String, Object> consistency) {
        if (metrics == null || performance == null || consistency == null) {
            return 0.5;
        }

        double consistencyScore = computeConsistencyScore(consistency);
        double stabilityScore = performance.getOrDefault("stability_score", 0.5);
        double efficiencyScore = performance.getOrDefault("efficiency_score", 0.5);
        double riskScore = performance.getOrDefault("risk_score", 0.5);

        double baseHealth = (consistencyScore * CONSISTENCY_WEIGHT) +
                (stabilityScore * STABILITY_WEIGHT) +
                (efficiencyScore * EFFICIENCY_WEIGHT) -
                (riskScore * RISK_PENALTY_WEIGHT);

        double anomalyPenalty = calculateAnomalyPenalty(metrics);

        double finalHealth = baseHealth - anomalyPenalty;

        return clamp(finalHealth);
    }

    private double computeConsistencyScore(Map<String, Object> consistency) {
        boolean isConsistent = (Boolean) consistency.getOrDefault("consistent", true);

        // Fix: If explicitly inconsistent, do NOT return 1.0 even if issues list is
        // empty/missing
        if (isConsistent) {
            return 1.0;
        }

        Object issuesObj = consistency.get("issues");
        int issueCount = 0;

        if (issuesObj instanceof java.util.List) {
            issueCount = ((java.util.List<?>) issuesObj).size();
        }

        if (issueCount == 0) {
            return 0.5; // Inconsistent but no details
        } else if (issueCount <= 2) {
            return 0.7;
        } else if (issueCount <= 5) {
            return 0.4;
        } else {
            return 0.1;
        }
    }

    private double calculateAnomalyPenalty(Map<String, Double> metrics) {
        double penalty = 0.0;

        double failureRate = metrics.getOrDefault("failure_rate", 0.0);
        if (failureRate >= 0.3) {
            penalty += CRITICAL_ANOMALY_PENALTY;
        } else if (failureRate >= 0.15) {
            penalty += MAJOR_ANOMALY_PENALTY;
        } else if (failureRate >= 0.05) {
            penalty += MINOR_ANOMALY_PENALTY;
        }

        double avgScore = metrics.getOrDefault("avg_reasoning_score", 0.5);
        if (avgScore < 0.4) {
            penalty += CRITICAL_ANOMALY_PENALTY;
        } else if (avgScore < 0.6) {
            penalty += MAJOR_ANOMALY_PENALTY;
        }

        double iterationAvg = metrics.getOrDefault("iteration_average", 0.0);
        if (iterationAvg >= 5.0) {
            penalty += MAJOR_ANOMALY_PENALTY;
        } else if (iterationAvg >= 2.0) {
            penalty += MINOR_ANOMALY_PENALTY;
        }

        double timeoutRate = metrics.getOrDefault("tool_timeout_rate", 0.0);
        if (timeoutRate >= 0.2) {
            penalty += MAJOR_ANOMALY_PENALTY;
        } else if (timeoutRate >= 0.1) {
            penalty += MINOR_ANOMALY_PENALTY;
        }

        double denialRate = metrics.getOrDefault("approval_denial_rate", 0.0);
        if (denialRate >= 0.4) {
            penalty += MAJOR_ANOMALY_PENALTY;
        } else if (denialRate >= 0.2) {
            penalty += MINOR_ANOMALY_PENALTY;
        }

        return Math.min(penalty, 0.6);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
