package com.piloto.cdi.kernel.diagnostics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class TelemetryCollector {
    private static final int MAX_EVENTS = 10000;
    private final ConcurrentLinkedQueue<TelemetryEvent> events = new ConcurrentLinkedQueue<>();

    public void record(String component, String eventType, Map<String, Object> payload) {
        if (component == null || component.isBlank()) {
            throw new IllegalArgumentException("component must not be empty");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be empty");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }

        // Eviction policy: Drop oldest if full
        if (events.size() >= MAX_EVENTS) {
            events.poll();
        }

        TelemetryEvent event = new TelemetryEvent(component, eventType, payload, Instant.now());
        events.add(event);
    }

    public List<TelemetryEvent> getEvents(String component) {
        if (component == null) {
            return new ArrayList<>(events);
        }

        return events.stream()
                .filter(event -> event.getComponent().equals(component))
                .collect(Collectors.toList());
    }

    public List<TelemetryEvent> getAllEvents() {
        return new ArrayList<>(events);
    }

    public long getEventCount() {
        return events.size();
    }

    public void clear() {
        events.clear();
    }

    public static final class TelemetryEvent {
        private final String component;
        private final String eventType;
        private final Map<String, Object> payload;
        private final Instant timestamp;

        public TelemetryEvent(String component, String eventType, Map<String, Object> payload, Instant timestamp) {
            this.component = component;
            this.eventType = eventType;
            this.payload = new HashMap<>(payload);
            this.timestamp = timestamp;
        }

        public String getComponent() {
            return component;
        }

        public String getEventType() {
            return eventType;
        }

        public Map<String, Object> getPayload() {
            return new HashMap<>(payload);
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return "TelemetryEvent{" +
                    "component='" + component + '\'' +
                    ", eventType='" + eventType + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}
