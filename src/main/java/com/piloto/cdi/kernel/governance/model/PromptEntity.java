package com.piloto.cdi.kernel.governance.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.piloto.cdi.kernel.governance.type.PromptLayer;
import com.piloto.cdi.kernel.governance.type.PromptRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a versioned prompt fragment within the governance engine.
 */
public class PromptEntity {
    private final UUID id;
    private final String name;
    private final PromptLayer layer;
    private final PromptRole role; // Optional
    private final String domain; // Optional
    private final String tenantId; // Optional
    private final String content;
    private final int version;
    private boolean isActive;
    private final Instant createdAt;
    private Instant updatedAt;
    private final String checksum;

    private PromptEntity(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.layer = builder.layer;
        this.role = builder.role;
        this.domain = builder.domain;
        this.tenantId = builder.tenantId;
        this.content = builder.content;
        this.version = builder.version;
        this.isActive = builder.isActive;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.checksum = builder.checksum;
    }

    @JsonCreator
    public PromptEntity(
            @JsonProperty("id") UUID id,
            @JsonProperty("name") String name,
            @JsonProperty("layer") PromptLayer layer,
            @JsonProperty("role") PromptRole role,
            @JsonProperty("domain") String domain,
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("content") String content,
            @JsonProperty("version") int version,
            @JsonProperty("active") boolean isActive,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt,
            @JsonProperty("checksum") String checksum) {
        this.id = id;
        this.name = name;
        this.layer = layer;
        this.role = role;
        this.domain = domain;
        this.tenantId = tenantId;
        this.content = content;
        this.version = version;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.checksum = checksum;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PromptLayer getLayer() {
        return layer;
    }

    public PromptRole getRole() {
        return role;
    }

    public String getDomain() {
        return domain;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getContent() {
        return content;
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

    public String getChecksum() {
        return checksum;
    }

    public void setActive(boolean active) {
        this.isActive = active;
        this.updatedAt = Instant.now();
    }

    public static class Builder {
        private UUID id;
        private String name;
        private PromptLayer layer;
        private PromptRole role;
        private String domain;
        private String tenantId;
        private String content;
        private int version;
        private boolean isActive;
        private Instant createdAt;
        private Instant updatedAt;
        private String checksum;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder layer(PromptLayer layer) {
            this.layer = layer;
            return this;
        }

        public Builder role(PromptRole role) {
            this.role = role;
            return this;
        }

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
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

        public Builder checksum(String checksum) {
            this.checksum = checksum;
            return this;
        }

        public PromptEntity build() {
            if (id == null)
                id = UUID.randomUUID();
            if (createdAt == null)
                createdAt = Instant.now();
            return new PromptEntity(this);
        }
    }
}
