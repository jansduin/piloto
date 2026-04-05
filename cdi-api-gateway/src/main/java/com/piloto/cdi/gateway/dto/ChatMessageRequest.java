package com.piloto.cdi.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for incoming chat messages from the UI.
 * 
 * This is a pure data transfer object with NO business logic.
 * It represents a user's message to the CDI Cognitive Kernel.
 */
public class ChatMessageRequest {

    @JsonProperty("message")
    private String message;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("domain")
    private String domain;

    @JsonProperty("executionMode")
    private com.piloto.cdi.kernel.types.ExecutionMode executionMode;

    // Constructors
    public ChatMessageRequest() {
    }

    public ChatMessageRequest(String message, String sessionId, String tenantId) {
        this.message = message;
        this.sessionId = sessionId;
        this.tenantId = tenantId;
        this.executionMode = com.piloto.cdi.kernel.types.ExecutionMode.NORMAL; // Default
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public com.piloto.cdi.kernel.types.ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(com.piloto.cdi.kernel.types.ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    @Override
    public String toString() {
        return "ChatMessageRequest{" +
                "message='" + message + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                '}';
    }
}
