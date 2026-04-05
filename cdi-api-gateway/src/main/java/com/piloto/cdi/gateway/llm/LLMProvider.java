package com.piloto.cdi.gateway.llm;

import com.piloto.cdi.gateway.llm.dto.LLMRequest;
import com.piloto.cdi.gateway.llm.dto.LLMResponse;
import java.util.List;

/**
 * Interface base para proveedores de LLM (Google, OpenAI, Anthropic, etc).
 */
public interface LLMProvider {
    /**
     * Genera una respuesta del LLM basada en el prompt y parámetros.
     */
    LLMResponse generateCompletion(LLMRequest request);

    /**
     * Genera un vector de embeddings para búsqueda semántica.
     */
    List<Float> generateEmbedding(String text);

    /**
     * Verifica si el proveedor es alcanzable y sus credenciales son válidas.
     */
    boolean isHealthy();

    /**
     * Retorna el nombre del proveedor (e.g., "google", "openai", "ollama").
     */
    String getProviderName();

    /**
     * Retorna el nombre del modelo específico (e.g., "gemini-2.5-pro", "gpt-4o").
     */
    String getModelName();
}
