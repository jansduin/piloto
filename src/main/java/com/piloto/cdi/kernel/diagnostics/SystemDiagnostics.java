package com.piloto.cdi.kernel.diagnostics;

import com.piloto.cdi.kernel.orchestrator.ExecutionTrace;

import java.util.*;

public class SystemDiagnostics {

    private final TelemetryCollector telemetry;
    private final MetricsEngine metricsEngine;
    private final AnomalyDetector anomalyDetector;
    private final PerformanceAnalyzer performanceAnalyzer;
    private final ConsistencyValidator consistencyValidator;
    private final OptimizationRecommender optimizer;
    private final SystemHealthEngine healthEngine;
    private final DiagnosticReviewGateway reviewGateway;

    public SystemDiagnostics(
            TelemetryCollector telemetry,
            MetricsEngine metricsEngine,
            AnomalyDetector anomalyDetector,
            PerformanceAnalyzer performanceAnalyzer,
            ConsistencyValidator consistencyValidator,
            OptimizationRecommender optimizer,
            SystemHealthEngine healthEngine,
            DiagnosticReviewGateway reviewGateway) {
        if (telemetry == null || metricsEngine == null || anomalyDetector == null ||
                performanceAnalyzer == null || consistencyValidator == null ||
                optimizer == null || healthEngine == null || reviewGateway == null) {
            throw new IllegalArgumentException("All diagnostic components must be non-null");
        }

        this.telemetry = telemetry;
        this.metricsEngine = metricsEngine;
        this.anomalyDetector = anomalyDetector;
        this.performanceAnalyzer = performanceAnalyzer;
        this.consistencyValidator = consistencyValidator;
        this.optimizer = optimizer;
        this.healthEngine = healthEngine;
        this.reviewGateway = reviewGateway;
    }

    public Map<String, Object> runFullDiagnostic(List<ExecutionTrace.TraceEntry> trace) {
        telemetry.record("DIAGNOSTICS", "DIAGNOSTIC_RUN", Map.of("timestamp", System.currentTimeMillis()));
        // Collect Phase
        List<TelemetryCollector.TelemetryEvent> events = telemetry.getAllEvents();

        // Compute Phase
        Map<String, Double> metrics = metricsEngine.compute(events);

        // Detect Phase
        List<String> anomalies = anomalyDetector.detect(metrics);

        // Analyze Phase (Performance)
        Map<String, Double> performance = performanceAnalyzer.analyze(metrics);

        // Audit Phase (Consistency)
        Map<String, Object> consistency = consistencyValidator.validate(trace);

        // Score Phase (Health)
        double healthScore = healthEngine.computeHealth(metrics, performance, consistency);

        // Recommend Phase (Optimization)
        List<Map<String, Object>> recommendations = optimizer.recommend(anomalies, performance);

        // Review Phase (Human Gateway) - Simulated for now as per spec
        Map<String, Object> reviewResult = reviewGateway.review(recommendations);

        // Build Report
        Map<String, Object> report = new HashMap<>();
        report.put("timestamp", System.currentTimeMillis());
        report.put("diagnostic_version", "2.0-REAL"); // Bump version to indicate real impl
        report.put("total_events_collected", events.size());
        report.put("metrics", metrics);
        report.put("anomalies", anomalies);
        report.put("anomaly_count", anomalies.size());
        report.put("consistency", consistency);
        report.put("performance", performance);
        report.put("recommendations", recommendations);
        report.put("review_result", reviewResult);

        if (events.isEmpty() && (trace == null || trace.isEmpty())) {
            report.put("health_score", 1.0);
            report.put("health_status", "EXCELLENT (READY)");
        } else {
            report.put("health_score", healthScore);
            report.put("health_status", getHealthStatus(healthScore));
        }

        return Collections.unmodifiableMap(report);
    }

    private String getHealthStatus(double healthScore) {
        if (healthScore >= 0.9)
            return "EXCELLENT";
        if (healthScore >= 0.75)
            return "GOOD";
        if (healthScore >= 0.6)
            return "FAIR";
        if (healthScore >= 0.4)
            return "DEGRADED";
        return "CRITICAL";
    }

    public TelemetryCollector getTelemetry() {
        return telemetry;
    }

    public Map<String, Double> getLatestMetrics() {
        return metricsEngine.compute(telemetry.getAllEvents());
    }

    public double getLatestHealthScore() {
        List<TelemetryCollector.TelemetryEvent> events = telemetry.getAllEvents();
        Map<String, Double> metrics = metricsEngine.compute(events);
        Map<String, Double> performance = performanceAnalyzer.analyze(metrics);
        Map<String, Object> consistency = Map.of("consistent", true); // Default for quick check
        return healthEngine.computeHealth(metrics, performance, consistency);
    }
}
