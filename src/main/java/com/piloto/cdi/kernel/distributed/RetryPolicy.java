package com.piloto.cdi.kernel.distributed;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class RetryPolicy {

    private final int maxRetries;
    private final double backoffFactor;
    private final long initialDelayMs;

    public RetryPolicy(int maxRetries, double backoffFactor, long initialDelayMs) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }
        if (backoffFactor < 1.0) {
            throw new IllegalArgumentException("Backoff factor must be >= 1.0");
        }
        if (initialDelayMs <= 0) {
            throw new IllegalArgumentException("Initial delay must be positive");
        }

        this.maxRetries = maxRetries;
        this.backoffFactor = backoffFactor;
        this.initialDelayMs = initialDelayMs;
    }

    public RetryPolicy(int maxRetries) {
        this(maxRetries, 2.0, 100);
    }

    public <T> T execute(Callable<T> operation) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.call();
            } catch (Exception e) {
                lastException = e;
                
                if (attempt < maxRetries) {
                    long delay = calculateDelay(attempt);
                    Thread.sleep(delay);
                }
            }
        }

        throw new RetryExhaustedException(
            "Operation failed after " + (maxRetries + 1) + " attempts",
            lastException
        );
    }

    public <T> CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> operation) {
        return executeAsyncWithRetry(operation, 0);
    }

    private <T> CompletableFuture<T> executeAsyncWithRetry(
        Supplier<CompletableFuture<T>> operation,
        int attempt
    ) {
        return operation.get().exceptionally(throwable -> {
            if (attempt < maxRetries) {
                long delay = calculateDelay(attempt);
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", e);
                }

                return executeAsyncWithRetry(operation, attempt + 1).join();
            } else {
                throw new RuntimeException(
                    new RetryExhaustedException(
                        "Async operation failed after " + (maxRetries + 1) + " attempts",
                        throwable
                    )
                );
            }
        });
    }

    private long calculateDelay(int attempt) {
        return (long) (initialDelayMs * Math.pow(backoffFactor, attempt));
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public double getBackoffFactor() {
        return backoffFactor;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public static class RetryExhaustedException extends Exception {
        public RetryExhaustedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
