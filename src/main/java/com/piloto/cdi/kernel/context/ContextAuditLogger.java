package com.piloto.cdi.kernel.context;

import com.piloto.cdi.kernel.memory.MemoryEntry;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ContextAuditLogger {

    private final Queue<AuditEntry> auditLog = new ConcurrentLinkedQueue<>();

    public void record(
        int tokensUsed,
        List<MemoryEntry> selectedMemories,
        List<MemoryEntry> discardedMemories,
        double compressionRatio,
        String riskLevel
    ) {
        AuditEntry entry = new AuditEntry(
            Instant.now(),
            tokensUsed,
            selectedMemories != null ? selectedMemories.size() : 0,
            discardedMemories != null ? discardedMemories.size() : 0,
            compressionRatio,
            riskLevel
        );

        auditLog.offer(entry);
    }

    public void recordWithDetails(
        int tokensUsed,
        List<MemoryEntry> selectedMemories,
        List<MemoryEntry> discardedMemories,
        double compressionRatio,
        String riskLevel,
        Map<String, Object> additionalData
    ) {
        AuditEntry entry = new AuditEntry(
            Instant.now(),
            tokensUsed,
            selectedMemories != null ? selectedMemories.size() : 0,
            discardedMemories != null ? discardedMemories.size() : 0,
            compressionRatio,
            riskLevel,
            additionalData
        );

        auditLog.offer(entry);
    }

    public List<AuditEntry> getHistory() {
        return List.copyOf(auditLog);
    }

    public List<AuditEntry> getHistory(int limit) {
        if (limit <= 0) {
            return List.of();
        }

        List<AuditEntry> history = new ArrayList<>(auditLog);
        int fromIndex = Math.max(0, history.size() - limit);
        return List.copyOf(history.subList(fromIndex, history.size()));
    }

    public void clear() {
        auditLog.clear();
    }

    public Map<String, Object> getStatistics() {
        if (auditLog.isEmpty()) {
            return Map.of(
                "total_entries", 0,
                "avg_tokens_used", 0.0,
                "avg_compression_ratio", 0.0,
                "high_risk_count", 0
            );
        }

        int totalTokens = 0;
        double totalCompression = 0.0;
        int highRiskCount = 0;
        int selectedMemoriesTotal = 0;
        int discardedMemoriesTotal = 0;

        for (AuditEntry entry : auditLog) {
            totalTokens += entry.getTokensUsed();
            totalCompression += entry.getCompressionRatio();
            selectedMemoriesTotal += entry.getSelectedMemoriesCount();
            discardedMemoriesTotal += entry.getDiscardedMemoriesCount();
            
            if ("HIGH".equals(entry.getRiskLevel())) {
                highRiskCount++;
            }
        }

        int count = auditLog.size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_entries", count);
        stats.put("avg_tokens_used", (double) totalTokens / count);
        stats.put("avg_compression_ratio", totalCompression / count);
        stats.put("high_risk_count", highRiskCount);
        stats.put("avg_selected_memories", (double) selectedMemoriesTotal / count);
        stats.put("avg_discarded_memories", (double) discardedMemoriesTotal / count);

        return Map.copyOf(stats);
    }

    public static class AuditEntry {
        private final Instant timestamp;
        private final int tokensUsed;
        private final int selectedMemoriesCount;
        private final int discardedMemoriesCount;
        private final double compressionRatio;
        private final String riskLevel;
        private final Map<String, Object> additionalData;

        public AuditEntry(
            Instant timestamp,
            int tokensUsed,
            int selectedMemoriesCount,
            int discardedMemoriesCount,
            double compressionRatio,
            String riskLevel
        ) {
            this(timestamp, tokensUsed, selectedMemoriesCount, discardedMemoriesCount,
                 compressionRatio, riskLevel, Map.of());
        }

        public AuditEntry(
            Instant timestamp,
            int tokensUsed,
            int selectedMemoriesCount,
            int discardedMemoriesCount,
            double compressionRatio,
            String riskLevel,
            Map<String, Object> additionalData
        ) {
            this.timestamp = timestamp;
            this.tokensUsed = tokensUsed;
            this.selectedMemoriesCount = selectedMemoriesCount;
            this.discardedMemoriesCount = discardedMemoriesCount;
            this.compressionRatio = compressionRatio;
            this.riskLevel = riskLevel;
            this.additionalData = additionalData != null ? Map.copyOf(additionalData) : Map.of();
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public int getTokensUsed() {
            return tokensUsed;
        }

        public int getSelectedMemoriesCount() {
            return selectedMemoriesCount;
        }

        public int getDiscardedMemoriesCount() {
            return discardedMemoriesCount;
        }

        public double getCompressionRatio() {
            return compressionRatio;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public Map<String, Object> getAdditionalData() {
            return additionalData;
        }

        @Override
        public String toString() {
            return String.format(
                "AuditEntry{timestamp=%s, tokens=%d, selected=%d, discarded=%d, compression=%.2f, risk=%s}",
                timestamp, tokensUsed, selectedMemoriesCount, discardedMemoriesCount,
                compressionRatio, riskLevel
            );
        }
    }
}
