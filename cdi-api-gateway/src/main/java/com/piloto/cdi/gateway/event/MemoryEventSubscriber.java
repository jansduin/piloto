package com.piloto.cdi.gateway.event;

import com.piloto.cdi.kernel.memory.MemoryManager;
import com.piloto.cdi.kernel.types.TenantID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Event-driven memory persistence for CDI.
 * Eliminates state-mutating bypasses in the REST Gateway.
 */
@Component
public class MemoryEventSubscriber {

    private static final Logger logger = LoggerFactory.getLogger(MemoryEventSubscriber.class);
    private final MemoryManager memoryManager;

    public MemoryEventSubscriber(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }

    @Async
    @EventListener
    public void onChatInteractionCompleted(ChatInteractionCompletedEvent event) {
        if (!event.getResponse().success()) {
            return;
        }

        try {
            String content = String.format("USER: %s\nPILOTO: %s",
                    event.getRequest().getMessage(),
                    event.getResponse().content());

            Map<String, String> metadata = new HashMap<>();
            metadata.put("sessionId", event.getRequest().getSessionId());
            metadata.put("tenantId", event.getRequest().getTenantId());

            com.piloto.cdi.kernel.memory.MemoryEntry entry = com.piloto.cdi.kernel.memory.MemoryEntry.create(
                    TenantID.of(event.getRequest().getTenantId()),
                    com.piloto.cdi.kernel.memory.MemoryType.SHORT_TERM,
                    content,
                    metadata,
                    "MemoryEventSubscriber");

            memoryManager.addMemory(entry).get(); // Await persistence
            logger.info("Interaction stored asynchronously in CQRS memory for session: {}",
                    event.getRequest().getSessionId());
        } catch (Exception e) {
            logger.error("Error storing interaction in CQRS memory: {}", e.getMessage());
        }
    }
}
