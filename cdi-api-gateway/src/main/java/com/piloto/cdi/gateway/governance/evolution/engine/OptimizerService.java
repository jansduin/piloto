package com.piloto.cdi.gateway.governance.evolution.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piloto.cdi.gateway.governance.evolution.model.BehavioralCell;
import com.piloto.cdi.gateway.governance.evolution.model.EvolutionSnapshot;
import com.piloto.cdi.gateway.governance.evolution.model.EvolutionVariant;
import com.piloto.cdi.gateway.governance.evolution.store.EvolutionStore;
import com.piloto.cdi.gateway.llm.LLMRole;
import com.piloto.cdi.gateway.llm.LLMRoleManager;
import com.piloto.cdi.gateway.llm.dto.LLMRequest;
import com.piloto.cdi.gateway.llm.dto.LLMResponse;
import com.piloto.cdi.gateway.llm.util.LLMJsonUtils;
import com.piloto.cdi.kernel.governance.type.PromptLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * OPRO + PromptBreeder meta-optimizer.
 *
 * Two-level evolution:
 *   Level 1 (OPRO): LLM generates new prompt candidates from (prompt, fitness) history
 *   Level 2 (PromptBreeder): LLM evolves the META-PROMPT that Level 1 uses
 *
 * Uses PILOTO F1's existing LLMRoleManager for all LLM calls via
 * {@code executeForRole(LLMRole.OPTIMIZER, new LLMRequest(prompt, systemPrompt))}.
 *
 * Runs every 48h via @Scheduled. Also triggerable via REST.
 * Failure is NON-CRITICAL — system continues with existing variants.
 *
 * References:
 *   OPRO:          Yang et al., 2024 (ICLR) — "Large Language Models as Optimizers"
 *   PromptBreeder: Fernando et al., 2024 (ICML) — "Self-Referential Self-Improvement"
 */
@Service
public class OptimizerService {

    private static final Logger logger = LoggerFactory.getLogger(OptimizerService.class);

    private final MapElitesGrid grid;
    private final EvolutionStore store;
    private final LLMRoleManager roleManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final int minVariantsToOptimize;
    private final int minSamplesPerVariant;

    // Default meta-prompt (Level 2: PromptBreeder evolves this over time)
    private static final String DEFAULT_META_PROMPT = """
            You are a cognitive prompt optimizer. Analyze the history of prompt variants
            and their Wilson Score fitness (confidence-adjusted conversion rate).
            Generate a new variant expected to outperform the current best.

            You are domain-agnostic. Do not assume any business context.
            Focus on: linguistic clarity, specificity, structure, and patterns that
            correlate with higher fitness scores in the provided history.
            """;

    public OptimizerService(MapElitesGrid grid, EvolutionStore store,
                            LLMRoleManager roleManager,
                            int minVariantsToOptimize, int minSamplesPerVariant) {
        this.grid = grid;
        this.store = store;
        this.roleManager = roleManager;
        this.minVariantsToOptimize = minVariantsToOptimize;
        this.minSamplesPerVariant = minSamplesPerVariant;
    }

    /**
     * Scheduled OPRO + PromptBreeder cycle.
     * Runs for "default" tenant. Additional tenants triggered via REST.
     */
    @Scheduled(cron = "${piloto.evolution.optimizer.cron:0 0 3 */2 * *}")
    public void runScheduledCycle() {
        logger.info("[OPRO] Starting scheduled optimization cycle");
        optimize("default");
    }

