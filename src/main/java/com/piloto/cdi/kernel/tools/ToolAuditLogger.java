package com.piloto.cdi.kernel.tools;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ToolAuditLogger {
    private final ConcurrentLinkedQueue<AuditRecord> auditLog = new ConcurrentLinkedQueue<>();
    
    public void record(ToolExecutionRequest request, ToolExecutionResult result, ApprovalDecision approval) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }
        
        AuditRecord record = new AuditRecord(request, result, approval, Instant.now());
        auditLog.add(record);
    }
    
    public List<AuditRecord> getAuditLog() {
        return new ArrayList<>(auditLog);
    }
    
    public long getRecordCount() {
        return auditLog.size();
    }
    
    public void clear() {
        auditLog.clear();
    }
    
    public static final class AuditRecord {
        private final ToolExecutionRequest request;
        private final ToolExecutionResult result;
        private final ApprovalDecision approval;
        private final Instant timestamp;
        
        public AuditRecord(ToolExecutionRequest request, ToolExecutionResult result, ApprovalDecision approval, Instant timestamp) {
            this.request = request;
            this.result = result;
            this.approval = approval;
            this.timestamp = timestamp;
        }
        
        public ToolExecutionRequest getRequest() {
            return request;
        }
        
        public ToolExecutionResult getResult() {
            return result;
        }
        
        public ApprovalDecision getApproval() {
            return approval;
        }
        
        public Instant getTimestamp() {
            return timestamp;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AuditRecord)) return false;
            AuditRecord that = (AuditRecord) o;
            return request.equals(that.request) &&
                    result.equals(that.result) &&
                    Objects.equals(approval, that.approval) &&
                    timestamp.equals(that.timestamp);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(request, result, approval, timestamp);
        }
        
        @Override
        public String toString() {
            return "AuditRecord{" +
                    "tool='" + request.getToolName() + '\'' +
                    ", success=" + result.isSuccess() +
                    ", executionTimeMs=" + result.getExecutionTimeMs() +
                    ", approved=" + (approval != null ? approval.isApproved() : "N/A") +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}
