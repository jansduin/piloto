package com.piloto.cdi.gateway.controller;

import com.piloto.cdi.gateway.dto.ChatMessageRequest;
import com.piloto.cdi.gateway.dto.ChatMessageResponse;
import com.piloto.cdi.gateway.service.KernelBridgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for chat-related endpoints.
 * 
 * This controller is a PURE PRESENTATION LAYER:
 * - Receives HTTP requests
 * - Validates basic HTTP-level concerns
 * - Delegates to KernelBridgeService
 * - Returns HTTP responses
 * 
 * NO BUSINESS LOGIC HERE - only HTTP/REST concerns.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final KernelBridgeService kernelBridgeService;

    public ChatController(KernelBridgeService kernelBridgeService) {
        this.kernelBridgeService = kernelBridgeService;
    }

    /**
     * Send a message to the CDI Cognitive Kernel.
     * 
     * POST /api/chat/message
     * 
     * @param request Chat message from user
     * @return Response from CDI Kernel
     */
    @PostMapping("/message")
    public ResponseEntity<ChatMessageResponse> sendMessage(@RequestBody ChatMessageRequest request) {
        logger.info("Received message request: session={}, tenant={}",
                request.getSessionId(), request.getTenantId());

        try {
            // Validate request
            kernelBridgeService.validateRequest(request);

            // Process through kernel
            ChatMessageResponse response = kernelBridgeService.processMessage(request);

            if (response.isSuccess()) {
                logger.info("Message processed successfully: messageId={}", response.getMessageId());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Message processing failed: error={}", response.getError());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ChatMessageResponse.error("validation_error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error processing message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ChatMessageResponse.error("server_error", "An unexpected error occurred"));
        }
    }

    /**
     * Health check endpoint for chat service.
     * 
     * GET /api/chat/health
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "chat");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}
