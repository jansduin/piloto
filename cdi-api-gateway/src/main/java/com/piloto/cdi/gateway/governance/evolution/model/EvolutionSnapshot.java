package com.piloto.cdi.gateway.governance.evolution.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

/**
 * Serializable evolution state per tenant. Persisted to JSON.
 * Contains all variants, champions, processed sessions, and meta-prompt state.
 *
 * Follows the same serialization patterns as the kernel event/snapshot stores.
 */
public record EvolutionSnapshot(
    @JsonProperty("tenantId") String tenantId,
    @JsonProperty("variants") List<EvolutionVariantDTO> variants,
    @JsonProperty("championKeys") Map<String, String> championKeys,
    @JsonProperty("processedSessionIds") Set<String> processedSessionIds,
    @JsonProperty("metaPrompt") String metaPrompt,
    @JsonProperty("metaPromptGeneration") int metaPromptGeneration
) {
    @JsonCreator
    public EvolutionSnapshot {}

    public static EvolutionSnapshot empty(String tenantId) {
        return new EvolutionSnapshot(
                tenantId, new ArrayList<>(), new HashMap<>(),
                new HashSet<>(), null, 0);
    }

    /**
     * Serializable DTO for EvolutionVariant (since EvolutionVariant uses AtomicInteger
     * which is not directly JSON-serializable).
     */
    public record EvolutionVariantDTO(
        @JsonProperty("id") String id,
        @JsonProperty("promptEntityId") String promptEntityId,
        @JsonProperty("cellDimensions") Map<String, String> cellDimensions,
        @JsonProperty("cellKey") String cellKey,
        @JsonProperty("layer") String layer,
        @JsonProperty("tenantId") String tenantId,
        @JsonProperty("promptContentSnapshot") String promptContentSnapshot,
        @JsonProperty("totalSeen") int totalSeen,
        @JsonProperty("totalConverted") int totalConverted,
        @JsonProperty("createdAt") String createdAt,
        @JsonProperty("lastOutcomeAt") String lastOutcomeAt
    ) {
        @JsonCreator
        public EvolutionVariantDTO {}
    }
}
