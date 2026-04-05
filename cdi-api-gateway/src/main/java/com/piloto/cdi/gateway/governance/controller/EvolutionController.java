package com.piloto.cdi.gateway.governance.controller;

import com.piloto.cdi.gateway.governance.evolution.engine.CognitiveEvolutionEngine;
import com.piloto.cdi.gateway.governance.evolution.model.OutcomeReport;
import com.piloto.cdi.gateway.governance.evolution.model.VariantResponse;
import com.piloto.cdi.kernel.command.BaseCommand;
import com.piloto.cdi.kernel.command.CommandType;
import com.piloto.cdi.kernel.executive.ExecutionResult;
import com.piloto.cdi.kernel.executive.ExecutiveController;
import com.piloto.cdi.kernel.types.TenantID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API for the Cognitive Evolution Engine.
 *
 * Follows GovernanceController conventions:
 *   - Base path: /gov/evolution (consistent with /gov)
 *   - CQRS: commands under /commands/
 *   - Validation: 400 with {"error": "..."} on bad input
 *   - Audit trail: emitAuditEvent via ExecutiveController
 *
 * DOMAIN AGNOSTIC: cellDimensions values are defined entirely by the consuming app.
 * PILOTO F1 sees only Map<String,String> + SUCCESS/FAILURE.
 */
@RestController
@RequestMapping("/gov/evolution")
public class EvolutionController {

    private static final Logger logger = LoggerFactory.getLogger(EvolutionController.class);

    private final CognitiveEvolutionEngine engine;
    private final ExecutiveController executiveController;

    public EvolutionController(CognitiveEvolutionEngine engine,
                                ExecutiveController executiveController) {
        this.engine = engine;
        this.executiveController = executiveController;
    }

    // ═══════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════

    /**
     * Get the optimal variant for a behavioral cell.
     * Uses epsilon-greedy routing: champions most of the time, challengers ~15%.
     * Returns isFallback=true if no variants exist yet (use base prompt).
     */
    @GetMapping("/variant")
    public ResponseEntity<?> getVariant(
            @RequestParam String intentType,
            @RequestParam String sessionStage,
            @RequestParam(required = false, defaultValue = "default") String tenantId) {

        if (intentType == null || intentType.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "intentType is required"));
        }
        if (sessionStage == null || sessionStage.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionStage is required"));
        }

        Map<String, String> dims = Map.of(
                "intenttype", intentType.toLowerCase().trim(),
                "sessionstage", sessionStage.toLowerCase().trim());

        VariantResponse response = engine.getVariant(dims, tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the full MAP-Elites grid state for observability/dashboard.
     */
    @GetMapping("/grid")
    public ResponseEntity<Map<String, Object>> getGridState() {
        return ResponseEntity.ok(engine.getGridState());
    }

    // ═══════════════════════════════════════════════════════════
    // COMMANDS
    // ═══════════════════════════════════════════════════════════

    /**
     * Report an outcome from a consuming application session.
     * Idempotent: duplicate sessionIds are safely ignored.
     */
    @PostMapping("/commands/report-outcome")
    public ResponseEntity<?> reportOutcome(@RequestBody OutcomeReport report) {
        try {
            boolean processed = engine.processOutcome(report);

            emitAuditEvent(report.tenantId(), "EVOLUTION_OUTCOME_RECORDED",
                    Map.of("sessionId", report.sessionId(),
                            "variantId", report.variantId(),
                            "outcome", report.outcome().name(),
                            "processed", String.valueOf(processed)));

            return ResponseEntity.ok(Map.of(
                    "status", processed ? "recorded" : "duplicate_ignored",
                    "sessionId", report.sessionId()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error processing outcome: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal processing error"));
        }
    }

    /**
     * Trigger a manual OPRO + PromptBreeder optimization cycle.
     * Admin-only operation.
     */
    @PostMapping("/commands/trigger-optimization")
    public ResponseEntity<?> triggerOptimization(
            @RequestParam(defaultValue = "default") String tenantId) {

        engine.triggerOptimization(tenantId);

        emitAuditEvent(tenantId, "EVOLUTION_OPTIMIZATION_TRIGGERED",
                Map.of("tenantId", tenantId));

        return ResponseEntity.accepted().body(Map.of(
                "status", "cycle_triggered",
                "tenantId", tenantId));
    }

    // ═══════════════════════════════════════════════════════════
    // AUDIT (same pattern as GovernanceController)
    // ═══════════════════════════════════════════════════════════

    private void emitAuditEvent(String tenantId, String eventName, Map<String, Object> details) {
        try {
            String resolved = (tenantId != null && !tenantId.isBlank())
                    ? tenantId : "default_tenant";
            Map<String, Object> payload = new HashMap<>(details);
            payload.put("governanceEvent", eventName);
            payload.put("timestamp", Instant.now().toString());

            BaseCommand cmd = BaseCommand.create(
                    TenantID.of(resolved), CommandType.UPDATE_STATE, payload);
            ExecutionResult result = executiveController.execute(cmd);

            if (!result.isSuccess()) {
                logger.warn("Evolution audit event '{}' not recorded: {}",
                        eventName, result.getErrors());
            }
        } catch (Exception e) {
            // Non-critical: audit failure must not break the evolution flow
            logger.error("Non-critical: failed to record evolution audit event '{}': {}",
                    eventName, e.getMessage());
        }
    }
}
