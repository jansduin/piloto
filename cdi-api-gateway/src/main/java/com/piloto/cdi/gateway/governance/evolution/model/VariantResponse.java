package com.piloto.cdi.gateway.governance.evolution.model;

import java.util.Map;
import java.util.UUID;

/**
 * Response DTO containing the optimal variant for the requested behavioral cell.
 *
 * isChallenger=true means this is an epsilon-greedy exploration pick (not the champion).
 * isFallback=true means no variants exist yet — caller should use the base prompt
 * from PromptGovernanceEngine and report outcomes to bootstrap the grid.
 */
public record VariantResponse(
    UUID variantId,
    String promptContent,
    Map<String, String> cellDimensions,
    double wilsonScore,
    int sampleSize,
    boolean isChallenger,
    boolean isFallback
) {
    public static VariantResponse fallback(Map<String, String> cellDimensions) {
        return new VariantResponse(null, null, cellDimensions, 0.0, 0, false, true);
    }
}
