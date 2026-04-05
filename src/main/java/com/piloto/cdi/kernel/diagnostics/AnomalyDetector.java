package com.piloto.cdi.kernel.diagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnomalyDetector {
    private static final double FAILURE_THRESHOLD = 0.3;
    private static final double TIMEOUT_THRESHOLD = 0.2;
    private static final double DENIAL_THRESHOLD = 0.4;
    private static final double LOW_SCORE_THRESHOLD = 0.5;
    private static final double HIGH_ITERATION_THRESHOLD = 2.5;
    
    public List<String> detect(Map<String, Double> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return List.of();
        }
        
        List<String> anomalies = new ArrayList<>();
        
        double failureRate = metrics.getOrDefault("failure_rate", 0.0);
        if (failureRate > FAILURE_THRESHOLD) {
            anomalies.add(String.format("High failure rate detected: %.2f (threshold: %.2f)", 
                failureRate, FAILURE_THRESHOLD));
        }
        
        double timeoutRate = metrics.getOrDefault("tool_timeout_rate", 0.0);
        if (timeoutRate > TIMEOUT_THRESHOLD) {
            anomalies.add(String.format("High tool timeout rate: %.2f (threshold: %.2f)", 
                timeoutRate, TIMEOUT_THRESHOLD));
        }
        
        double denialRate = metrics.getOrDefault("approval_denial_rate", 0.0);
        if (denialRate > DENIAL_THRESHOLD) {
            anomalies.add(String.format("High approval denial rate: %.2f (threshold: %.2f)", 
                denialRate, DENIAL_THRESHOLD));
        }
        
        double avgScore = metrics.getOrDefault("avg_reasoning_score", 1.0);
        if (avgScore < LOW_SCORE_THRESHOLD) {
            anomalies.add(String.format("Low average reasoning score: %.2f (threshold: %.2f)", 
                avgScore, LOW_SCORE_THRESHOLD));
        }
        
        double avgIterations = metrics.getOrDefault("iteration_average", 1.0);
        if (avgIterations > HIGH_ITERATION_THRESHOLD) {
            anomalies.add(String.format("Excessive average iterations: %.2f (threshold: %.2f)", 
                avgIterations, HIGH_ITERATION_THRESHOLD));
        }
        
        return anomalies;
    }
}
