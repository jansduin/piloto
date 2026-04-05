package com.piloto.cdi.gateway.event;

import com.piloto.cdi.gateway.dto.ChatMessageRequest;
import com.piloto.cdi.gateway.llm.dto.LLMResponse;
import org.springframework.context.ApplicationEvent;

/**
 * Domain-style event triggered when a successful chat interaction completes.
 * Used for CQRS eventual consistency (e.g. updating vector memory).
 */
public class ChatInteractionCompletedEvent extends ApplicationEvent {

    private final ChatMessageRequest request;
    private final LLMResponse response;

    public ChatInteractionCompletedEvent(Object source, ChatMessageRequest request, LLMResponse response) {
        super(source);
        this.request = request;
        this.response = response;
    }

    public ChatMessageRequest getRequest() {
        return request;
    }

    public LLMResponse getResponse() {
        return response;
    }
}
