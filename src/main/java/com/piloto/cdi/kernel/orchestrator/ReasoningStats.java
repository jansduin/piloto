package com.piloto.cdi.kernel.orchestrator;

import java.util.concurrent.atomic.LongAdder;

public class ReasoningStats {
    private final LongAdder totalRuns = new LongAdder();
    private final LongAdder totalIterations = new LongAdder();
    private final LongAdder failedRuns = new LongAdder();
    private final LongAdder totalScore = new LongAdder();
    
    public void recordRun(boolean success, int iterations, double score) {
        totalRuns.increment();
        totalIterations.add(iterations);
        
        if (!success) {
            failedRuns.increment();
        }
        
        totalScore.add((long) (score * 1000));
    }
    
    public long getTotalRuns() {
        return totalRuns.sum();
    }
    
    public long getTotalIterations() {
        return totalIterations.sum();
    }
    
    public long getFailedRuns() {
        return failedRuns.sum();
    }
    
    public double getAvgScore() {
        long total = totalRuns.sum();
        if (total == 0) {
            return 0.0;
        }
        return (totalScore.sum() / 1000.0) / total;
    }
    
    public double getAvgIterations() {
        long total = totalRuns.sum();
        if (total == 0) {
            return 0.0;
        }
        return (double) totalIterations.sum() / total;
    }
    
    public void reset() {
        totalRuns.reset();
        totalIterations.reset();
        failedRuns.reset();
        totalScore.reset();
    }
    
    @Override
    public String toString() {
        return "ReasoningStats{" +
                "totalRuns=" + getTotalRuns() +
                ", totalIterations=" + getTotalIterations() +
                ", failedRuns=" + getFailedRuns() +
                ", avgScore=" + String.format("%.3f", getAvgScore()) +
                ", avgIterations=" + String.format("%.2f", getAvgIterations()) +
                '}';
    }
}
