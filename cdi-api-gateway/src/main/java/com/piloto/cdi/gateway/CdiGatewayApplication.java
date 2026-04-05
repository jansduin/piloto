package com.piloto.cdi.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for CDI API Gateway.
 * 
 * This gateway acts as a communication bridge between UI clients and the CDI
 * Cognitive Kernel.
 * It provides REST endpoints for synchronous commands and WebSocket channels
 * for real-time events.
 * 
 * Architecture:
 * - UI Layer (React/Vue/CLI) ↔ REST + WebSocket
 * - API Gateway (this app) ↔ Java API
 * - CDI Kernel (business logic)
 * 
 * The gateway is STATELESS and contains NO business logic - only translation
 * and routing.
 */
@SpringBootApplication
@EnableAsync // Required for DeliberationEventBroadcaster @Async methods
@EnableScheduling // Required for CEE OptimizerService @Scheduled OPRO cycles
public class CdiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(CdiGatewayApplication.class, args);
    }
}
