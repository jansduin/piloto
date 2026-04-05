package com.piloto.cdi.kernel.context;

import java.util.*;
import java.util.stream.Collectors;

public class ContextBudgetManager {

    private static final int DEFAULT_CHARS_PER_TOKEN = 4;

    private static final Map<String, Integer> SECTION_PRIORITIES = Map.of(
            "system", 1,
            "goal", 2,
            "critical_memory", 3,
            "episodic", 4,
            "technical", 5,
            "trace", 6);

    private final int maxTokens;

    public ContextBudgetManager(int maxTokens) {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        this.maxTokens = maxTokens;
    }

    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / (double) DEFAULT_CHARS_PER_TOKEN);
    }

    public Map<String, String> allocate(Map<String, String> sections) {
        if (sections == null || sections.isEmpty()) {
            return Map.of();
        }

        Map<String, Integer> tokenCounts = sections.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> estimateTokens(e.getValue())));

        int totalTokens = tokenCounts.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        if (totalTokens <= maxTokens) {
            return Map.copyOf(sections);
        }

        List<Map.Entry<String, String>> prioritizedSections = sections.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> SECTION_PRIORITIES.getOrDefault(e.getKey(), Integer.MAX_VALUE)))
                .collect(Collectors.toList());

        Map<String, String> result = new LinkedHashMap<>();
        int usedTokens = 0;

        for (Map.Entry<String, String> entry : prioritizedSections) {
            String key = entry.getKey();
            String value = entry.getValue();
            int sectionTokens = tokenCounts.get(key);

            if (usedTokens + sectionTokens <= maxTokens) {
                result.put(key, value);
                usedTokens += sectionTokens;
            } else {
                int remainingTokens = maxTokens - usedTokens;
                if (remainingTokens > 0) {
                    String truncated = truncateToTokens(value, remainingTokens);
                    result.put(key, truncated);
                    usedTokens = maxTokens;
                }
                break;
            }
        }

        return Map.copyOf(result);
    }

    private String truncateToTokens(String text, int targetTokens) {
        if (text == null || text.isEmpty() || targetTokens <= 0) {
            return "";
        }

        String suffix = "... [truncated]";
        int suffixTokens = estimateTokens(suffix);

        // If target is too small for even the suffix, return strict truncation or empty
        if (targetTokens <= suffixTokens) {
            // Strictly truncate to target chars if we can't fit suffix nicely
            int maxChars = targetTokens * DEFAULT_CHARS_PER_TOKEN;
            return text.length() > maxChars ? text.substring(0, maxChars) : text;
        }

        int availableTokensForText = targetTokens - suffixTokens;
        int targetChars = availableTokensForText * DEFAULT_CHARS_PER_TOKEN;

        if (text.length() <= targetChars) {
            return text;
        }

        String truncated = text.substring(0, targetChars);
        int lastSpace = truncated.lastIndexOf(' ');

        if (lastSpace > targetChars / 2) {
            truncated = truncated.substring(0, lastSpace);
        }

        return truncated + suffix;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public Map<String, Integer> analyzeTokenUsage(Map<String, String> sections) {
        if (sections == null) {
            return Map.of();
        }

        return sections.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> estimateTokens(e.getValue())));
    }
}
