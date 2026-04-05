package com.piloto.cdi.kernel.governance.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.piloto.cdi.kernel.governance.type.MemoryCategory;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a discrete item of domain knowledge or business rule.
 */
public class DomainMemoryItem {
    private final UUID id;
    private final String tenantId;
    private final MemoryCategory category;
    private final String title;
    private final String content;
    private final int priority; // 1-5
    private final int version;
    private boolean isActive;
    private final Instant createdAt;
    private Instant updatedAt;
    private final String createdBy;

    private DomainMemoryItem(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.category = builder.category;
        this.title = builder.title;
        this.content = builder.content;
        this.priority = builder.priority;
        this.version = builder.version;
        this.isActive = builder.isActive;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.createdBy = builder.createdBy;
    }

    @JsonCreator
    public DomainMemoryItem(
            @JsonProperty("id") UUID id,
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("category") MemoryCategory category,
            @JsonProperty("title") String title,
            @JsonProperty("content") String content,
            @JsonProperty("priority") int priority,
            @JsonProperty("version") int version,
            @JsonProperty("active") boolean isActive,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt,
            @JsonProperty("createdBy") String createdBy) {
        this.id = id;
        this.tenantId = tenantId;
        this.category = category;
        this.title = title;
        this.content = content;
        this.priority = priority;
        this.version = version;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public MemoryCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public int getPriority() {
        return priority;
    }

    public int getVersion() {
        return version;
    }

    public boolean isActive() {
        return isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setActive(boolean active) {
        this.isActive = active;
        this.updatedAt = Instant.now();
    }

    public static class Builder {
        private UUID id;
        private String tenantId;
        private MemoryCategory category;
        private String title;
        private String content;
        private int priority;
        private int version;
        private boolean isActive;
        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder category(MemoryCategory category) {
            this.category = category;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder isActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public DomainMemoryItem build() {
            if (id == null)
                id = UUID.randomUUID();
            if (createdAt == null)
                createdAt = Instant.now();
            return new DomainMemoryItem(this);
        }
    }
}