    /**
     * Full optimization cycle for a tenant.
     * Level 1: Generate new prompt candidate (OPRO)
     * Level 2: Evolve the meta-prompt itself (PromptBreeder)
     */
    public void optimize(String tenantId) {
        List<EvolutionVariant> history = grid.getVariantsSortedByFitness(tenantId).stream()
                .filter(v -> v.hasMinSamples(minSamplesPerVariant))
                .toList();

        if (history.size() < minVariantsToOptimize) {
            logger.info("[OPRO] Skipping tenant {}: insufficient history ({}/{})",
                    tenantId, history.size(), minVariantsToOptimize);
            return;
        }

        // --- LEVEL 1: OPRO — Generate new prompt candidate ---
        try {
            EvolutionSnapshot snapshot = store.getOrCreate(tenantId);
            String metaPrompt = snapshot.metaPrompt() != null
                    ? snapshot.metaPrompt()
                    : DEFAULT_META_PROMPT;

            String userPrompt = buildOproPrompt(history);
            LLMRequest request = new LLMRequest(userPrompt, metaPrompt);
            LLMResponse response = roleManager.executeForRole(LLMRole.OPTIMIZER, request);

            if (!response.success()) {
                logger.error("[OPRO] LLM call failed for tenant {}: {}",
                        tenantId,
                        response.error() != null ? response.error().message() : "unknown error");
                return;
            }

            EvolutionVariant candidate = parseCandidate(response.content(), tenantId);
            if (candidate != null) {
                grid.addVariant(candidate);
                logger.info("[OPRO] New candidate {} generated for tenant {} in cell {}",
                        candidate.getId(), tenantId, candidate.getCell().key());

                // Persist after adding candidate
                persistGridState(tenantId, snapshot);
            }

            // --- LEVEL 2: PromptBreeder — Evolve the meta-prompt ---
            evolveMetaPrompt(tenantId, metaPrompt, history, snapshot);

        } catch (Exception e) {
            logger.error("[OPRO] Cycle failed for tenant {}: {}", tenantId, e.getMessage(), e);
            // NON-CRITICAL: system continues with existing variants
        }
    }

    /**
     * PromptBreeder's self-referential twist: evolve the meta-prompt
     * that generates candidates. This makes the optimization ITSELF
     * get better over time.
     *
     * Reference: Fernando et al., 2024 (ICML)
     */
    private void evolveMetaPrompt(String tenantId, String currentMetaPrompt,
                                   List<EvolutionVariant> history, EvolutionSnapshot snapshot) {
        try {
            String mutationSystemPrompt = """
                    You are a meta-optimizer. Your job is to IMPROVE the optimizer prompt below.
                    The optimizer prompt is used to generate new prompt candidates from performance history.
                    Analyze what the current optimizer prompt does well and what it misses.
                    Generate an improved version that should produce better candidates.

                    You are domain-agnostic. The optimizer should work for ANY task.
                    Respond ONLY with valid JSON: {"improvedMetaPrompt": "...", "justification": "..."}
                    """;

            String mutationUserPrompt = String.format(
                    "Current optimizer prompt (generation %d):\n\"\"\"\n%s\n\"\"\"\n\n" +
                    "Recent candidate quality (last %d variants, sorted by fitness):\n%s\n\n" +
                    "Improve the optimizer prompt to generate better candidates.",
                    snapshot.metaPromptGeneration(),
                    currentMetaPrompt,
                    history.size(),
                    buildFitnessSummary(history));

            LLMRequest request = new LLMRequest(mutationUserPrompt, mutationSystemPrompt);
            LLMResponse response = roleManager.executeForRole(LLMRole.OPTIMIZER, request);

            if (response.success() && response.content() != null) {
                String cleaned = LLMJsonUtils.cleanJson(response.content());
                JsonNode node = objectMapper.readTree(cleaned);
                if (node.has("improvedMetaPrompt")) {
                    String improved = node.get("improvedMetaPrompt").asText();

                    // Persist the evolved meta-prompt
                    EvolutionSnapshot updated = new EvolutionSnapshot(
                            snapshot.tenantId(),
                            snapshot.variants(),
                            snapshot.championKeys(),
                            snapshot.processedSessionIds(),
                            improved,
                            snapshot.metaPromptGeneration() + 1);
                    store.save(tenantId, updated);

                    String justification = node.has("justification")
                            ? node.get("justification").asText() : "none";
                    logger.info("[PromptBreeder] Meta-prompt evolved to gen {} for tenant {}: {}",
                            updated.metaPromptGeneration(), tenantId, justification);
                }
            }
        } catch (Exception e) {
            logger.warn("[PromptBreeder] Meta-prompt evolution failed (non-critical): {}",
                    e.getMessage());
        }
    }

