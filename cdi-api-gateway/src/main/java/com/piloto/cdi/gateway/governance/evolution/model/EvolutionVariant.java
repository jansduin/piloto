package com.piloto.cdi.gateway.governance.evolution.model;

import com.piloto.cdi.kernel.governance.type.PromptLayer;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A prompt variant occupying one cell in the MAP-Elites behavioral space.
 *
 * DESIGN DECISIONS:
 * - Composes PromptEntity via promptEntityId (does NOT duplicate content)
 * - Uses AtomicInteger for thread-safe outcome recording
 * - Fitness is computed via Wilson Score Interval (not raw conversion rate)
 * - Builder pattern follows PromptEntity.Builder convention
 *
 * Reference: Wilson, 1927 — "Probable inference, the law of succession"
 */
public class EvolutionVariant {

    private final UUID id;
    private final UUID promptEntityId;
    private final BehavioralCell cell;
    private final PromptLayer layer;
    private final String tenantId;
    private final String promptContentSnapshot;

    private final AtomicInteger totalSeen = new AtomicInteger(0);
    private final AtomicInteger totalConverted = new AtomicInteger(0);

    private final Instant createdAt;
    private volatile Instant lastOutcomeAt;

    private EvolutionVariant(Builder builder) {
        this.id = builder.id;
        this.promptEntityId = builder.promptEntityId;
        this.cell = builder.cell;
        this.layer = builder.layer;
        this.tenantId = builder.tenantId;
        this.promptContentSnapshot = builder.promptContentSnapshot;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.lastOutcomeAt = builder.lastOutcomeAt;
        if (builder.totalSeen > 0) this.totalSeen.set(builder.totalSeen);
        if (builder.totalConverted > 0) this.totalConverted.set(builder.totalConverted);
    }

    /**
     * Thread-safe outcome recording via AtomicInteger.
     * No race conditions when multiple requests report concurrently.
     */
    public void recordOutcome(boolean converted) {
        this.totalSeen.incrementAndGet();
        if (converted) this.totalConverted.incrementAndGet();
        this.lastOutcomeAt = Instant.now();
    }

    public int getTotalSeen() { return totalSeen.get(); }
    public int getTotalConverted() { return totalConverted.get(); }

    public double getRawConversionRate() {
        int seen = totalSeen.get();
        return seen > 0 ? (double) totalConverted.get() / seen : 0.0;
    }

    public boolean hasMinSamples(int min) { return totalSeen.get() >= min; }

    public UUID getId() { return id; }
    public UUID getPromptEntityId() { return promptEntityId; }
    public BehavioralCell getCell() { return cell; }
    public PromptLayer getLayer() { return layer; }
    public String getTenantId() { return tenantId; }
    public String getPromptContentSnapshot() { return promptContentSnapshot; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastOutcomeAt() { return lastOutcomeAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID promptEntityId;
        private BehavioralCell cell;
        private PromptLayer layer;
        private String tenantId;
        private String promptContentSnapshot;
        private Instant createdAt;
        private Instant lastOutcomeAt;
        private int totalSeen;
        private int totalConverted;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder promptEntityId(UUID promptEntityId) { this.promptEntityId = promptEntityId; return this; }
        public Builder cell(BehavioralCell cell) { this.cell = cell; return this; }
        public Builder layer(PromptLayer layer) { this.layer = layer; return this; }
        public Builder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public Builder promptContentSnapshot(String s) { this.promptContentSnapshot = s; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder lastOutcomeAt(Instant lastOutcomeAt) { this.lastOutcomeAt = lastOutcomeAt; return this; }
        public Builder totalSeen(int totalSeen) { this.totalSeen = totalSeen; return this; }
        public Builder totalConverted(int totalConverted) { this.totalConverted = totalConverted; return this; }

        public EvolutionVariant build() {
            if (id == null) id = UUID.randomUUID();
            if (cell == null) throw new IllegalArgumentException("cell is required");
            if (layer == null) throw new IllegalArgumentException("layer is required");
            if (promptContentSnapshot == null || promptContentSnapshot.isBlank())
                throw new IllegalArgumentException("promptContentSnapshot is required");
            return new EvolutionVariant(this);
        }
    }
}
