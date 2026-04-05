package com.piloto.cdi.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * DTO for chat message responses to the UI.
 * 
 * This represents the result of processing a user's message through the CDI
 * Kernel.
 * Contains the response text plus metadata about execution.
 */
public class ChatMessageResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("response")
    private String response;

    @JsonProperty("goalId")
    private String goalId;

    @JsonProperty("error")
    private String error;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public ChatMessageResponse() {
    }

    public ChatMessageResponse(boolean success, String messageId, String response, String goalId) {
        this.success = success;
        this.messageId = messageId;
        this.response = response;
        this.goalId = goalId;
    }

    // Static factory methods for common cases
    public static ChatMessageResponse success(String messageId, String response, String goalId,
            Map<String, Object> metadata) {
        ChatMessageResponse dto = new ChatMessageResponse(true, messageId, response, goalId);
        dto.setMetadata(metadata);
        return dto;
    }

    public static ChatMessageResponse error(String messageId, String errorMessage) {
        ChatMessageResponse dto = new ChatMessageResponse();
        dto.setSuccess(false);
        dto.setMessageId(messageId);
        dto.setError(errorMessage);
        return dto;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getGoalId() {
        return goalId;
    }

    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "ChatMessageResponse{" +
                "success=" + success +
                ", messageId='" + messageId + '\'' +
                ", response='" + response + '\'' +
                ", goalId='" + goalId + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
