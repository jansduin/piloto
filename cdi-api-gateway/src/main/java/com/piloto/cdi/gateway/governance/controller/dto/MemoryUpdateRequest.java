package com.piloto.cdi.gateway.governance.controller.dto;

/**
 * Typed request for updating an existing DomainMemoryItem's content via CQRS
 * command.
 * CDI Doctrine: No PUT endpoints — all mutations flow through command
 * endpoints.
 */
public record MemoryUpdateRequest(
        String id, // UUID as String — the memory item to update (required)
        String content // New memory content (required)
) {
}
