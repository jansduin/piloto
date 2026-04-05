package com.piloto.cdi.kernel.model;

import com.piloto.cdi.kernel.types.*;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GoalContract {
    private final GoalID id;
    private final TenantID tenantId;
    private final VersionID version;
    private final String description;
    private final Map<String, String> successMetrics;
    private final Map<String, String> constraints;
    private final int autonomyLevel;
    private final Set<GoalID> subGoals;
    private final Instant createdAt;
    private final Instant updatedAt;

    private GoalContract(
            GoalID id,
            TenantID tenantId,
            VersionID version,
            String description,
            Map<String, String> successMetrics,
            Map<String, String> constraints,
            int autonomyLevel,
            Set<GoalID> subGoals,
            Instant createdAt,
            Instant updatedAt) {
        
        if (id == null) {
            throw new IllegalArgumentException("GoalID cannot be null");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("TenantID cannot be null");
        }
        if (version == null) {
            throw new IllegalArgumentException("VersionID cannot be null");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description cannot be null or blank");
        }
        if (successMetrics == null) {
            throw new IllegalArgumentException("successMetrics cannot be null");
        }
        if (constraints == null) {
            throw new IllegalArgumentException("constraints cannot be null");
        }
        if (autonomyLevel < 0 || autonomyLevel > 3) {
            throw new IllegalArgumentException("autonomyLevel must be between 0 and 3");
        }
        if (subGoals == null) {
            throw new IllegalArgumentException("subGoals cannot be null");
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
        this.description = description;
        this.successMetrics = Map.copyOf(successMetrics);
        this.constraints = Map.copyOf(constraints);
        this.autonomyLevel = autonomyLevel;
        this.subGoals = Set.copyOf(subGoals);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static GoalContract create(
            GoalID id,
            TenantID tenantId,
            VersionID version,
            String description,
            Map<String, String> successMetrics,
            Map<String, String> constraints,
            int autonomyLevel,
            Set<GoalID> subGoals,
            Instant createdAt,
            Instant updatedAt) {
        
        return new GoalContract(
                id,
                tenantId,
                version,
                description,
                successMetrics,
                constraints,
                autonomyLevel,
                subGoals,
                createdAt,
                updatedAt
        );
    }

    public GoalID id() {
        return id;
    }

    public TenantID tenantId() {
        return tenantId;
    }

    public VersionID version() {
        return version;
    }

    public String description() {
        return description;
    }

    public Map<String, String> successMetrics() {
        return successMetrics;
    }

    public Map<String, String> constraints() {
        return constraints;
    }

    public int autonomyLevel() {
        return autonomyLevel;
    }

    public Set<GoalID> subGoals() {
        return subGoals;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoalContract that = (GoalContract) o;
        return autonomyLevel == that.autonomyLevel &&
                Objects.equals(id, that.id) &&
                Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(version, that.version) &&
                Objects.equals(description, that.description) &&
                Objects.equals(successMetrics, that.successMetrics) &&
                Objects.equals(constraints, that.constraints) &&
                Objects.equals(subGoals, that.subGoals) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tenantId, version, description, 
                successMetrics, constraints, autonomyLevel, 
                subGoals, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "GoalContract{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", version=" + version +
                ", description='" + description + '\'' +
                ", successMetrics=" + successMetrics +
                ", constraints=" + constraints +
                ", autonomyLevel=" + autonomyLevel +
                ", subGoals=" + subGoals +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
