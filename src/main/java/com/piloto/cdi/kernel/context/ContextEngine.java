package com.piloto.cdi.kernel.context;

import com.piloto.cdi.kernel.memory.MemoryEntry;

import java.util.*;

public class ContextEngine {

    private final ContextBudgetManager budgetManager;
    private final RelevanceScorer relevanceScorer;
    private final ContextRanker contextRanker;
    private final ContextCompressor compressor;
    private final StructuredPromptBuilder promptBuilder;
    private final GroundingValidator groundingValidator;
    private final HallucinationGuard hallucinationGuard;
    private final ContextAuditLogger auditLogger;

    public ContextEngine(
        ContextBudgetManager budgetManager,
        RelevanceScorer relevanceScorer,
        ContextRanker contextRanker,
        ContextCompressor compressor,
        StructuredPromptBuilder promptBuilder,
        GroundingValidator groundingValidator,
        HallucinationGuard hallucinationGuard,
        ContextAuditLogger auditLogger
    ) {
        if (budgetManager == null || relevanceScorer == null || contextRanker == null ||
            compressor == null || promptBuilder == null || groundingValidator == null ||
            hallucinationGuard == null || auditLogger == null) {
            throw new IllegalArgumentException("All context engine components must be non-null");
        }

        this.budgetManager = budgetManager;
        this.relevanceScorer = relevanceScorer;
        this.contextRanker = contextRanker;
        this.compressor = compressor;
        this.promptBuilder = promptBuilder;
        this.groundingValidator = groundingValidator;
        this.hallucinationGuard = hallucinationGuard;
        this.auditLogger = auditLogger;
    }

    public Map<String, Object> prepareContext(
        String goal,
        List<MemoryEntry> availableMemories,
        String traceSummary,
        int maxTokens
    ) {
        if (goal == null || goal.isEmpty()) {
            throw new IllegalArgumentException("Goal cannot be null or empty");
        }

        List<MemoryEntry> selectedMemories = contextRanker.rank(
            goal,
            availableMemories != null ? availableMemories : List.of(),
            Math.min(10, availableMemories != null ? availableMemories.size() : 0)
        );

        List<MemoryEntry> discardedMemories = new ArrayList<>(
            availableMemories != null ? availableMemories : List.of()
        );
        discardedMemories.removeAll(selectedMemories);

        String memoryContext = buildMemoryContext(selectedMemories);
        String compressedTrace = traceSummary;

        int estimatedTokens = budgetManager.estimateTokens(memoryContext + compressedTrace);
        double compressionRatio = 1.0;

        if (estimatedTokens > maxTokens) {
            int targetTokens = (int) (maxTokens * 0.8);
            compressedTrace = compressor.compress(traceSummary, targetTokens / 2);
            memoryContext = compressor.compress(memoryContext, targetTokens / 2);
            
            int newTokens = budgetManager.estimateTokens(memoryContext + compressedTrace);
            compressionRatio = (double) newTokens / estimatedTokens;
        }

        Map<String, Object> prompt = promptBuilder.build(
            "You are a precise reasoning assistant.",
            goal,
            memoryContext,
            compressedTrace
        );

        auditLogger.record(
            budgetManager.estimateTokens(
                prompt.get("system_prompt").toString() +
                prompt.get("user_prompt").toString()
            ),
            selectedMemories,
            discardedMemories,
            compressionRatio,
            "UNKNOWN"
        );

        Map<String, Object> result = new HashMap<>(prompt);
        result.put("selected_memories_count", selectedMemories.size());
        result.put("discarded_memories_count", discardedMemories.size());
        result.put("compression_ratio", compressionRatio);

        return Map.copyOf(result);
    }

    public Map<String, Object> validateResponse(
        String response,
        String context,
        double evaluationScore
    ) {
        if (response == null) {
            response = "";
        }
        if (context == null) {
            context = "";
        }

        Map<String, Object> groundingResult = groundingValidator.validate(response, context);

        Map<String, Object> guardResult = hallucinationGuard.check(
            groundingResult,
            evaluationScore
        );

        boolean shouldBlock = hallucinationGuard.shouldBlock(guardResult);

        Map<String, Object> result = new HashMap<>();
        result.put("grounding", groundingResult);
        result.put("hallucination_check", guardResult);
        result.put("should_block", shouldBlock);
        result.put("safe_to_proceed", !shouldBlock);

        return Map.copyOf(result);
    }

    public Map<String, Object> processWithValidation(
        String goal,
        List<MemoryEntry> availableMemories,
        String traceSummary,
        int maxTokens,
        String modelResponse,
        double evaluationScore
    ) {
        Map<String, Object> preparedContext = prepareContext(
            goal,
            availableMemories,
            traceSummary,
            maxTokens
        );

        String context = preparedContext.get("user_prompt").toString();

        Map<String, Object> validationResult = validateResponse(
            modelResponse,
            context,
            evaluationScore
        );

        String riskLevel = (String) ((Map<?, ?>) validationResult.get("hallucination_check"))
            .get("risk_level");

        auditLogger.record(
            budgetManager.estimateTokens(preparedContext.get("system_prompt").toString() +
                                        preparedContext.get("user_prompt").toString()),
            List.of(),
            List.of(),
            (Double) preparedContext.get("compression_ratio"),
            riskLevel
        );

        Map<String, Object> result = new HashMap<>();
        result.put("prepared_context", preparedContext);
        result.put("validation", validationResult);
        result.put("final_decision", validationResult.get("safe_to_proceed"));

        return Map.copyOf(result);
    }

    private String buildMemoryContext(List<MemoryEntry> memories) {
        if (memories == null || memories.isEmpty()) {
            return "No relevant memory available.";
        }

        StringBuilder context = new StringBuilder();
        int index = 1;

        for (MemoryEntry entry : memories) {
            context.append(String.format("[Memory %d - %s v%d]\n%s\n\n",
                index++,
                entry.getType(),
                entry.getVersion(),
                entry.getContent()
            ));
        }

        return context.toString();
    }

    public ContextAuditLogger getAuditLogger() {
        return auditLogger;
    }

    public Map<String, Object> getEngineStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("audit", auditLogger.getStatistics());
        return Map.copyOf(stats);
    }
}
