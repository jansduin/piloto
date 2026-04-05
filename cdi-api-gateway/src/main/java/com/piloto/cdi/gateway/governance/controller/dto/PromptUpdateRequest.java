package com.piloto.cdi.gateway.governance.controller.dto;

/**
 * Typed request for updating an existing Prompt's content via CQRS command.
 * CDI Doctrine: No PUT endpoints — all mutations flow through command
 * endpoints.
 */
public record PromptUpdateRequest(
        String id, // UUID as String — the prompt to update (required)
        String content // New prompt content (required)
) {
}