    /**
     * Build the OPRO user prompt containing (prompt, fitness) history
     * sorted worst-to-best so the LLM can identify improvement patterns.
     */
    private String buildOproPrompt(List<EvolutionVariant> history) {
        var sb = new StringBuilder();
        sb.append("Prompt variants ordered LOWEST to HIGHEST Wilson Score fitness:\n\n");

        for (int i = 0; i < history.size(); i++) {
            var v = history.get(i);
            double wilson = FitnessCalculator.wilsonLowerBound(
                    v.getTotalConverted(), v.getTotalSeen());
            sb.append(String.format(
                    "=== Variant %d (wilson=%.4f, raw=%.1f%%, n=%d, cell=%s) ===\n%s\n\n",
                    i + 1, wilson, v.getRawConversionRate() * 100,
                    v.getTotalSeen(), v.getCell().key(),
                    v.getPromptContentSnapshot()));
        }

        sb.append("Generate a NEW variant that improves upon patterns of the top performers.\n");
        sb.append("Respond ONLY with valid JSON:\n");
        sb.append("""
                {
                    "proposedContent": "the new prompt text here",
                    "targetCell": {"intentType": "...", "sessionStage": "..."},
                    "justification": "brief explanation of why this should perform better"
                }
                """);
        return sb.toString();
    }

    /**
     * Build a concise fitness summary for the PromptBreeder meta-prompt evolution.
     */
    private String buildFitnessSummary(List<EvolutionVariant> history) {
        var sb = new StringBuilder();
        for (var v : history) {
            double wilson = FitnessCalculator.wilsonLowerBound(
                    v.getTotalConverted(), v.getTotalSeen());
            sb.append(String.format("  - cell=%s, wilson=%.4f, n=%d, raw=%.1f%%\n",
                    v.getCell().key(), wilson, v.getTotalSeen(),
                    v.getRawConversionRate() * 100));
        }
        return sb.toString();
    }

    /**
     * Parse an OPRO candidate from LLM JSON response.
     * Uses LLMJsonUtils.cleanJson() for robust parsing of LLM output.
     */
    private EvolutionVariant parseCandidate(String llmContent, String tenantId) {
        try {
            String cleaned = LLMJsonUtils.cleanJson(llmContent);
            JsonNode node = objectMapper.readTree(cleaned);

            if (!node.has("proposedContent") || !node.has("targetCell")) {
                logger.warn("[OPRO] LLM response missing required fields: proposedContent, targetCell");
                return null;
            }

            String proposedContent = node.get("proposedContent").asText();
            if (proposedContent == null || proposedContent.isBlank()) {
                logger.warn("[OPRO] Empty proposedContent in LLM response");
                return null;
            }

            JsonNode cellNode = node.get("targetCell");
            Map<String, String> dims = new HashMap<>();
            cellNode.fields().forEachRemaining(entry ->
                    dims.put(entry.getKey(), entry.getValue().asText()));

            if (dims.isEmpty()) {
                logger.warn("[OPRO] Empty targetCell dimensions in LLM response");
                return null;
            }

            return EvolutionVariant.builder()
                    .cell(new BehavioralCell(dims))
                    .layer(PromptLayer.RUNTIME)
                    .tenantId(tenantId)
                    .promptContentSnapshot(proposedContent)
                    .build();

        } catch (Exception e) {
            logger.error("[OPRO] Failed to parse candidate from LLM response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Persist the current grid state to EvolutionStore.
     */
    private void persistGridState(String tenantId, EvolutionSnapshot currentSnapshot) {
        try {
            EvolutionSnapshot updated = store.buildSnapshot(
                    tenantId,
                    new ArrayList<>(grid.getAllVariants()),
                    grid.getChampionMap(),
                    grid.getProcessedSessions(tenantId),
                    currentSnapshot.metaPrompt(),
                    currentSnapshot.metaPromptGeneration());
            store.save(tenantId, updated);
        } catch (Exception e) {
            logger.error("[OPRO] Failed to persist grid state for tenant {}: {}",
                    tenantId, e.getMessage());
        }
    }
}
