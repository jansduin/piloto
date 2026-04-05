package com.piloto.cdi.kernel.diagnostics;

import java.util.*;

public class OptimizationRecommender {

    private static final double HIGH_FAILURE_THRESHOLD = 0.2;
    private static final double HIGH_ITERATION_THRESHOLD = 0.6;
    private static final double HIGH_TIMEOUT_THRESHOLD = 0.15;
    private static final double LOW_SCORE_THRESHOLD = 0.6;
    private static final double HIGH_DENIAL_THRESHOLD = 0.3;

    public List<Map<String, Object>> recommend(
            List<String> anomalies,
            Map<String, Double> performance) {
        if (anomalies == null) {
            anomalies = List.of();
        }
        if (performance == null) {
            performance = Map.of();
        }

        List<Map<String, Object>> recommendations = new ArrayList<>();

        recommendations.addAll(analyzeFailureRate(anomalies, performance));
        recommendations.addAll(analyzeIterations(anomalies, performance));
        recommendations.addAll(analyzeTimeouts(anomalies, performance));
        recommendations.addAll(analyzeScore(anomalies, performance));
        recommendations.addAll(analyzeApprovals(anomalies, performance));

        return List.copyOf(recommendations);
    }

    private List<Map<String, Object>> analyzeFailureRate(
            List<String> anomalies,
            Map<String, Double> performance) {
        List<Map<String, Object>> recs = new ArrayList<>();

        if (containsAnomaly(anomalies, "failure rate")) {
            double risk = performance.getOrDefault("risk_score", 0.5);

            recs.add(Map.of(
                    "action", "Review and strengthen validation layer",
                    "component", "EvaluationService",
                    "expected_benefit", "Reduce invalid command execution by 30-50%",
                    "risk", Math.min(0.3, risk * 0.5)));

            recs.add(Map.of(
                    "action", "Implement retry mechanism with backoff",
                    "component", "ExecutiveController",
                    "expected_benefit", "Recover from transient failures automatically",
                    "risk", 0.25));
        }

        return recs;
    }

    private List<Map<String, Object>> analyzeIterations(
            List<String> anomalies,
            Map<String, Double> performance) {
        List<Map<String, Object>> recs = new ArrayList<>();

        if (containsAnomaly(anomalies, "iterations")) {
            recs.add(Map.of(
                    "action", "Lower self-evaluation score threshold from 0.7 to 0.6",
                    "component", "ReasoningOrchestrator",
                    "expected_benefit", "Reduce unnecessary iterations by 20-30%",
                    "risk", 0.35));

            recs.add(Map.of(
                    "action", "Improve task decomposition heuristics",
                    "component", "TaskDecomposer",
                    "expected_benefit", "Generate clearer sub-tasks, reducing rework",
                    "risk", 0.2));
        }

        return recs;
    }

    private List<Map<String, Object>> analyzeTimeouts(
            List<String> anomalies,
            Map<String, Double> performance) {
        List<Map<String, Object>> recs = new ArrayList<>();

        if (containsAnomaly(anomalies, "timeout rate")) {
            recs.add(Map.of(
                    "action", "Increase sandbox timeout from 5s to 10s",
                    "component", "SandboxPolicy",
                    "expected_benefit", "Allow legitimate long-running tools to complete",
                    "risk", 0.4));

            recs.add(Map.of(
                    "action", "Implement tool execution progress monitoring",
                    "component", "ToolExecutionEngine",
                    "expected_benefit", "Detect and handle stuck tools proactively",
                    "risk", 0.25));
        }

        return recs;
    }

    private List<Map<String, Object>> analyzeScore(
            List<String> anomalies,
            Map<String, Double> performance) {
        List<Map<String, Object>> recs = new ArrayList<>();

        if (containsAnomaly(anomalies, "reasoning score")) {
            double avgScore = performance.getOrDefault("avg_reasoning_score", 0.5);

            if (avgScore < LOW_SCORE_THRESHOLD) {
                recs.add(Map.of(
                        "action", "Enhance SelfEvaluator scoring logic",
                        "component", "SelfEvaluator",
                        "expected_benefit", "More accurate assessment of goal completion",
                        "risk", 0.3));

                recs.add(Map.of(
                        "action", "Integrate memory context into reasoning",
                        "component", "ReasoningOrchestrator",
                        "expected_benefit", "Leverage past experience to improve decisions",
                        "risk", 0.35));
            }
        }

        return recs;
    }

    private List<Map<String, Object>> analyzeApprovals(
            List<String> anomalies,
            Map<String, Double> performance) {
        List<Map<String, Object>> recs = new ArrayList<>();

        if (containsAnomaly(anomalies, "denial rate")) {
            recs.add(Map.of(
                    "action", "Refine tool risk scoring algorithm",
                    "component", "ApprovalGateway",
                    "expected_benefit", "Present more accurate pros/cons for human review",
                    "risk", 0.2));

            recs.add(Map.of(
                    "action", "Implement approval learning mechanism",
                    "component", "ApprovalGateway",
                    "expected_benefit", "Learn from past approvals/denials patterns",
                    "risk", 0.45));
        }

        return recs;
    }

    private boolean containsAnomaly(List<String> anomalies, String keyword) {
        return anomalies.stream().anyMatch(a -> a.toLowerCase().contains(keyword));
    }
}
