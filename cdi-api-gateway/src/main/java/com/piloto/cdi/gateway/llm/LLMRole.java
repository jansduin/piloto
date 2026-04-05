package com.piloto.cdi.gateway.llm;

/**
 * Roles específicos que un LLM puede cumplir en el ecosistema CDI.
 */
public enum LLMRole {
    /**
     * Encargado de planificar objetivos y descomponer tareas complejas.
     */
    PLANNER("Strategic planning and goal decomposition"),

    /**
     * Encargado de revisar código, seguridad y calidad técnica.
     */
    CODE_REVIEWER("Code quality and security review"),

    /**
     * Interfaz principal de conversación con el usuario.
     */
    CHAT_AGENT("Interactive user conversation"),

    /**
     * Validador de inputs, outputs y consistencia técnica (fast & cheap).
     */
    VALIDATOR("Technical validation and verification"),

    /**
     * Análisis semántico de intenciones y clasificación de datos.
     */
    SEMANTIC_ANALYZER("Semantic understanding and classification"),

    /**
     * Rol genérico de respaldo cuando falla el primario.
     */
    FALLBACK("Generic fallback for all roles"),

    /**
     * Propone planes de acción y declara supuestos.
     */
    PROPOSER("Proposes actions, plans and explicit assumptions"),

    /**
     * Busca fallas lógicas y ambigüedades en las propuestas.
     */
    CRITIC("Identifies logical flaws and hidden assumptions"),

    /**
     * Verifica afirmaciones factuales contra contexto y memoria.
     */
    VERIFIER("Verifies factual claims and ensures grounding"),

    /**
     * Evalúa el consejo y toma la decisión final basada en el scoring.
     */
    ARBITER("Final decision maker based on counsel feedback"),

    /**
     * Análisis de imágenes y comprensión visual multimodal.
     */
    VISION_ANALYZER("Multimodal image analysis and visual understanding"),

    /**
     * Meta-optimizer for Cognitive Evolution Engine (OPRO + PromptBreeder).
     * Uses the LLM to generate and evolve prompt candidates from performance history.
     */
    OPTIMIZER("Meta-optimization for cognitive prompt evolution");

    private final String description;

    LLMRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
