package com.piloto.cdi.kernel.memory;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import com.piloto.cdi.kernel.types.TenantID;

/**
 * Immutable memory entry in the hybrid memory system.
 * 
 * <p>
 * Represents a single piece of stored information with versioning,
 * metadata, and relevance scoring capabilities.
 * </p>
 * 
 * <p>
 * <b>Immutability Contract:</b>
 * </p>
 * <ul>
 * <li>All fields final</li>
 * <li>Map.copyOf() for defensive copy</li>
 * <li>No setters</li>
 * <li>Updates create new instances with incremented version</li>
 * </ul>
 * 
 * @since Phase 4
 */
public final class MemoryEntry {

    private final UUID id;
    private final TenantID tenantId;
    private final MemoryType type;
    private final String content;
    private final Map<String, String> metadata;
    private final Instant createdAt;
    private final int version;
    private final String sourceAgent;
    private final Optional<Double> relevanceScore;

    private MemoryEntry(
            UUID id,
            TenantID tenantId,
            MemoryType type,
            String content,
            Map<String, String> metadata,
            Instant createdAt,
            int version,
            String sourceAgent,
            Optional<Double> relevanceScore) {

        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1, got: " + version);
        }

        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.content = Objects.requireNonNull(content, "content cannot be null");
        this.metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata cannot be null"));
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.version = version;
        this.sourceAgent = Objects.requireNonNull(sourceAgent, "sourceAgent cannot be null");
        this.relevanceScore = Objects.requireNonNull(relevanceScore, "relevanceScore Optional cannot be null");
    }

    /**
     * Creates new memory entry with generated ID and current timestamp.
     * 
     * @param tenantId    tenant identifier
     * @param type        memory type
     * @param content     textual content
     * @param metadata    additional metadata (immutable)
     * @param sourceAgent agent identifier that created this entry
     * @return new MemoryEntry with version 1
     */
    public static MemoryEntry create(
            TenantID tenantId,
            MemoryType type,
            String content,
            Map<String, String> metadata,
            String sourceAgent) {
        return new MemoryEntry(
                UUID.randomUUID(),
                tenantId,
                type,
                content,
                metadata,
                Instant.now(),
                1,
                sourceAgent,
                Optional.empty());
    }

    /**
     * Creates memory entry with explicit relevance score.
     * 
     * @param tenantId       tenant identifier
     * @param type           memory type
     * @param content        textual content
     * @param metadata       additional metadata
     * @param sourceAgent    agent identifier
     * @param relevanceScore semantic similarity score (0.0-1.0)
     * @return new MemoryEntry
     */
    public static MemoryEntry createWithScore(
            TenantID tenantId,
            MemoryType type,
            String content,
            Map<String, String> metadata,
            String sourceAgent,
            double relevanceScore) {
        return new MemoryEntry(
                UUID.randomUUID(),
                tenantId,
                type,
                content,
                metadata,
                Instant.now(),
                1,
                sourceAgent,
                Optional.of(relevanceScore));
    }

    /**
     * Creates updated copy with incremented version (for updates).
     * 
     * @param content  new content
     * @param metadata new metadata
     * @return new MemoryEntry with version + 1
     */
    public MemoryEntry createUpdated(String content, Map<String, String> metadata) {
        return new MemoryEntry(
                this.id,
                this.tenantId,
                this.type,
                content,
                metadata,
                this.createdAt,
                this.version + 1,
                this.sourceAgent,
                this.relevanceScore);
    }

    /**
     * Creates copy with updated relevance score (for query results).
     * 
     * @param score new relevance score
     * @return new MemoryEntry with updated score
     */
    public MemoryEntry withRelevanceScore(double score) {
        return new MemoryEntry(
                this.id,
                this.tenantId,
                this.type,
                this.content,
                this.metadata,
                this.createdAt,
                this.version,
                this.sourceAgent,
                Optional.of(score));
    }

    /**
     * Restores a memory entry from persistent storage.
     * 
     * @return Restored MemoryEntry
     */
    public static MemoryEntry restore(
            UUID id,
            TenantID tenantId,
            MemoryType type,
            String content,
            Map<String, String> metadata,
            Instant createdAt,
            int version,
            String sourceAgent) {
        return new MemoryEntry(id, tenantId, type, content, metadata, createdAt, version, sourceAgent,
                Optional.empty());
    }

    public UUID getId() {
        return id;
    }

    public TenantID getTenantId() {
        return tenantId;
    }

    public MemoryType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getVersion() {
        return version;
    }

    public String getSourceAgent() {
        return sourceAgent;
    }

    public Optional<Double> getRelevanceScore() {
        return relevanceScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MemoryEntry that = (MemoryEntry) o;
        return version == that.version &&
                id.equals(that.id) &&
                tenantId.equals(that.tenantId) &&
                type == that.type &&
                content.equals(that.content) &&
                metadata.equals(that.metadata) &&
                createdAt.equals(that.createdAt) &&
                sourceAgent.equals(that.sourceAgent) &&
                relevanceScore.equals(that.relevanceScore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tenantId, type, content, metadata, createdAt, version, sourceAgent, relevanceScore);
    }

    @Override
    public String toString() {
        return "MemoryEntry{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", type=" + type +
                ", content='" + (content.length() > 50 ? content.substring(0, 50) + "..." : content) + '\'' +
                ", version=" + version +
                ", sourceAgent='" + sourceAgent + '\'' +
                ", relevanceScore=" + relevanceScore +
                '}';
    }
}
