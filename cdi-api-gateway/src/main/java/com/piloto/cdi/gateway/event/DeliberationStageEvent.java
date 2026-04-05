package com.piloto.cdi.gateway.event;

import org.springframework.context.ApplicationEvent;

/**
 * Spring ApplicationEvent fired after each deliberation stage completes.
 *
 * <p>
 * Published by {@code KernelBridgeService} during the reasoning pipeline,
 * consumed by {@code DeliberationEventBroadcaster} to stream progress to UI.
 * </p>
 *
 * <p>
 * <b>Stage lifecycle:</b>
 * </p>
 * <ol>
 * <li>STARTED — stage begins (no content yet)</li>
 * <li>COMPLETED — stage finished with result content</li>
 * <li>FINAL — entire deliberation done, final answer ready</li>
 * <li>FAILED — error in stage or deliberation</li>
 * </ol>
 *
 * @since Phase 4 WebSocket
 */
public class DeliberationStageEvent extends ApplicationEvent {

    private final String sessionId;
    private final String stageName;
    private final String status; // STARTED | COMPLETED | FINAL | FAILED
    private final String content;
    private final String agentRole;
    private final int iterationNumber;

    private DeliberationStageEvent(Object source, String sessionId, String stageName,
            String status, String content, String agentRole, int iterationNumber) {
        super(source);
        this.sessionId = sessionId;
        this.stageName = stageName;
        this.status = status;
        this.content = content;
        this.agentRole = agentRole;
        this.iterationNumber = iterationNumber;
    }

    /** STARTED event: stage begins. */
    public static DeliberationStageEvent started(Object source, String sessionId,
            String stageName, String agentRole, int iteration) {
        return new DeliberationStageEvent(source, sessionId, stageName, "STARTED",
                null, agentRole, iteration);
    }

    /** COMPLETED event: stage finished with content. */
    public static DeliberationStageEvent completed(Object source, String sessionId,
            String stageName, String content, String agentRole, int iteration) {
        return new DeliberationStageEvent(source, sessionId, stageName, "COMPLETED",
                content, agentRole, iteration);
    }

    /** FINAL event: deliberation complete, final answer ready. */
    public static DeliberationStageEvent finalAnswer(Object source, String sessionId, String content) {
        return new DeliberationStageEvent(source, sessionId, "FINAL_RESPONSE", "FINAL",
                content, "System", 0);
    }

    /** FAILED event: error during deliberation. */
    public static DeliberationStageEvent failed(Object source, String sessionId, String errorMessage) {
        return new DeliberationStageEvent(source, sessionId, "ERROR", "FAILED",
                errorMessage, "System", 0);
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getStageName() {
        return stageName;
    }

    public String getStatus() {
        return status;
    }

    public String getContent() {
        return content;
    }

    public String getAgentRole() {
        return agentRole;
    }

    public int getIterationNumber() {
        return iterationNumber;
    }
}
