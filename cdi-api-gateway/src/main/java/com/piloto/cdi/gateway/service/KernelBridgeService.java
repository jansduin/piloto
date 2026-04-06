package com.piloto.cdi.gateway.service;

import com.piloto.cdi.gateway.dto.ChatMessageRequest;
import com.piloto.cdi.gateway.dto.ChatMessageResponse;
import com.piloto.cdi.gateway.event.DeliberationStageEvent;
import com.piloto.cdi.gateway.llm.LLMRole;
import com.piloto.cdi.gateway.llm.LLMRoleManager;
import com.piloto.cdi.gateway.llm.dto.LLMRequest;
import com.piloto.cdi.gateway.llm.dto.LLMResponse;
import com.piloto.cdi.kernel.command.BaseCommand;
import com.piloto.cdi.kernel.command.Command;
import com.piloto.cdi.kernel.command.CommandType;
import com.piloto.cdi.kernel.executive.ExecutiveController;
import com.piloto.cdi.kernel.executive.ExecutionResult;
import com.piloto.cdi.kernel.model.DomainEvent;
import com.piloto.cdi.kernel.service.EventApplicationService;
import com.piloto.cdi.kernel.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service that bridges the API Gateway with the CDI Cognitive Kernel.
 * 
 * ARCHITECTURE PRINCIPLE: This service is a PURE ADAPTER.
 * - It translates REST DTOs → Kernel API calls
 * - It does NOT contain business logic
 * - It does NOT modify the kernel's behavior
 * - It acts as a stateless translator
 * 
 * ENTERPRISE INTEGRATION (Phase 1 - REAL):
 * - Injects ExecutiveController from Spring context
 * - Translates ChatMessageRequest → Command
 * - Invokes ExecutiveController.execute()
 * - Translates ExecutionResult → ChatMessageResponse
 * - Full event sourcing and state management
 */
@Service
public class KernelBridgeService {

    private static final Logger logger = LoggerFactory.getLogger(KernelBridgeService.class);

    private final ExecutiveController executiveController;
    private final LLMRoleManager llmRoleManager;
    private final EventApplicationService eventApplicationService;
    private final com.piloto.cdi.kernel.memory.MemoryManager memoryManager;
    private final com.piloto.cdi.kernel.orchestrator.ReasoningOrchestrator orchestrator;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    /**
     * Constructor con inyección de dependencias completa (Cerebro CDI Conectado -
     * Fase 6).
     * Nota: SystemDiagnostics y ToolExecutionEngine han sido eliminados del Gateway
     * para impedir "Bypass". Toda ejecución se delega al Orquestador (Fase 6).
     */
    @Autowired
    public KernelBridgeService(ExecutiveController executiveController,
            LLMRoleManager llmRoleManager,
            EventApplicationService eventApplicationService,
            com.piloto.cdi.kernel.memory.MemoryManager memoryManager,
            com.piloto.cdi.kernel.orchestrator.ReasoningOrchestrator orchestrator,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.executiveController = executiveController;
        this.llmRoleManager = llmRoleManager;
        this.eventApplicationService = eventApplicationService;
        this.memoryManager = memoryManager;
        this.orchestrator = orchestrator;
        this.eventPublisher = eventPublisher;
        logger.info("KernelBridgeService (Pure Adapter) initialized with Full CDI Pipeline");
    }

