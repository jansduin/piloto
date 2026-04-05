package com.piloto.cdi.kernel.types;

/**
 * Defines the operational mode for a command execution.
 * Used for testing, stress simulation, and rigorous validation.
 */
public enum ExecutionMode {
    /**
     * Standard execution flow with default checks.
     */
    NORMAL,

    /**
     * Simulates high load or resource constraints.
     */
    STRESS_TEST,

    /**
     * Enforces maximum scrutiny by the Deliberation Engine.
     * Useful for validating safety guardrails.
     */
    HIGH_RISK_SIMULATION,

    /**
     * Bypasses some non-critical checks for speed (DEV only).
     */
    FAST_TRACK
}
