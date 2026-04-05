package com.piloto.cdi.gateway.governance.evolution.model;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Extensible coordinates in the MAP-Elites behavioral space.
 *
 * Unlike a fixed 2D grid, dimensions are a flexible Map<String,String>.
 * The consuming app defines its own dimensions:
 *   App A: {intentType=price_inquiry, sessionStage=first_contact}
 *   App B: {intentType=architecture_review, sessionStage=initial_brief}
 *   App C: {intentType=risk_assessment, sessionStage=position_open}
 *
 * PILOTO F1 organizes the grid — it does not interpret the semantics.
 *
 * Reference: Mouret & Clune, 2015 — "Illuminating Search Spaces by Mapping Elites"
 */
public record BehavioralCell(Map<String, String> dimensions) {

    public BehavioralCell {
        Objects.requireNonNull(dimensions, "dimensions must not be null");
        if (dimensions.isEmpty()) {
            throw new IllegalArgumentException("dimensions must not be empty");
        }
        // Defensive copy + normalize to lowercase, sorted for deterministic key generation
        dimensions = dimensions.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toLowerCase().trim(),
                        e -> e.getValue().toLowerCase().trim(),
                        (a, b) -> a,
                        TreeMap::new));
    }

    /**
     * Convenience factory for the common 2-dimension case.
     */
    public static BehavioralCell of(String intentType, String sessionStage) {
        if (intentType == null || intentType.isBlank())
            throw new IllegalArgumentException("intentType must not be blank");
        if (sessionStage == null || sessionStage.isBlank())
            throw new IllegalArgumentException("sessionStage must not be blank");
        return new BehavioralCell(Map.of("intenttype", intentType, "sessionstage", sessionStage));
    }

    /**
     * Deterministic key for HashMap lookup. Sorted dimensions ensure consistency.
     */
    public String key() {
        return dimensions.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("::"));
    }
}
