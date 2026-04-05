package com.piloto.cdi.gateway.governance.evolution.engine;

import com.piloto.cdi.gateway.governance.evolution.model.BehavioralCell;
import com.piloto.cdi.gateway.governance.evolution.model.EvolutionVariant;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Epsilon-greedy traffic router for the MAP-Elites grid.
 *
 * Solves the critical flaw identified in the v4.2 audit: challengers MUST
 * receive traffic to accumulate samples and have a chance at promotion.
 *
 * Policy:
 *   - With probability (1-ε): return the champion (exploit)
 *   - With probability ε: return a random challenger (explore)
 *   - If no champion exists: return any challenger (bootstrap mode)
 *   - If no variants exist: return empty (fallback to base prompt)
 *
 * ε defaults to 0.15 (15% exploration). Configurable via application.yml.
 */
@Component
public class TrafficRouter {

    private final MapElitesGrid grid;
    private final double epsilon;

    public TrafficRouter(MapElitesGrid grid, double epsilon) {
        this.grid = grid;
        this.epsilon = epsilon;
    }

    /**
     * Select the variant to serve for a given behavioral cell.
     *
     * @return route decision with variant, challenger flag, or fallback
     */
    public RouteDecision route(BehavioralCell cell) {
        Optional<EvolutionVariant> champion = grid.getChampion(cell);
        List<EvolutionVariant> challengers = grid.getChallengers(cell);

        // No variants at all → fallback to base prompt
        if (champion.isEmpty() && challengers.isEmpty()) {
            return RouteDecision.fallback();
        }

        // No champion yet → pick any challenger (bootstrap phase)
        if (champion.isEmpty()) {
            EvolutionVariant pick = challengers.get(
                    ThreadLocalRandom.current().nextInt(challengers.size()));
            return RouteDecision.of(pick, true);
        }

        // No challengers → always serve champion
        if (challengers.isEmpty()) {
            return RouteDecision.of(champion.get(), false);
        }

        // Epsilon-greedy: explore vs exploit
        if (ThreadLocalRandom.current().nextDouble() < epsilon) {
            EvolutionVariant pick = challengers.get(
                    ThreadLocalRandom.current().nextInt(challengers.size()));
            return RouteDecision.of(pick, true);
        }

        return RouteDecision.of(champion.get(), false);
    }

    /**
     * Immutable route decision.
     */
    public record RouteDecision(
            EvolutionVariant variant,
            boolean isChallenger,
            boolean isFallback
    ) {
        public static RouteDecision of(EvolutionVariant variant, boolean isChallenger) {
            return new RouteDecision(variant, isChallenger, false);
        }

        public static RouteDecision fallback() {
            return new RouteDecision(null, false, true);
        }
    }
}
