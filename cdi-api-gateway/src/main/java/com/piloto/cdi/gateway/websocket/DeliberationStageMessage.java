package com.piloto.cdi.gateway.websocket;

import java.time.Instant;

/**
 * Immutable event payload sent to UI clients via WebSocket.
 *
 * <p>
 * Represents a single deliberation stage update from the reasoning engine.
 * Serialized to JSON and broadcast via STOMP to
 * {@code /topic/deliberation/{sessionId}}.
 * </p>
 *
 * <p>
 * <b>JSON example:</b>
 * </p>
 * 
 * <pre>
 * {
 *   "sessionId": "sess-abc123",
 *   "stage": "CRITIQUE",
 *   "status": "COMPLETED",
 *   "content": "The proposal lacks...",
 *   "timestamp": "2026-02-23T17:48:00Z",
 *   "iterationNumber": 2,
 *   "agentRole": "Critic"
 * }
 * </pre>
 */
public record DeliberationStageMessage(
        String sessionId,
        String stage,
        String status, // STARTED | COMPLETED | FAILED | FINAL
        String content,
        String agentRole,
        int iterationNumber,
        Instant timestamp) {

    /** Factory for a stage-started notification (no content yet). */
    public static DeliberationStageMessage started(String sessionId, String stage, String agentRole, int iteration) {
        return new DeliberationStageMessage(sessionId, stage, "STARTED", null, agentRole, iteration, Instant.now());
    }

    /** Factory for a completed stage with result content. */
    public static DeliberationStageMessage completed(String sessionId, String stage, String content,
            String agentRole, int iteration) {
        return new DeliberationStageMessage(sessionId, stage, "COMPLETED",
                truncate(content, 2000), agentRole, iteration, Instant.now());
    }

    /** Factory for the final response (end of deliberation). */
    public static DeliberationStageMessage finalResponse(String sessionId, String content) {
        return new DeliberationStageMessage(sessionId, "FINAL_RESPONSE", "FINAL",
                truncate(content, 4000), "System", 0, Instant.now());
    }

    /** Factory for error events. */
    public static DeliberationStageMessage error(String sessionId, String errorMessage) {
        return new DeliberationStageMessage(sessionId, "ERROR", "FAILED",
                errorMessage, "System", 0, Instant.now());
    }

    /** Prevents oversized payloads in WebSocket frames. */
    private static String truncate(String text, int maxChars) {
        if (text == null)
            return null;
        return text.length() > maxChars ? text.substring(0, maxChars) + "…" : text;
    }
}
