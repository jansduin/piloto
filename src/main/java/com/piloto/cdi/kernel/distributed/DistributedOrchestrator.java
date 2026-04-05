package com.piloto.cdi.kernel.distributed;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public class DistributedOrchestrator {

    private final TenantManager tenantManager;
    private final ShardRouter shardRouter;
    private final IdempotencyManager idempotencyManager;
    private final CircuitBreaker circuitBreaker;
    private final RetryPolicy retryPolicy;
    private final DistributedLock distributedLock;
    private final LoadBalancer loadBalancer;

    public DistributedOrchestrator(
            TenantManager tenantManager,
            ShardRouter shardRouter,
            IdempotencyManager idempotencyManager,
            CircuitBreaker circuitBreaker,
            RetryPolicy retryPolicy,
            DistributedLock distributedLock,
            LoadBalancer loadBalancer) {
        if (tenantManager == null || shardRouter == null || idempotencyManager == null ||
                circuitBreaker == null || retryPolicy == null || distributedLock == null ||
                loadBalancer == null) {
            throw new IllegalArgumentException("All distributed components must be non-null");
        }

        this.tenantManager = tenantManager;
        this.shardRouter = shardRouter;
        this.idempotencyManager = idempotencyManager;
        this.circuitBreaker = circuitBreaker;
        this.retryPolicy = retryPolicy;
        this.distributedLock = distributedLock;
        this.loadBalancer = loadBalancer;
    }

    public <T> ExecutionResult<T> executeDistributed(
            String tenantId,
            Callable<T> operation) throws Exception {
        return executeDistributed(tenantId, generateExecutionId(tenantId), operation);
    }

    public <T> ExecutionResult<T> executeDistributed(
            String tenantId,
            String executionId,
            Callable<T> operation) throws Exception {
        long startTime = System.currentTimeMillis();

        if (!tenantManager.validateIsolation(tenantId)) {
            return ExecutionResult.failure(
                    "Tenant isolation validation failed",
                    executionId);
        }

        int shard = shardRouter.route(tenantId);
        String node = loadBalancer.selectNode(tenantId);

        if (!idempotencyManager.register(executionId)) {
            return ExecutionResult.duplicate(executionId);
        }

        if (!circuitBreaker.allowRequest()) {
            return ExecutionResult.circuitOpen(executionId);
        }

        String lockKey = "tenant_" + tenantId;
        boolean lockAcquired = false;

        try {
            lockAcquired = distributedLock.acquire(lockKey, 5000);

            if (!lockAcquired) {
                return ExecutionResult.lockTimeout(executionId);
            }

            T result = retryPolicy.execute(operation);

            circuitBreaker.recordSuccess();

            long duration = System.currentTimeMillis() - startTime;

            return ExecutionResult.success(
                    result,
                    executionId,
                    shard,
                    node,
                    duration);

        } catch (RetryPolicy.RetryExhaustedException e) {
            circuitBreaker.recordFailure();

            return ExecutionResult.failure(
                    "Operation failed after retries: " + e.getMessage(),
                    executionId);

        } catch (Exception e) {
            circuitBreaker.recordFailure();
            throw e;

        } finally {
            if (lockAcquired) {
                distributedLock.release(lockKey);
            }
        }
    }

    private String generateExecutionId(String tenantId) {
        return tenantId + "_" + UUID.randomUUID().toString();
    }

    public Map<String, Object> getSystemStatistics() {
        return Map.of(
                "tenant_stats", tenantManager.getTenantStats(),
                "shard_stats", shardRouter.getStatistics(),
                "idempotency_stats", idempotencyManager.getStatistics(),
                "circuit_breaker_stats", circuitBreaker.getStatistics(),
                "lock_stats", distributedLock.getStatistics(),
                "load_balancer_stats", loadBalancer.getStatistics());
    }

    public static class ExecutionResult<T> {
        private final boolean success;
        private final T result;
        private final String error;
        private final String executionId;
        private final Integer shard;
        private final String node;
        private final Long durationMs;
        private final ResultType type;

        private enum ResultType {
            SUCCESS,
            FAILURE,
            DUPLICATE,
            CIRCUIT_OPEN,
            LOCK_TIMEOUT
        }

        private ExecutionResult(
                boolean success,
                T result,
                String error,
                String executionId,
                Integer shard,
                String node,
                Long durationMs,
                ResultType type) {
            this.success = success;
            this.result = result;
            this.error = error;
            this.executionId = executionId;
            this.shard = shard;
            this.node = node;
            this.durationMs = durationMs;
            this.type = type;
        }

        public static <T> ExecutionResult<T> success(
                T result,
                String executionId,
                int shard,
                String node,
                long durationMs) {
            return new ExecutionResult<>(
                    true, result, null, executionId, shard, node, durationMs, ResultType.SUCCESS);
        }

        public static <T> ExecutionResult<T> failure(String error, String executionId) {
            return new ExecutionResult<>(
                    false, null, error, executionId, null, null, null, ResultType.FAILURE);
        }

        public static <T> ExecutionResult<T> duplicate(String executionId) {
            return new ExecutionResult<>(
                    false, null, "Duplicate execution detected", executionId, null, null, null, ResultType.DUPLICATE);
        }

        public static <T> ExecutionResult<T> circuitOpen(String executionId) {
            return new ExecutionResult<>(
                    false, null, "Circuit breaker is OPEN", executionId, null, null, null, ResultType.CIRCUIT_OPEN);
        }

        public static <T> ExecutionResult<T> lockTimeout(String executionId) {
            return new ExecutionResult<>(
                    false, null, "Failed to acquire distributed lock", executionId, null, null, null,
                    ResultType.LOCK_TIMEOUT);
        }

        public boolean isSuccess() {
            return success;
        }

        public T getResult() {
            return result;
        }

        public String getError() {
            return error;
        }

        public String getExecutionId() {
            return executionId;
        }

        public Integer getShard() {
            return shard;
        }

        public String getNode() {
            return node;
        }

        public Long getDurationMs() {
            return durationMs;
        }

        public ResultType getType() {
            return type;
        }

        public Map<String, Object> toMap() {
            return Map.of(
                    "success", success,
                    "execution_id", executionId,
                    "type", type.name(),
                    "error", error != null ? error : "",
                    "shard", shard != null ? shard : -1,
                    "node", node != null ? node : "",
                    "duration_ms", durationMs != null ? durationMs : 0L);
        }
    }
}
