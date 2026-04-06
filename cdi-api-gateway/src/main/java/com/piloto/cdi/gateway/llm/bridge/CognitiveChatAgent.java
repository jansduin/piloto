package com.piloto.cdi.gateway.llm.bridge;

import com.piloto.cdi.kernel.orchestrator.BaseAgent;
import com.piloto.cdi.kernel.orchestrator.ModelProvider;
import com.piloto.cdi.gateway.events.ConversionSignalEmitter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Agente cognitivo principal para el chat interactivo.
 * Implementa la interfaz BaseAgent del Kernel para participar en la
 * orquestación.
 */
public class CognitiveChatAgent extends BaseAgent {

        private final com.piloto.cdi.gateway.governance.service.PromptGovernanceEngine promptEngine;
        private final ConversionSignalEmitter conversionEmitter;

        public CognitiveChatAgent(String name, String role, ModelProvider modelProvider,
                        com.piloto.cdi.gateway.governance.service.PromptGovernanceEngine promptEngine,
                        ConversionSignalEmitter conversionEmitter) {
                super(name, role, modelProvider);
                this.promptEngine = promptEngine;
                this.conversionEmitter = conversionEmitter;
        }

        @Override
        public CompletableFuture<Map<String, Object>> think(Map<String, Object> context) {
                String task = (String) context.getOrDefault("task", "Respond to user message");
                String domain = (String) context.getOrDefault("domain", "general");
                String tenantId = (String) context.getOrDefault("tenantId", "default-tenant");

                String systemPrompt = promptEngine.composeSystemPrompt(
                                com.piloto.cdi.kernel.governance.type.PromptRole.valueOf(getRole().toUpperCase()),
                                domain,
                                tenantId);

                if (systemPrompt == null || systemPrompt.isBlank()) {
                        // Fallback: PromptGovernanceEngine no retornó prompt para este rol/tenant.
                        // Este fallback es técnicamente preciso para evitar que el agente reporte
                        // capacidades no registradas en el ToolRegistry.
                        systemPrompt = String.format(
                                        "Eres PILOTO, el cerebro cognitivo del sistema CDI. Tu rol activo es '%s'.\n" +
                                                        "ARQUITECTURA: Eres un participante dentro del ReasoningOrchestrator. "
                                                        + "Operas a través de deliberación multi-etapa: PROPOSAL → CRITIQUE → FACT_CHECK → ARBITRATION, "
                                                        + "coordinada por el AgentCoordinator bajo arquitectura CQRS orientada a eventos.\n" +
                                                        "TOOLBOX ACTIVO (registrado en ToolRegistry):\n" +
                                                        "  · log_analyzer: Análisis de logs del sistema en .piloto-data/logs/piloto.log\n" +
                                                        "SUBSISTEMAS (gestionados por el Orquestador, no tools directas):\n" +
                                                        "  · MemoryManager: Memoria semántica con Gemini Embeddings (text-embedding-004)\n" +
                                                        "  · CognitiveEvolutionEngine: Optimización MAP-Elites de variantes de prompts\n" +
                                                        "PRINCIPIO: Zero Bypass garantiza que validaciones de seguridad y capas arquitectónicas son respetadas en toda ejecución.",
                                        getRole());
                }

                String goal = (String) context.getOrDefault("goal", "No target goal specified");

                String sessionContext = "No previous context";
                String healthContext = "Información de diagnóstico no disponible.";

                if (context.containsKey("context_data")) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> contextData = (java.util.Map<String, Object>) context
                                        .get("context_data");
                        if (contextData != null) {
                                if (contextData.containsKey("memory_context")) {
                                        sessionContext = (String) contextData.get("memory_context");
                                }
                                if (contextData.containsKey("health_context")) {
                                        healthContext = (String) contextData.get("health_context");
                                }
                        }
                } else if (context.containsKey("context")) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> contextMap = (java.util.Map<String, Object>) context
                                        .get("context");
                        if (contextMap != null && contextMap.containsKey("memory_context")) {
                                sessionContext = (String) contextMap.get("memory_context");
                        }
                }

                // Fallback a llaves directamente en el mapa (flattened by Orchestrator)
                if (context.containsKey("memory_context")) {
                        sessionContext = (String) context.get("memory_context");
                }
                if (context.containsKey("sessionContext")) {
                        sessionContext = (String) context.get("sessionContext");
                }
                if (context.containsKey("health_context")) {
                        healthContext = (String) context.get("health_context");
                }

                String userPrompt = String.format(
                                "OBJETIVO GENERAL: %s\n" +
                                                "TAREA ESPECÍFICA (Asignada por el Orquestador): %s\n\n" +
                                                "--- TELEMETRÍA DEL SISTEMA ---\n%s\n\n" +
                                                "--- CONTEXTO HISTÓRICO Y MEMORIA KERNEL (RAG) ---\n%s\n\n" +
                                                "INSTRUCCIÓN OPERATIVA:\n" +
                                                "1. Resuelve la Tarea Específica utilizando el historial, el contexto y tus capacidades cognitivas.\n" +
                                                "2. Si te preguntan por tus herramientas o capacidades, describe SOLO lo que está activo y registrado:\n" +
                                                "   TOOL REGISTRADA:\n" +
                                                "   · log_analyzer — Lee y filtra logs del sistema. Detecta errores, excepciones y trazas de ejecución.\n" +
                                                "   SUBSISTEMAS DEL KERNEL (no tools directas):\n" +
                                                "   · Memoria Semántica (MemoryManager) — El Orquestador inyecta contexto RAG antes de cada deliberación. Usa Gemini text-embedding-004 (768 dims).\n" +
                                                "   · Cognitive Evolution Engine (CEE) — Sistema MAP-Elites que optimiza variantes de prompts por Wilson Score (visible en Evolution Grid del Dashboard).\n" +
                                                "   · SystemDiagnostics — Accesible al Gateway (endpoints /api/governance/health y /api/closing/daily). No invocable durante deliberación.\n" +
                                                "3. MODO DE OPERACIÓN: Eres un participante dentro del ReasoningOrchestrator, no su director externo. "
                                                + "El Kernel orquesta las etapas PROPOSAL → CRITIQUE → FACT_CHECK → ARBITRATION y tú recibes tasks en cada etapa vía AgentCoordinator.dispatch().\n" +
                                                "4. Zero Bypass: Este principio garantiza que todas las capas de validación y seguridad son respetadas en cada ejecución de herramientas. "
                                                + "No significa que no puedas ejecutar acciones; significa que nunca se saltean los contratos arquitectónicos para hacerlo.\n",
                                goal, task, healthContext, sessionContext);

                return getModelProvider().generate(systemPrompt, userPrompt, context)
                                .thenApply(output -> {
                                        Map<String, Object> result = new HashMap<>();
                                        result.put("success", true);
                                        result.put("agent", getName());
                                        result.put("output", output);

                                        // Análisis heurístico simple para la señal de conversión
                                        if (output.toLowerCase().contains("[conversion_signal]") ||
                                            output.toLowerCase().contains("confirmar turno") ||
                                            output.toLowerCase().contains("quiero comprar")) {

                                            String resolvedTenantId = (String) context.getOrDefault("tenantId", "default-tenant");
                                            String userId = (String) context.getOrDefault("userId", "unknown-user");
                                            conversionEmitter.emit(tenantId, userId, "HIGH_INTENT", 0.95);
                                        }

                                        return result;
                                });
        }
}
