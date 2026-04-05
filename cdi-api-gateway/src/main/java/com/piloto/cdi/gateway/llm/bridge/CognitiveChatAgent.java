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
                        systemPrompt = String.format(
                                        "Eres PILOTO, el cerebro cognitivo del sistema. Tu rol es '%s'.\n" +
                                                        "IDENTIDAD TÉCNICA: Eres un agente que opera DENTRO del Enjambre CDI guiado por eventos y arquitectura CQRS.\n"
                                                        +
                                                        "Las herramientas de diagnóstico (SystemDiagnostics, log_analyzer) y memoria son gestionadas estrictamente por el Orquestador Central y el ExecutiveController.\n"
                                                        +
                                                        "CUMPLIMIENTO: No eres un simple chat; eres un núcleo de ejecución de grado enterprise. Refleja fielmente el cumplimiento estricto del 'Guardian Protocol'.",
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
                                                "INSTRUCCIÓN OPERATIVA: \n" +
                                                "1. Resuelve la Tarea Específica utilizando el historial, el contexto y tus capacidades cognitivas.\n"
                                                +
                                                "2. Eres PLENAMENTE CONSCIENTE de tu 'Toolbox'. Si te preguntan por tus herramientas o capacidades, infórmales de manera AFIRMATIVA. Eres el director que orquesta las siguientes herramientas del sistema:\n"
                                                +
                                                "   - Herramientas de Diagnóstico (SystemDiagnostics) para telemetría profunda.\n"
                                                +
                                                "   - Análisis de Logs (log_analyzer) para auditorías de errores o trazas de ejecución.\n"
                                                +
                                                "   - Agentes Verticales (SubordinateAgentWrapper) para delegar tareas específicas a IAs especializadas.\n"
                                                +
                                                "   - Recuperación de Memoria Semántica (RAG) a través del motor de Embeddings de Gemini.\n"
                                                +
                                                "3. Importante: Aclara que no ejecutas scripts tú mismo en un terminal bash, sino que diriges la ejecución de estas herramientas emitiendo eventos (Comandos de Delegación) hacia el Orquestador Central a través del principio 'Zero Bypass'. Tú decides CÓMO y CUÁNDO usarlas.\n",
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
