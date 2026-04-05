package com.piloto.cdi.kernel.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.piloto.cdi.kernel.types.*;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class SnapshotState {
    private final StateID id;
    private final TenantID tenantId;
    private final VersionID version;
    private final Map<String, String> stateProperties;
    private final Set<GoalID> activeGoals;
    private final Instant createdAt;
    private final Instant updatedAt;

    @JsonCreator
    private SnapshotState(
            @JsonProperty("id") StateID id,
            @JsonProperty("tenantId") TenantID tenantId,
            @JsonProperty("version") VersionID version,
            @JsonProperty("stateProperties") Map<String, String> stateProperties,
            @JsonProperty("activeGoals") Set<GoalID> activeGoals,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt) {

        if (id == null) {
            throw new IllegalArgumentException("StateID cannot be null");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantID cannot be null");
        }
        if (version == null) {
            throw new IllegalArgumentException("VersionID cannot be null");
        }
        if (stateProperties == null) {
            throw new IllegalArgumentException("stateProperties cannot be null");
        }
        if (activeGoals == null) {
            throw new IllegalArgumentException("activeGoals cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt cannot be null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt cannot be null");
        }

        this.id = id;
        this.tenantId = tenantId;
        this.version = version;
        this.stateProperties = Map.copyOf(stateProperties);
        this.activeGoals = Set.copyOf(activeGoals);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SnapshotState create(
            StateID id,
            TenantID tenantId,
            VersionID version,
            Map<String, String> stateProperties,
            Set<GoalID> activeGoals,
            Instant createdAt,
            Instant updatedAt) {

        return new SnapshotState(
                id,
                tenantId,
                version,
                stateProperties,
                activeGoals,
                createdAt,
                updatedAt);
    }

    /**
     * Simplified factory method with auto-generated timestamps.
     * 
     * @param id              state ID
     * @param tenantId        tenant ID
     * @param version         version ID
     * @param stateProperties state properties
     * @param activeGoals     active goals
     * @return new SnapshotState with current timestamp
     */
    public static SnapshotState create(
            StateID id,
            TenantID tenantId,
            VersionID version,
            Map<String, String> stateProperties,
            Set<GoalID> activeGoals) {
        Instant now = Instant.now();
        return create(id, tenantId, version, stateProperties, activeGoals, now, now);
    }

    /**
     * Full factory method with explicit timestamps (for testing).
     * Alias for create with all parameters.
     * 
     * @param id              state ID
     * @param tenantId        tenant ID
     * @param version         version ID (can be null for testing edge cases)
     * @param stateProperties state properties
     * @param activeGoals     active goals
     * @param createdAt       creation timestamp
     * @param updatedAt       update timestamp
     * @return new SnapshotState
     */
    public static SnapshotState createFull(
            StateID id,
            TenantID tenantId,
            VersionID version,
            Map<String, String> stateProperties,
            Set<GoalID> activeGoals,
            Instant createdAt,
            Instant updatedAt) {
        // Allow null version for testing edge cases
        return new SnapshotState(id, tenantId, version, stateProperties, activeGoals, createdAt, updatedAt);
    }

    public StateID getId() {
        return id;
    }

    public TenantID getTenantId() {
        return tenantId;
    }

    public VersionID getVersion() {
        return version;
    }

    public Map<String, String> getStateProperties() {
        return stateProperties;
    }

    public Set<GoalID> getActiveGoals() {
        return activeGoals;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Record-style accessors for backward compatibility
    public StateID id() {
        return id;
    }

    public TenantID tenantId() {
        return tenantId;
    }

    public VersionID version() {
        return version;
    }

    public Map<String, String> stateProperties() {
        return stateProperties;
    }

    public Set<GoalID> activeGoals() {
        return activeGoals;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SnapshotState that = (SnapshotState) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(version, that.version) &&
                Objects.equals(stateProperties, that.stateProperties) &&
                Objects.equals(activeGoals, that.activeGoals) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tenantId, version, stateProperties,
                activeGoals, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "SnapshotState{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", version=" + version +
                ", stateProperties=" + stateProperties +
                ", activeGoals=" + activeGoals +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
