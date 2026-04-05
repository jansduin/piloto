package com.piloto.cdi.gateway.governance.evolution.model;

import java.util.Map;

/**
 * Domain-agnostic outcome report from consuming applications.
 * PILOTO F1 does not interpret what "success" means — the consuming app defines it.
 *
 * All dimension values in cellDimensions are defined by the consuming app:
 *   {"intentType": "...", "sessionStage": "..."}
 */
public record OutcomeReport(
    String tenantId,
    String sessionId,
    String variantId,
    Map<String, String> cellDimensions,
    Outcome outcome,
    Long durationMs
) {
    public enum Outcome { SUCCESS, FAILURE }

    public boolean isSuccess() { return outcome == Outcome.SUCCESS; }

    public OutcomeReport {
        if (tenantId == null || tenantId.isBlank())
            throw new IllegalArgumentException("tenantId is required");
        if (sessionId == null || sessionId.isBlank())
            throw new IllegalArgumentException("sessionId is required");
        if (variantId == null || variantId.isBlank())
            throw new IllegalArgumentException("variantId is required");
        if (cellDimensions == null || cellDimensions.isEmpty())
            throw new IllegalArgumentException("cellDimensions is required");
        if (outcome == null)
            throw new IllegalArgumentException("outcome is required");
    }
}
