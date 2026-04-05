package com.piloto.cdi.kernel.distributed;

import com.piloto.cdi.kernel.model.DomainEvent;
import com.piloto.cdi.kernel.types.*;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TenantManager {

    private final Map<String, TenantConfig> tenantConfigs = new ConcurrentHashMap<>();
    private final Map<String, String> tenantNamespaces = new ConcurrentHashMap<>();
    private final DistributedLock lock;
    private final Consumer<DomainEvent> eventPublisher;

    public TenantManager(DistributedLock lock, Consumer<DomainEvent> eventPublisher) {
        if (lock == null) {
            throw new IllegalArgumentException("DistributedLock cannot be null");
        }
        if (eventPublisher == null) {
            throw new IllegalArgumentException("Event publisher cannot be null");
        }
        this.lock = lock;
        this.eventPublisher = eventPublisher;
    }

    public TenantManager() {
        this(new DistributedLock(), event -> {
        });
    }

    public String registerTenant(TenantConfig config, VersionID versionId) {
        if (config == null || config.getTenantId() == null || config.getTenantId().isEmpty()) {
            throw new IllegalArgumentException("Valid tenant config with tenantId required");
        }
        if (versionId == null) {
            throw new IllegalArgumentException("VersionID cannot be null");
        }

        String tenantId = config.getTenantId();
        String lockKey = "tenant_register_" + tenantId;

        try {
            if (!lock.acquire(lockKey)) {
                throw new IllegalStateException("Failed to acquire lock for tenant: " + tenantId);
            }

            if (tenantConfigs.containsKey(tenantId)) {
                throw new IllegalStateException("Tenant already registered: " + tenantId);
            }

            tenantConfigs.put(tenantId, config);
            String namespace = generateNamespace(tenantId, versionId);
            tenantNamespaces.put(tenantId, namespace);

            // Event Sourcing: Emit DomainEvent
            DomainEvent event = DomainEvent.create(
                    EventID.generate(),
                    EventType.TENANT_REGISTERED,
                    ActorType.SYSTEM,
                    "TenantManager",
                    SeverityLevel.INFO,
                    Instant.now(),
                    null,
                    Map.of(
                            "tenant_id", tenantId,
                            "namespace", namespace,
                            "memory_limit_bytes", String.valueOf(config.getMemoryLimitBytes()),
                            "rate_limit_per_minute", String.valueOf(config.getRateLimitPerMinute())),
                    versionId,
                    TenantID.of(tenantId));
            eventPublisher.accept(event);

            return namespace;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while acquiring lock for tenant: " + tenantId, e);
        } finally {
            if (lock.isLocked(lockKey)) {
                lock.release(lockKey);
            }
        }
    }

    public TenantConfig getConfig(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new IllegalArgumentException("TenantId cannot be null or empty");
        }

        TenantConfig config = tenantConfigs.get(tenantId);
        if (config == null) {
            throw new IllegalArgumentException("Tenant not registered: " + tenantId);
        }

        return config;
    }

    public String getNamespace(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new IllegalArgumentException("TenantId cannot be null or empty");
        }

        String namespace = tenantNamespaces.get(tenantId);
        if (namespace == null) {
            throw new IllegalArgumentException("Tenant not registered: " + tenantId);
        }

        return namespace;
    }

    public boolean validateIsolation(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return false;
        }

        return tenantConfigs.containsKey(tenantId) &&
                tenantNamespaces.containsKey(tenantId);
    }

    public boolean isTenantRegistered(String tenantId) {
        return tenantId != null && tenantConfigs.containsKey(tenantId);
    }

    public Map<String, Object> getTenantStats() {
        return Map.of(
                "total_tenants", tenantConfigs.size(),
                "registered_namespaces", tenantNamespaces.size());
    }

    public void unregisterTenant(String tenantId, VersionID versionId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new IllegalArgumentException("TenantId cannot be null or empty");
        }
        if (versionId == null) {
            throw new IllegalArgumentException("VersionID cannot be null");
        }

        String lockKey = "tenant_unregister_" + tenantId;

        try {
            if (!lock.acquire(lockKey)) {
                throw new IllegalStateException("Failed to acquire lock for tenant: " + tenantId);
            }

            tenantConfigs.remove(tenantId);
            tenantNamespaces.remove(tenantId);

            // Event Sourcing: Emit DomainEvent
            DomainEvent event = DomainEvent.create(
                    EventID.generate(),
                    EventType.TENANT_UNREGISTERED,
                    ActorType.SYSTEM,
                    "TenantManager",
                    SeverityLevel.INFO,
                    Instant.now(),
                    null,
                    Map.of("tenant_id", tenantId),
                    versionId,
                    TenantID.of(tenantId));
            eventPublisher.accept(event);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while acquiring lock for tenant: " + tenantId, e);
        } finally {
            if (lock.isLocked(lockKey)) {
                lock.release(lockKey);
            }
        }
    }

    private String generateNamespace(String tenantId, VersionID versionId) {
        return "tenant_" + tenantId + "_ns_" + versionId.value();
    }

    public static class TenantConfig {
        private final String tenantId;
        private final long memoryLimitBytes;
        private final Map<String, Boolean> toolPermissions;
        private final Map<String, Boolean> modelAccess;
        private final int rateLimitPerMinute;

        public TenantConfig(
                String tenantId,
                long memoryLimitBytes,
                Map<String, Boolean> toolPermissions,
                Map<String, Boolean> modelAccess,
                int rateLimitPerMinute) {
            if (tenantId == null || tenantId.isEmpty()) {
                throw new IllegalArgumentException("TenantId cannot be null or empty");
            }
            if (memoryLimitBytes <= 0) {
                throw new IllegalArgumentException("Memory limit must be positive");
            }
            if (rateLimitPerMinute <= 0) {
                throw new IllegalArgumentException("Rate limit must be positive");
            }

            this.tenantId = tenantId;
            this.memoryLimitBytes = memoryLimitBytes;
            this.toolPermissions = toolPermissions != null ? Map.copyOf(toolPermissions) : Map.of();
            this.modelAccess = modelAccess != null ? Map.copyOf(modelAccess) : Map.of();
            this.rateLimitPerMinute = rateLimitPerMinute;
        }

        public String getTenantId() {
            return tenantId;
        }

        public long getMemoryLimitBytes() {
            return memoryLimitBytes;
        }

        public Map<String, Boolean> getToolPermissions() {
            return toolPermissions;
        }

        public Map<String, Boolean> getModelAccess() {
            return modelAccess;
        }

        public int getRateLimitPerMinute() {
            return rateLimitPerMinute;
        }

        public boolean hasToolPermission(String tool) {
            return toolPermissions.getOrDefault(tool, false);
        }

        public boolean hasModelAccess(String model) {
            return modelAccess.getOrDefault(model, false);
        }
    }
}
