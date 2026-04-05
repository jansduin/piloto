package com.piloto.cdi.kernel.distributed;

import java.util.Map;

public class DeploymentConfig {

    public enum Environment {
        DEV,
        STAGING,
        PROD
    }

    public enum IsolationLevel {
        NONE,
        BASIC,
        STRICT
    }

    public enum LoggingLevel {
        DEBUG,
        INFO,
        WARNING,
        ERROR
    }

    private final Environment environment;
    private final int replicas;
    private final long maxMemoryPerNodeBytes;
    private final int maxConcurrentTasks;
    private final IsolationLevel tenantIsolationLevel;
    private final LoggingLevel loggingLevel;

    private DeploymentConfig(Builder builder) {
        this.environment = builder.environment;
        this.replicas = builder.replicas;
        this.maxMemoryPerNodeBytes = builder.maxMemoryPerNodeBytes;
        this.maxConcurrentTasks = builder.maxConcurrentTasks;
        this.tenantIsolationLevel = builder.tenantIsolationLevel;
        this.loggingLevel = builder.loggingLevel;

        validate();
    }

    private void validate() {
        if (environment == null) {
            throw new IllegalArgumentException("Environment cannot be null");
        }

        if (replicas <= 0) {
            throw new IllegalArgumentException("Replicas must be positive");
        }

        if (maxMemoryPerNodeBytes <= 0) {
            throw new IllegalArgumentException("Max memory per node must be positive");
        }

        if (maxConcurrentTasks <= 0) {
            throw new IllegalArgumentException("Max concurrent tasks must be positive");
        }

        if (tenantIsolationLevel == null) {
            throw new IllegalArgumentException("Tenant isolation level cannot be null");
        }

        if (loggingLevel == null) {
            throw new IllegalArgumentException("Logging level cannot be null");
        }

        if (environment == Environment.PROD && tenantIsolationLevel == IsolationLevel.NONE) {
            throw new IllegalArgumentException(
                "Production environment requires at least BASIC tenant isolation");
        }

        if (environment == Environment.PROD && replicas < 2) {
            throw new IllegalArgumentException(
                "Production environment requires at least 2 replicas");
        }
    }

    public Environment getEnvironment() {
        return environment;
    }

    public int getReplicas() {
        return replicas;
    }

    public long getMaxMemoryPerNodeBytes() {
        return maxMemoryPerNodeBytes;
    }

    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    public IsolationLevel getTenantIsolationLevel() {
        return tenantIsolationLevel;
    }

    public LoggingLevel getLoggingLevel() {
        return loggingLevel;
    }

    public Map<String, Object> toMap() {
        return Map.of(
            "environment", environment.name(),
            "replicas", replicas,
            "max_memory_per_node_bytes", maxMemoryPerNodeBytes,
            "max_concurrent_tasks", maxConcurrentTasks,
            "tenant_isolation_level", tenantIsolationLevel.name(),
            "logging_level", loggingLevel.name()
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Environment environment = Environment.DEV;
        private int replicas = 1;
        private long maxMemoryPerNodeBytes = 2L * 1024 * 1024 * 1024;
        private int maxConcurrentTasks = 10;
        private IsolationLevel tenantIsolationLevel = IsolationLevel.BASIC;
        private LoggingLevel loggingLevel = LoggingLevel.INFO;

        public Builder environment(Environment environment) {
            this.environment = environment;
            return this;
        }

        public Builder replicas(int replicas) {
            this.replicas = replicas;
            return this;
        }

        public Builder maxMemoryPerNodeBytes(long maxMemoryPerNodeBytes) {
            this.maxMemoryPerNodeBytes = maxMemoryPerNodeBytes;
            return this;
        }

        public Builder maxConcurrentTasks(int maxConcurrentTasks) {
            this.maxConcurrentTasks = maxConcurrentTasks;
            return this;
        }

        public Builder tenantIsolationLevel(IsolationLevel tenantIsolationLevel) {
            this.tenantIsolationLevel = tenantIsolationLevel;
            return this;
        }

        public Builder loggingLevel(LoggingLevel loggingLevel) {
            this.loggingLevel = loggingLevel;
            return this;
        }

        public DeploymentConfig build() {
            return new DeploymentConfig(this);
        }
    }
}
