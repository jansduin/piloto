package com.piloto.cdi.gateway.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.piloto.cdi.gateway.event.DeliberationStageEvent;

/**
 * Listens to internal Spring DeliberationStageEvents and broadcasts them
 * to connected WebSocket clients via STOMP.
 *
 * <p>
 * <b>Topic:</b> {@code /topic/deliberation/{sessionId}}
 * </p>
 *
 * <p>
 * This component decouples the reasoning engine (pure Java, no Spring deps)
 * from the WebSocket layer. The flow is:
 * </p>
 * <ol>
 * <li>KernelBridgeService publishes {@link DeliberationStageEvent}</li>
 * <li>This listener picks it up asynchronously</li>
 * <li>Converts and broadcasts {@link DeliberationStageMessage} to UI
 * clients</li>
 * </ol>
 *
 * @since Phase 4 WebSocket
 */
@Component
public class DeliberationEventBroadcaster {

    private static final Logger logger = LoggerFactory.getLogger(DeliberationEventBroadcaster.class);
    private static final String TOPIC_PREFIX = "/topic/deliberation/";

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public DeliberationEventBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Receives deliberation stage events from the reasoning pipeline
     * and broadcasts them to the corresponding session's WebSocket topic.
     *
     * @param event deliberation stage event from KernelBridgeService
     */
    @Async
    @EventListener
    public void onDeliberationStage(DeliberationStageEvent event) {
        String destination = TOPIC_PREFIX + event.getSessionId();

        DeliberationStageMessage message = switch (event.getStatus()) {
            case "STARTED" -> DeliberationStageMessage.started(
                    event.getSessionId(), event.getStageName(),
                    event.getAgentRole(), event.getIterationNumber());
            case "FINAL" -> DeliberationStageMessage.finalResponse(
                    event.getSessionId(), event.getContent());
            case "FAILED" -> DeliberationStageMessage.error(
                    event.getSessionId(), event.getContent());
            default -> DeliberationStageMessage.completed(
                    event.getSessionId(), event.getStageName(),
                    event.getContent(), event.getAgentRole(), event.getIterationNumber());
        };

        try {
            messagingTemplate.convertAndSend(destination, message);
            logger.debug("WS broadcast → {} [{}] stage={} status={}",
                    destination, event.getSessionId(), event.getStageName(), event.getStatus());
        } catch (Exception e) {
            // Broadcasting failure must NEVER block the reasoning pipeline
            logger.warn("Non-critical: WebSocket broadcast failed for session {}: {}",
                    event.getSessionId(), e.getMessage());
        }
    }
}
