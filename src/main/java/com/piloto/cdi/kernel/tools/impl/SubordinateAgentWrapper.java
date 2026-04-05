package com.piloto.cdi.kernel.tools.impl;

import com.piloto.cdi.kernel.orchestrator.BaseAgent;
import com.piloto.cdi.kernel.tools.BaseTool;
import com.piloto.cdi.kernel.tools.ToolCapability;
import com.piloto.cdi.kernel.tools.ToolDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * SubordinateAgentWrapper acts as a technical bridge.
 * It allows a full Vertical Agent (e.g., Administrative, Concierge)
 * to be registered and executed as a Tool within the Kernel's Toolbox.
 */
public class SubordinateAgentWrapper extends BaseTool {

    private final BaseAgent agent;

    public SubordinateAgentWrapper(BaseAgent agent) {
        super(ToolDefinition.create(
                "agent_vertical_" + agent.getName().toLowerCase().replace(" ", "_"),
                "1.0-CDI",
                "Wrapper de capacidad para el agente vertical: " + agent.getRole(),
                List.of(ToolCapability.EXECUTE, ToolCapability.AI_DELEGATION),
                Map.of(
                        "intent", "String: El objetivo o tarea específica para la vertical",
                        "context", "Map: Datos adicionales requeridos para la ejecución"),
                Map.of(
                        "response", "String: Resultado del razonamiento de la vertical",
                        "confidence", "Double: Nivel de confianza del agente subordinado"),
                true // Mandatorio: Requiere aprobación si la vertical realiza acciones de riesgo
        ));
        this.agent = agent;
    }

    @Override
    public CompletableFuture<Map<String, Object>> execute(Map<String, Object> payload) {
        String intent = (String) payload.getOrDefault("intent", "");

        Map<String, Object> context = new HashMap<>();
        Object providedContext = payload.get("context");
        if (providedContext instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String s) {
                    context.put(s, entry.getValue());
                }
            }
        }
        context.put("vertical_intent", intent);
        context.put("bridged_via", "SubordinateAgentWrapper");

        // Delegación real al agente subordinado
        return agent.think(context).thenApply(result -> {
            Map<String, Object> toolResponse = new HashMap<>();
            toolResponse.put("agent_name", agent.getName());
            toolResponse.put("agent_role", agent.getRole());
            toolResponse.put("output", result.getOrDefault("output", result));
            toolResponse.put("confidence", result.getOrDefault("confidence", 1.0));
            return toolResponse;
        });
    }

    /**
     * Propagación de compensación si la vertical la soporta.
     */
    @Override
    public CompletableFuture<Void> compensate(Map<String, Object> payload) {
        // En una implementación avanzada, el agente vertical podría tener su propio
        // mecanismo de rollback
        return CompletableFuture.completedFuture(null);
    }
}
