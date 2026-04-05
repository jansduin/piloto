package com.piloto.cdi.kernel.tools;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class ToolStats {
    private final LongAdder totalExecutions = new LongAdder();
    private final LongAdder failedExecutions = new LongAdder();
    private final LongAdder approvalDenials = new LongAdder();
    private final LongAdder totalExecutionTimeMs = new LongAdder();
    
    public void recordExecution(ToolExecutionResult result, boolean approvalDenied) {
        totalExecutions.increment();
        
        if (approvalDenied) {
            approvalDenials.increment();
        }
        
        if (!result.isSuccess()) {
            failedExecutions.increment();
        }
        
        totalExecutionTimeMs.add(result.getExecutionTimeMs());
    }
    
    public long getTotalExecutions() {
        return totalExecutions.sum();
    }
    
    public long getFailedExecutions() {
        return failedExecutions.sum();
    }
    
    public long getApprovalDenials() {
        return approvalDenials.sum();
    }
    
    public long getAvgExecutionTimeMs() {
        long total = totalExecutions.sum();
        if (total == 0) {
            return 0;
        }
        return totalExecutionTimeMs.sum() / total;
    }
    
    public void reset() {
        totalExecutions.reset();
        failedExecutions.reset();
        approvalDenials.reset();
        totalExecutionTimeMs.reset();
    }
    
    @Override
    public String toString() {
        return "ToolStats{" +
                "totalExecutions=" + getTotalExecutions() +
                ", failedExecutions=" + getFailedExecutions() +
                ", approvalDenials=" + getApprovalDenials() +
                ", avgExecutionTimeMs=" + getAvgExecutionTimeMs() +
                '}';
    }
}
