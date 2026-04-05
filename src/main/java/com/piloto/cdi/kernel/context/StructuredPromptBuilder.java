package com.piloto.cdi.kernel.context;

import java.util.HashMap;
import java.util.Map;

public class StructuredPromptBuilder {

    private static final String SYSTEM_TEMPLATE = 
        "# SYSTEM RULES\n\n" +
        "%s\n\n" +
        "# RESTRICTIONS\n" +
        "- Stay grounded in provided context\n" +
        "- Do not invent facts\n" +
        "- Cite sources when possible\n" +
        "- If uncertain, explicitly state it\n\n" +
        "# OBJECTIVE\n" +
        "%s";

    private static final String USER_TEMPLATE =
        "# GOAL\n" +
        "%s\n\n" +
        "# RELEVANT CONTEXT\n" +
        "%s\n\n" +
        "# EXECUTION TRACE\n" +
        "%s\n\n" +
        "# EXPECTED OUTPUT\n" +
        "Provide a structured response addressing the goal.\n" +
        "Format: JSON with keys 'reasoning', 'decision', 'next_actions'";

    public Map<String, Object> build(
        String system,
        String goal,
        String memoryContext,
        String traceSummary
    ) {
        if (system == null) system = "You are a precise reasoning assistant.";
        if (goal == null) goal = "No specific goal provided.";
        if (memoryContext == null) memoryContext = "No relevant memory available.";
        if (traceSummary == null) traceSummary = "No execution trace available.";

        String systemPrompt = String.format(SYSTEM_TEMPLATE, system, goal);
        String userPrompt = String.format(USER_TEMPLATE, goal, memoryContext, traceSummary);

        Map<String, Object> result = new HashMap<>();
        result.put("system_prompt", systemPrompt);
        result.put("user_prompt", userPrompt);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("system_length", systemPrompt.length());
        metadata.put("user_length", userPrompt.length());
        metadata.put("total_length", systemPrompt.length() + userPrompt.length());
        metadata.put("estimated_tokens", estimateTokens(systemPrompt + userPrompt));
        metadata.put("has_memory_context", !memoryContext.equals("No relevant memory available."));
        metadata.put("has_trace", !traceSummary.equals("No execution trace available."));
        
        result.put("metadata", Map.copyOf(metadata));

        return Map.copyOf(result);
    }

    public Map<String, Object> buildWithCustomFormat(
        String system,
        String goal,
        String memoryContext,
        String traceSummary,
        String outputFormat
    ) {
        if (outputFormat == null || outputFormat.isEmpty()) {
            return build(system, goal, memoryContext, traceSummary);
        }

        String userTemplate =
            "# GOAL\n" +
            "%s\n\n" +
            "# RELEVANT CONTEXT\n" +
            "%s\n\n" +
            "# EXECUTION TRACE\n" +
            "%s\n\n" +
            "# REQUIRED OUTPUT FORMAT\n" +
            "%s";

        if (system == null) system = "You are a precise reasoning assistant.";
        if (goal == null) goal = "No specific goal provided.";
        if (memoryContext == null) memoryContext = "No relevant memory available.";
        if (traceSummary == null) traceSummary = "No execution trace available.";

        String systemPrompt = String.format(SYSTEM_TEMPLATE, system, goal);
        String userPrompt = String.format(userTemplate, goal, memoryContext, traceSummary, outputFormat);

        Map<String, Object> result = new HashMap<>();
        result.put("system_prompt", systemPrompt);
        result.put("user_prompt", userPrompt);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("system_length", systemPrompt.length());
        metadata.put("user_length", userPrompt.length());
        metadata.put("total_length", systemPrompt.length() + userPrompt.length());
        metadata.put("estimated_tokens", estimateTokens(systemPrompt + userPrompt));
        metadata.put("has_memory_context", !memoryContext.equals("No relevant memory available."));
        metadata.put("has_trace", !traceSummary.equals("No execution trace available."));
        metadata.put("custom_format", true);
        
        result.put("metadata", Map.copyOf(metadata));

        return Map.copyOf(result);
    }

    private int estimateTokens(String text) {
        return (int) Math.ceil(text.length() / 4.0);
    }

    public Map<String, Integer> analyzePromptStructure(Map<String, Object> prompt) {
        Map<String, Integer> structure = new HashMap<>();
        
        String systemPrompt = (String) prompt.get("system_prompt");
        String userPrompt = (String) prompt.get("user_prompt");
        
        structure.put("system_tokens", estimateTokens(systemPrompt));
        structure.put("user_tokens", estimateTokens(userPrompt));
        structure.put("total_tokens", 
            estimateTokens(systemPrompt) + estimateTokens(userPrompt));
        
        return Map.copyOf(structure);
    }
}
