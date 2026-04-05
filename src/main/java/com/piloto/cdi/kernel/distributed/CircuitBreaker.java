package com.piloto.cdi.kernel.distributed;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class CircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final int failureThreshold;
    private final long recoveryTimeoutMs;

    private State state = State.CLOSED;
    private int failureCount = 0;
    private int successCount = 0;
    private Instant lastFailureTime;

    public CircuitBreaker(int failureThreshold, long recoveryTimeoutMs) {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("Failure threshold must be positive");
        }
        if (recoveryTimeoutMs <= 0) {
            throw new IllegalArgumentException("Recovery timeout must be positive");
        }

        this.failureThreshold = failureThreshold;
        this.recoveryTimeoutMs = recoveryTimeoutMs;
    }

    public synchronized void recordSuccess() {
        successCount++;

        if (state == State.HALF_OPEN) {
            if (successCount >= 2) {
                transitionTo(State.CLOSED);
                resetCounters();
            }
        } else if (state == State.CLOSED) {
            failureCount = 0;
        }
    }

    public synchronized void recordFailure() {
        failureCount++;
        lastFailureTime = Instant.now();

        if (state == State.CLOSED) {
            if (failureCount >= failureThreshold) {
                transitionTo(State.OPEN);
            }
        } else if (state == State.HALF_OPEN) {
            transitionTo(State.OPEN);
        }
    }

    public synchronized boolean allowRequest() {
        if (state == State.CLOSED) {
            return true;
        }

        if (state == State.OPEN) {
            if (shouldAttemptRecovery()) {
                transitionTo(State.HALF_OPEN);
                return true;
            }
            return false;
        }

        if (state == State.HALF_OPEN) {
            return true;
        }

        return false;
    }

    private boolean shouldAttemptRecovery() {
        if (lastFailureTime == null) {
            return false;
        }

        Instant recoveryTime = lastFailureTime.plus(recoveryTimeoutMs, ChronoUnit.MILLIS);
        return Instant.now().isAfter(recoveryTime);
    }

    private void transitionTo(State newState) {
        this.state = newState;
    }

    private void resetCounters() {
        this.failureCount = 0;
        this.successCount = 0;
    }

    public synchronized State getState() {
        return state;
    }

    public synchronized Map<String, Object> getStatistics() {
        return Map.of(
            "state", state.name(),
            "failure_count", failureCount,
            "success_count", successCount,
            "failure_threshold", failureThreshold,
            "recovery_timeout_ms", recoveryTimeoutMs
        );
    }

    public synchronized void reset() {
        this.state = State.CLOSED;
        resetCounters();
        this.lastFailureTime = null;
    }
}
