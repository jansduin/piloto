package com.piloto.cdi.kernel.distributed;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface MessageBus {

    CompletableFuture<Void> publish(String topic, Map<String, Object> payload);

    void subscribe(String topic, Consumer<Message> handler);

    CompletableFuture<Void> acknowledge(String messageId);

    CompletableFuture<Void> retry(String messageId);

    class Message {
        private final String messageId;
        private final String topic;
        private final Map<String, Object> payload;
        private final long timestamp;

        public Message(String messageId, String topic, Map<String, Object> payload, long timestamp) {
            this.messageId = messageId;
            this.topic = topic;
            this.payload = payload != null ? Map.copyOf(payload) : Map.of();
            this.timestamp = timestamp;
        }

        public String getMessageId() {
            return messageId;
        }

        public String getTopic() {
            return topic;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format("Message{id='%s', topic='%s', timestamp=%d}",
                messageId, topic, timestamp);
        }
    }
}
