package com.piloto.cdi.kernel.orchestrator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ExecutionTrace {
    private final ConcurrentLinkedQueue<TraceEntry> entries = new ConcurrentLinkedQueue<>();
    
    public void record(
            ReasoningStage stage,
            String agentName,
            Map<String, Object> inputSnapshot,
            Map<String, Object> outputSnapshot) {
        
        if (stage == null) {
            throw new IllegalArgumentException("stage must not be null");
        }
        
        TraceEntry entry = new TraceEntry(
            stage,
            agentName,
            inputSnapshot != null ? new HashMap<>(inputSnapshot) : Map.of(),
            outputSnapshot != null ? new HashMap<>(outputSnapshot) : Map.of(),
            Instant.now()
        );
        
        entries.add(entry);
    }
    
    public List<TraceEntry> getFullTrace() {
        return new ArrayList<>(entries);
    }
    
    public long getEntryCount() {
        return entries.size();
    }
    
    public void clear() {
        entries.clear();
    }
    
    public static final class TraceEntry {
        private final ReasoningStage stage;
        private final String agentName;
        private final Map<String, Object> inputSnapshot;
        private final Map<String, Object> outputSnapshot;
        private final Instant timestamp;
        
        public TraceEntry(
                ReasoningStage stage,
                String agentName,
                Map<String, Object> inputSnapshot,
                Map<String, Object> outputSnapshot,
                Instant timestamp) {
            this.stage = stage;
            this.agentName = agentName;
            this.inputSnapshot = Map.copyOf(inputSnapshot);
            this.outputSnapshot = Map.copyOf(outputSnapshot);
            this.timestamp = timestamp;
        }
        
        public ReasoningStage getStage() {
            return stage;
        }
        
        public String getAgentName() {
            return agentName;
        }
        
        public Map<String, Object> getInputSnapshot() {
            return inputSnapshot;
        }
        
        public Map<String, Object> getOutputSnapshot() {
            return outputSnapshot;
        }
        
        public Instant getTimestamp() {
            return timestamp;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TraceEntry)) return false;
            TraceEntry that = (TraceEntry) o;
            return stage == that.stage &&
                    Objects.equals(agentName, that.agentName) &&
                    timestamp.equals(that.timestamp);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(stage, agentName, timestamp);
        }
        
        @Override
        public String toString() {
            return "TraceEntry{" +
                    "stage=" + stage +
                    ", agent='" + agentName + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}