    /**
     * Process a chat message through the CDI Kernel.
     * 
     * This method:
     * 1. Receives a user message (DTO)
     * 2. Translates it to CDI Kernel Command
     * 3. Invokes the ExecutiveController
     * 4. Translates the ExecutionResult back to DTO
     * 
     * @param request User's chat message
     * @return Kernel's response
     */
    public ChatMessageResponse processMessage(ChatMessageRequest request) {
        validateRequest(request);

        logger.info("Processing message for session: {}, tenant: {}",
                request.getSessionId(), request.getTenantId());

        try {
            // Step 1: Translate ChatMessageRequest → Command
            Command command = translateRequestToCommand(request);

            // Step 2: Meta-Command Pre-Processing (Optional Context/Policy checks)
            // No deliberative council here anymore - everything is internal to the
            // Orchestrator

            // Step 3: Execute command through kernel
            long startTime = System.currentTimeMillis();
            ExecutionResult result = executiveController.execute(command);
            long executionTime = System.currentTimeMillis() - startTime;

            // Step 4: Retrieve Session and Historical Context (Memory RAG - Phase 5)
            logger.info("Retrieving memory context for session: {}", request.getSessionId());
            String unifiedContext = retrieveContextWithLongTermMemory(TenantID.of(request.getTenantId()),
                    request.getSessionId(), request.getMessage());

            // Step 5: Execute Reasoning Loop (Cognitive Orchestration - Phase 6)
            logger.info("Initiating Reasoning Orchestrator for goal: {}", request.getMessage());

            Map<String, Object> contextData = new HashMap<>();
            contextData.put("memory_context", unifiedContext);

            // WS Phase 4: Signal deliberation started to connected clients
            eventPublisher.publishEvent(DeliberationStageEvent.started(
                    this, request.getSessionId(), "DELIBERATION", "ReasoningOrchestrator", 1));

            Map<String, Object> reasoningResult;
            try {
                reasoningResult = orchestrator
                        .runWithContext(TenantID.of(request.getTenantId()), request.getMessage(), contextData).get();
            } catch (Exception orchEx) {
                // WS: notify error to connected clients
                eventPublisher.publishEvent(DeliberationStageEvent.failed(
                        this, request.getSessionId(), orchEx.getMessage()));
                throw orchEx;
            }

            logger.info("Reasoning Orchestrator finished. Success: {}, Iterations: {}, Score: {}",
                    reasoningResult.get("success"), reasoningResult.get("iterations"), reasoningResult.get("score"));

            // WS Phase 4: Replay deliberation history as individual stage events
            broadcastDeliberationHistory(request.getSessionId(), reasoningResult);

            if (reasoningResult.containsKey("error")) {
                logger.warn("Reasoning Error: {}", reasoningResult.get("error"));
            }
            LLMResponse llmResponse = translateReasoningToLLMResponse(reasoningResult);

            // Step 7: CQRS Event Publication for Async Memory Storage
            // Desvincular persistencia de la memoria hacia la arquitectura orientada a
            // Eventos
            eventPublisher.publishEvent(
                    new com.piloto.cdi.gateway.event.ChatInteractionCompletedEvent(this, request, llmResponse));

            // Step 8: Translate ExecutionResult + LLMResponse → ChatMessageResponse
            ChatMessageResponse response = translateResultToResponse(result, request, executionTime, llmResponse);

            // Inject Detailed Reasoning Trace for UI (Diagnostic Visibility)
            injectReasoningTrace(response, reasoningResult);

            return response;

        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage(), e);
            return ChatMessageResponse.error(
                    "msg_error_" + UUID.randomUUID().toString().substring(0, 8),
                    "Internal error: " + e.getMessage());
        }
    }

    /**
     * Translate ChatMessageRequest to CDI Command.
     * 
     * For Phase 1: All chat messages create CREATE_GOAL commands.
     * Future phases will parse intent and route to appropriate command types.
     * 
     * @param request chat message request
     * @return Command for kernel execution
     */
    private Command translateRequestToCommand(ChatMessageRequest request) {
        // Generate goal ID for tracking
        String goalId = "goal_" + UUID.randomUUID().toString().substring(0, 12);

        // Build command payload
        Map<String, Object> payload = Map.of(
                "goalId", goalId,
                "description", request.getMessage(),
                "sessionId", request.getSessionId(),
                "originalMessage", request.getMessage());

        // Create command with tenant context
        return BaseCommand.create(
                TenantID.of(request.getTenantId()),
                CommandType.CREATE_GOAL,
                payload);
    }

    /**
     * Translates the internal execution result into a user-facing DTO.
     */
    private ChatMessageResponse translateResultToResponse(
            ExecutionResult result,
            ChatMessageRequest originalRequest,
            long executionTimeMs,
            LLMResponse llmResponse) {

        if (!result.isSuccess()) {
            // Execution failed - return error response
            String errorMessage = "Command execution failed: " + String.join("; ", result.getErrors());
            logger.warn("Kernel execution failed: {}", errorMessage);

            ChatMessageResponse errorResponse = ChatMessageResponse.error(
                    "msg_error_" + UUID.randomUUID().toString().substring(0, 8),
                    errorMessage);

            return errorResponse;
        }

        // Success - extract information from result
        String messageId = "msg_" + UUID.randomUUID().toString().substring(0, 8);

        // Extract goalId from command payload (we put it there)
        String goalId = result.getEmittedEvent()
                .flatMap(event -> Optional.ofNullable(event.getMetadata().get("goalId")))
                .orElse("unknown");

        // Build response message
        String responseMessage = llmResponse.success() ? llmResponse.content()
                : buildSuccessResponseMessage(result, originalRequest);

        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("executionTimeMs", executionTimeMs);
        metadata.put("mode", "real-kernel-with-llm");
        metadata.put("llmProvider", llmResponse.providerName());
        metadata.put("llmModel", llmResponse.modelName());
        metadata.put("stateVersion", result.getNewVersion().toString());
        metadata.put("eventEmitted", result.getEmittedEvent().isPresent());
        metadata.put("timestamp", System.currentTimeMillis());

        if (llmResponse.latencyMs() > 0) {
            metadata.put("llmLatencyMs", llmResponse.latencyMs());
        }

        logger.debug("Successfully processed message: {} (goal: {}, execution: {}ms, llm: {}ms)",
                messageId, goalId, executionTimeMs, llmResponse.latencyMs());

        return ChatMessageResponse.success(messageId, responseMessage, goalId, metadata);
    }

    /**
     * Generates a natural language response using the LLM with session context.
     */
    private LLMResponse generateLLMResponse(ExecutionResult result, ChatMessageRequest request, String sessionContext) {
        String eventType = result.getEmittedEvent().map(e -> e.getType().name()).orElse("NONE");

        String systemPrompt = "Eres PILOTO, el cerebro cognitivo de una infraestructura CDI (Contract-Driven Infrastructure). "
                + "TU IDENTIDAD: Eres un participante activo dentro del ReasoningOrchestrator. Operas en deliberación multi-etapa "
                + "(PROPOSAL → CRITIQUE → FACT_CHECK → ARBITRATION) coordinada por el AgentCoordinator. "
                + "TU TOOLBOX ACTIVO (herramientas realmente registradas en el ToolRegistry): "
                + "  · log_analyzer: Lee y filtra los logs del sistema en .piloto-data/logs/piloto.log. Detecta errores y trazas de ejecución. "
                + "SUBSISTEMAS DE SOPORTE (no son tools directas, son capacidades del Kernel que el Orquestador gestiona): "
                + "  · Memoria Semántica (MemoryManager): Consulta automática de historial de sesión (corto plazo) y conocimiento histórico (largo plazo) vía Gemini Embeddings (text-embedding-004, 768 dims). "
                + "  · Cognitive Evolution Engine (CEE): Sistema MAP-Elites que optimiza variantes de prompts por celda comportamental usando Wilson Score. "
                + "  · SystemDiagnostics: Accesible al Gateway (HealthController, DailyClosingController), no invocable directamente por el agente en tiempo de deliberación. "
                + "ARQUITECTURA: El principio Zero Bypass garantiza que todas las validaciones de seguridad y capas arquitectónicas son respetadas en cada ejecución. "
                + "RESPUESTAS: Sé técnico, preciso y coherente con las capacidades reales del sistema. No describas herramientas que no estén registradas.";

        String prompt = String.format(
                "CONTEXTO UNIFICADO (HISTORIAL RECIENTE + RECUERDOS RELEVANTES):\n%s\n\n" +
                        "MENSAJE ACTUAL DEL USUARIO: \"%s\"\n\n" +
                        "CONTEXTO DEL KERNEL (referencia operativa):\n" +
                        "- Evento procesado: %s\n" +
                        "- Versión de estado: %s\n\n" +
                        "INSTRUCCIÓN: Considerando el historial previo, responde al mensaje actual de forma completa y coherente.",
                sessionContext, request.getMessage(), eventType, result.getNewVersion());

        return llmRoleManager.executeForRole(LLMRole.CHAT_AGENT, new LLMRequest(prompt, systemPrompt));
    }

    /**
     * Recupera el contexto unificado: Historial corto (Session) + Memoria Larga
     * (Semántica).
     */
    private String retrieveContextWithLongTermMemory(TenantID tenantId, String sessionId, String userMessage) {
        StringBuilder unifiedContext = new StringBuilder();

        try {
            // 1. Memoria a Corto Plazo (Conversación Inmediata de esta Sesión)
            com.piloto.cdi.kernel.memory.MemoryResult shortTermMemory = memoryManager
                    .getRecentSessionMemory(tenantId, sessionId, 10).get();

            unifiedContext.append("--- INICIO HISTORIAL DE ESTA SESIÓN RECIENTE ---\n");
            if (shortTermMemory.isEmpty()) {
                unifiedContext.append("[Nueva sesión, sin historial propio aún]\n");
            } else {
                unifiedContext.append(shortTermMemory.getAggregatedContext()).append("\n");
            }
            unifiedContext.append("--- FIN HISTORIAL SESIÓN ---\n\n");

            // 2. Memoria a Largo Plazo (Conocimiento Histórico Evolutivo)
            com.piloto.cdi.kernel.memory.MemoryQuery longTermQuery = com.piloto.cdi.kernel.memory.MemoryQuery.create(
                    tenantId,
                    userMessage,
                    15 // Traer los 15 recuerdos más relevantes (LLM soporta gran contexto)
            );
            com.piloto.cdi.kernel.memory.MemoryResult longTermMemory = memoryManager.queryMemory(longTermQuery).get();

            unifiedContext.append("--- INICIO RECUERDOS HISTÓRICOS RELEVANTES (LONG-TERM) ---\n");
            if (longTermMemory.isEmpty()) {
                unifiedContext.append("[No hay memorias previas directamente relevantes para esta consulta]\n");
            } else {
                for (com.piloto.cdi.kernel.memory.MemoryEntry entry : longTermMemory.getEntries()) {
                    // Evitar duplicar exactamente los mismos mensajes que leemos en la sesión corta
                    // comprobando los IDs
                    unifiedContext.append("- Recuerdo [RAG Score: ").append(entry.getRelevanceScore()).append("]:\n")
                            .append(entry.getContent()).append("\n\n");
                }
            }
            unifiedContext.append("--- FIN RECUERDOS HISTÓRICOS ---\n");

            return unifiedContext.toString();

        } catch (Exception e) {
            logger.error("Error retrieving unified memory context: {}", e.getMessage());
            return "[Error al recuperar el historial y vectores de memoria]";
        }
    }

    /**
     * Translate ReasoningOrchestrator result to LLMResponse format for
     * compatibility.
     */
    private LLMResponse translateReasoningToLLMResponse(Map<String, Object> reasoningResult) {
        boolean success = (boolean) reasoningResult.getOrDefault("success", false);
        if (!success) {
            String error = (String) reasoningResult.getOrDefault("error", "Reasoning failed");
            return LLMResponse.failure("KernelOrchestrator", "none", error);
        }

        // Extract output from last successful iteration or finalization
        Object rawOutput = reasoningResult.get("output");
        String content = "No output generated by reasoning engine.";

        if (rawOutput != null) {
            // First check if output is directly a String (simple chat response)
            if (rawOutput instanceof String) {
                content = (String) rawOutput;
            } else if (rawOutput instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> outputMap = (Map<String, Object>) rawOutput;
                // Find any agent output in the intermediate results
                for (Map.Entry<String, Object> entry : outputMap.entrySet()) {
                    if (entry.getKey().startsWith("task_")) {
                        Object entryValue = entry.getValue();
                        if (entryValue instanceof Map<?, ?>) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> taskResult = (Map<String, Object>) entryValue;
                            if (taskResult.containsKey("output")) {
                                content = String.valueOf(taskResult.get("output"));
                            } else if (taskResult.containsKey("final_decision")) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> decision = (Map<String, Object>) taskResult.get("final_decision");
                                if (decision.containsKey("output")) {
                                    content = String.valueOf(decision.get("output"));
                                }
                            }
                        }
                    }
                }
            }
        }

        return new LLMResponse(true, content, new LLMResponse.Usage(0, 0.0, 0),
                Map.of("provider", "KernelOrchestrator", "model", "Multi-Agent-Council"), null);
    }

    /**
     * Inject reasoning trace into ChatMessageResponse metadata.
     */
    private void injectReasoningTrace(ChatMessageResponse response, Map<String, Object> reasoningResult) {
        if (response.getMetadata() == null) {
            response.setMetadata(new HashMap<>());
        }

        Map<String, Object> metadata = response.getMetadata();
        metadata.put("deliberationTrace", reasoningResult.get("trace"));
        metadata.put("reasoningScore", reasoningResult.get("score"));
        metadata.put("iterations", reasoningResult.get("iterations"));
        metadata.put("completedTasks", reasoningResult.get("completed_tasks"));
    }

    // Método storeInteractionInMemory eliminado por violar arquitectura CQRS (State
    // Bypass). La lógica ahora se delega a MemoryEventSubscriber mediado por
    // eventos.

    /**
     * Build a human-readable success response message.
     * 
     * @param result  execution result
     * @param request original request
     * @return response message
     */
    private String buildSuccessResponseMessage(ExecutionResult result, ChatMessageRequest request) {
        StringBuilder message = new StringBuilder();

        message.append("✅ PILOTO ha procesado tu mensaje con éxito.\n\n");
        message.append("📝 Mensaje recibido: \"").append(request.getMessage()).append("\"\n\n");

        if (result.getEmittedEvent().isPresent()) {
            message.append("🎯 Evento generado: ").append(result.getEmittedEvent().get().getType()).append("\n");
        }

        message.append("🔄 Estado del sistema actualizado exitosamente\n");
        message.append("📊 Versión: ").append(result.getNewVersion()).append("\n\n");
        message.append("✨ CDI Kernel Enterprise - Ready for production");

        return message.toString();
    }

    /**
     * Validate a chat message request.
     * 
     * @param request Request to validate
     * @throws IllegalArgumentException if validation fails
     */
    // Métodos `isHealthCheckRequest` y `performHealthCheck` eliminados por
    // contravenir la filosofía CDI (Zero Bypass). Flujo centralizado enteramente en
    // ExecController y Orquestador.

    public void validateRequest(ChatMessageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        if (request.getSessionId() == null || request.getSessionId().trim().isEmpty()) {
            throw new IllegalArgumentException("SessionId cannot be empty");
        }
        if (request.getTenantId() == null || request.getTenantId().trim().isEmpty()) {
            throw new IllegalArgumentException("TenantId cannot be empty");
        }
    }

    /**
     * Phase 4 WebSocket: broadcasts each deliberation stage to connected clients.
     *
     * <p>
     * Reads {@code deliberation_history} from the orchestrator result and emits
     * one {@link DeliberationStageEvent} per stage, then a FINAL event with the
     * answer.
     * Non-critical: failures here must never block the REST response.
     * </p>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void broadcastDeliberationHistory(String sessionId, Map<String, Object> reasoningResult) {
        try {
            Object historyObj = reasoningResult.get("deliberation_history");
            if (historyObj instanceof List<?> rawList) {
                int index = 0;
                for (Object item : rawList) {
                    if (item instanceof Map rawStageMap) {
                        Map<Object, Object> stageMap = (Map<Object, Object>) rawStageMap;
                        String stageName = String.valueOf(
                                stageMap.getOrDefault("stage", "UNKNOWN"));
                        Object contentObj = stageMap.get("content");
                        String content;
                        if (contentObj instanceof Map rawContentMap) {
                            Map<Object, Object> contentMap = (Map<Object, Object>) rawContentMap;
                            Object output = contentMap.get("output");
                            content = output != null ? String.valueOf(output) : contentMap.toString();
                        } else {
                            content = String.valueOf(contentObj);
                        }

                        // Map deliberation stage names to agent roles
                        String role = switch (stageName) {
                            case "PROPOSAL" -> "Proposer";
                            case "CRITIQUE" -> "Critic";
                            case "FACT_CHECK" -> "Verifier";
                            case "ARBITRATION" -> "Arbiter";
                            default -> stageName;
                        };

                        eventPublisher.publishEvent(DeliberationStageEvent.completed(
                                this, sessionId, stageName, content, role, ++index));
                    }
                }
            }

            // Emit FINAL with the concluded answer
            Object finalDecision = reasoningResult.get("final_decision");
            if (finalDecision != null) {
                String finalContent;
                if (finalDecision instanceof Map rawFd) {
                    Map<Object, Object> fd = (Map<Object, Object>) rawFd;
                    Object output = fd.get("output");
                    finalContent = output != null ? String.valueOf(output) : finalDecision.toString();
                } else {
                    finalContent = String.valueOf(finalDecision);
                }
                eventPublisher.publishEvent(DeliberationStageEvent.finalAnswer(this, sessionId, finalContent));
            }
        } catch (Exception e) {
            // WS broadcast failure is non-critical — log and continue
            logger.warn("Non-critical: failed to broadcast deliberation history for session {}: {}",
                    sessionId, e.getMessage());
        }
    }
}
