package com.piloto.cdi.gateway.governance.evolution.engine;

import com.piloto.cdi.gateway.governance.evolution.model.BehavioralCell;
import com.piloto.cdi.gateway.governance.evolution.model.EvolutionSnapshot;
import com.piloto.cdi.gateway.governance.evolution.model.EvolutionVariant;
import com.piloto.cdi.gateway.governance.evolution.model.OutcomeReport;
import com.piloto.cdi.gateway.governance.evolution.model.VariantResponse;
import com.piloto.cdi.gateway.governance.evolution.store.EvolutionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

/**
 * Facade for the Cognitive Evolution Engine (CEE).
 * Single entry point for all evolution operations.
 *
 * Coordinates:
 *   TrafficRouter   → serve variant (with epsilon-greedy exploration)
 *   MapElitesGrid   → store outcomes, promote challengers
 *   OptimizerService→ generate new candidates (OPRO + PromptBreeder)
 *   EvolutionStore  → persist state to JSON files
 *
 * This is the class that EvolutionController injects.
 */
@Service
public class CognitiveEvolutionEngine {

    private static final Logger logger = LoggerFactory.getLogger(CognitiveEvolutionEngine.class);

    private final MapElitesGrid grid;
    private final TrafficRouter router;
    private final OptimizerService optimizer;
    private final EvolutionStore store;

    public CognitiveEvolutionEngine(MapElitesGrid grid, TrafficRouter router,
                                     OptimizerService optimizer, EvolutionStore store) {
        this.grid = grid;
        this.router = router;
        this.optimizer = optimizer;
        this.store = store;
    }

    /**
     * Get the best variant for a behavioral cell (with epsilon-greedy exploration).
     *
     * If no variants exist yet (isFallback=true), the consumer should use the
     * base prompt from PromptGovernanceEngine and report outcomes to bootstrap.
     */
    public VariantResponse getVariant(Map<String, String> cellDimensions, String tenantId) {
        BehavioralCell cell = new BehavioralCell(cellDimensions);

        // Ensure tenant data is loaded
        ensureTenantLoaded(tenantId);

        TrafficRouter.RouteDecision decision = router.route(cell);

        if (decision.isFallback()) {
            return VariantResponse.fallback(cellDimensions);
        }

        EvolutionVariant v = decision.variant();
        double wilson = FitnessCalculator.wilsonLowerBound(
                v.getTotalConverted(), v.getTotalSeen());

        return new VariantResponse(
                v.getId(),
                v.getPromptContentSnapshot(),
                cellDimensions,
                wilson,
                v.getTotalSeen(),
                decision.isChallenger(),
                false);
    }

    /**
     * Process an outcome report from a consumer.
     * Idempotent via sessionId — duplicate reports are safely ignored.
     *
     * @return true if processed, false if duplicate
     */
    public boolean processOutcome(OutcomeReport report) {
        ensureTenantLoaded(report.tenantId());

        UUID variantId;
        try {
            variantId = UUID.fromString(report.variantId());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid variantId format: {}", report.variantId());
            return false;
        }

        // recordOutcome handles idempotency internally via sessionId
        boolean processed = grid.recordOutcome(
                variantId, report.isSuccess(), report.tenantId(), report.sessionId());

        if (processed) {
            // Persist updated state
            persistState(report.tenantId());

            logger.debug("Outcome recorded: tenant={}, session={}, variant={}, success={}",
                    report.tenantId(), report.sessionId(),
                    report.variantId(), report.isSuccess());
        }

        return processed;
    }

    /**
     * Register a new variant manually (e.g., initial seed prompts).
     */
    public void registerVariant(EvolutionVariant variant) {
        grid.addVariant(variant);
        if (variant.getTenantId() != null) {
            persistState(variant.getTenantId());
        }
    }

    /**
     * Trigger a manual OPRO + PromptBreeder optimization cycle.
     */
    public void triggerOptimization(String tenantId) {
        ensureTenantLoaded(tenantId);
        optimizer.optimize(tenantId);
    }

    /**
     * Get the full grid state for observability/dashboard.
     */
    public Map<String, Object> getGridState() {
        return grid.getGridState();
    }

    /**
     * Ensure a tenant's persisted data is loaded into memory.
     */
    private void ensureTenantLoaded(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) return;
        // loadTenant is safe to call multiple times — it only loads if not already present
        grid.loadTenant(tenantId);
    }

    /**
     * Persist the current grid state for a tenant.
     */
    private void persistState(String tenantId) {
        try {
            EvolutionSnapshot current = store.getOrCreate(tenantId);
            EvolutionSnapshot snapshot = store.buildSnapshot(
                    tenantId,
                    new ArrayList<>(grid.getAllVariants()),
                    grid.getChampionMap(),
                    grid.getProcessedSessions(tenantId),
                    current.metaPrompt(),
                    current.metaPromptGeneration());
            store.save(tenantId, snapshot);
        } catch (Exception e) {
            logger.error("Failed to persist evolution state for tenant {}: {}",
                    tenantId, e.getMessage());
        }
    }
}
