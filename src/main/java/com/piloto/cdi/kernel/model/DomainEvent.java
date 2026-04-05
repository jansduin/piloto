package com.piloto.cdi.kernel.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.piloto.cdi.kernel.types.*;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class DomainEvent {
    private final EventID id;
    private final EventType type;
    private final ActorType actorType;
    private final String actorId;
    private final SeverityLevel severity;
    private final Instant timestamp;
    private final EventID causalReference;
    private final Map<String, String> metadata;
    private final VersionID version;
    private final TenantID tenantId;

    @JsonCreator
    private DomainEvent(
            @JsonProperty("id") EventID id,
            @JsonProperty("type") EventType type,
            @JsonProperty("actorType") ActorType actorType,
            @JsonProperty("actorId") String actorId,
            @JsonProperty("severity") SeverityLevel severity,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("causalReference") EventID causalReference,
            @JsonProperty("metadata") Map<String, String> metadata,
            @JsonProperty("version") VersionID version,
            @JsonProperty("tenantId") TenantID tenantId) {

        if (id == null) {
            throw new IllegalArgumentException("EventID cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("EventType cannot be null");
        }
        if (actorType == null) {
            throw new IllegalArgumentException("ActorType cannot be null");
        }
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actorId cannot be null or blank");
        }
        if (severity == null) {
            throw new IllegalArgumentException("SeverityLevel cannot be null");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp cannot be null");
        }
        if (version == null) {
            throw new IllegalArgumentException("VersionID cannot be null");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantID cannot be null");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("metadata cannot be null");
        }

        this.id = id;
        this.type = type;
        this.actorType = actorType;
        this.actorId = actorId;
        this.severity = severity;
        this.timestamp = timestamp;
        this.causalReference = causalReference;
        this.metadata = Map.copyOf(metadata);
        this.version = version;
        this.tenantId = tenantId;
    }

    public static DomainEvent create(
            EventID id,
            EventType type,
            ActorType actorType,
            String actorId,
            SeverityLevel severity,
            Instant timestamp,
            EventID causalReference,
            Map<String, String> metadata,
            VersionID version,
            TenantID tenantId) {

        return new DomainEvent(
                id,
                type,
                actorType,
                actorId,
                severity,
                timestamp,
                causalReference,
                metadata,
                version,
                tenantId);
    }

    /**
     * Simplified factory method with auto-generated ID, timestamp, version.
     * 
     * @param type      event type
     * @param actorType actor type
     * @param actorId   actor identifier
     * @param severity  severity level
     * @param tenantId  tenant ID
     * @param metadata  event metadata
     * @return new DomainEvent with generated fields
     */
    public static DomainEvent create(
            EventType type,
            ActorType actorType,
            String actorId,
            SeverityLevel severity,
            TenantID tenantId,
            Map<String, String> metadata) {

        return create(
                EventID.generate(),
                type,
                actorType,
                actorId,
                severity,
                Instant.now(),
                null, // no causal reference
                metadata,
                VersionID.of("v1"), // default version
                tenantId);
    }

    // JavaBeans-style getters
    public EventID getId() {
        return id;
    }

    public EventType getType() {
        return type;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public String getActorId() {
        return actorId;
    }

    public SeverityLevel getSeverity() {
        return severity;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public EventID getCausalReference() {
        return causalReference;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public VersionID getVersion() {
        return version;
    }

    public TenantID getTenantId() {
        return tenantId;
    }

    // Record-style accessors for backward compatibility
    public EventID id() {
        return id;
    }

    public EventType type() {
        return type;
    }

    public ActorType actorType() {
        return actorType;
    }

    public String actorId() {
        return actorId;
    }

    public SeverityLevel severity() {
        return severity;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public EventID causalReference() {
        return causalReference;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public VersionID version() {
        return version;
    }

    public TenantID tenantId() {
        return tenantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DomainEvent that = (DomainEvent) o;
        return Objects.equals(id, that.id) &&
                type == that.type &&
                actorType == that.actorType &&
                Objects.equals(actorId, that.actorId) &&
                severity == that.severity &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(causalReference, that.causalReference) &&
                Objects.equals(metadata, that.metadata) &&
                Objects.equals(version, that.version) &&
                Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, actorType, actorId, severity, timestamp,
                causalReference, metadata, version, tenantId);
    }

    @Override
    public String toString() {
        return "DomainEvent{" +
                "id=" + id +
                ", type=" + type +
                ", actorType=" + actorType +
                ", actorId='" + actorId + '\'' +
                ", severity=" + severity +
                ", timestamp=" + timestamp +
                ", causalReference=" + causalReference +
                ", metadata=" + metadata +
                ", version=" + version +
                ", tenantId=" + tenantId +
                '}';
    }
}
