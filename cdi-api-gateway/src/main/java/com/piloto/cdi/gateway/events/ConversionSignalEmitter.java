package com.piloto.cdi.gateway.events;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ConversionSignalEmitter {

    private final RabbitTemplate rabbitTemplate;
    private static final String EXCHANGE = "vanessa.events";
    private static final String ROUTING_KEY = "conversion.signal";

    public ConversionSignalEmitter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void emit(String tenantId, String userId, String intent, double confidence) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("timestamp", System.currentTimeMillis());
        event.put("type", "ConversionSignalEvent");
        event.put("tenantId", tenantId);
        event.put("payload", Map.of(
            "userId", userId,
            "intent", intent,
            "confidence", confidence,
            "source", "PILOTO_F1_KERNEL"
        ));

        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }
}
