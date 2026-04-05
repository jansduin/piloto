package com.piloto.cdi.kernel.context;

import com.piloto.cdi.kernel.memory.MemoryEntry;
import com.piloto.cdi.kernel.memory.MemoryType;

import java.util.*;

public class RelevanceScorer {

    private static final double RECENCY_WEIGHT = 0.3;
    private static final double KEYWORD_WEIGHT = 0.5;
    private static final double TYPE_WEIGHT = 0.2;

    private static final Map<MemoryType, Double> TYPE_SCORES = Map.of(
        MemoryType.SEMANTIC, 1.0,
        MemoryType.EPISODIC, 0.8,
        MemoryType.SHORT_TERM, 0.6,
        MemoryType.TECHNICAL, 0.5
    );

    public double score(String goal, MemoryEntry entry) {
        if (goal == null || goal.isEmpty() || entry == null) {
            return 0.0;
        }

        double keywordScore = calculateKeywordScore(goal, entry.getContent());
        double recencyScore = calculateRecencyScore(entry.getVersion());
        double typeScore = calculateTypeScore(entry.getType());

        double totalScore = (keywordScore * KEYWORD_WEIGHT) +
                           (recencyScore * RECENCY_WEIGHT) +
                           (typeScore * TYPE_WEIGHT);

        return clamp(totalScore);
    }

    private double calculateKeywordScore(String goal, String content) {
        if (content == null || content.isEmpty()) {
            return 0.0;
        }

        String normalizedGoal = normalize(goal);
        String normalizedContent = normalize(content);

        Set<String> goalWords = extractWords(normalizedGoal);
        Set<String> contentWords = extractWords(normalizedContent);

        if (goalWords.isEmpty()) {
            return 0.0;
        }

        long matchCount = goalWords.stream()
            .filter(contentWords::contains)
            .count();

        return (double) matchCount / goalWords.size();
    }

    private double calculateRecencyScore(int version) {
        return Math.min(1.0, version / 10.0);
    }

    private double calculateTypeScore(MemoryType type) {
        return TYPE_SCORES.getOrDefault(type, 0.5);
    }

    private String normalize(String text) {
        return text.toLowerCase().trim();
    }

    private Set<String> extractWords(String text) {
        if (text == null || text.isEmpty()) {
            return Set.of();
        }

        String[] words = text.split("\\s+");
        Set<String> filtered = new HashSet<>();
        
        for (String word : words) {
            word = word.replaceAll("[^a-z0-9]", "");
            if (word.length() > 2) {
                filtered.add(word);
            }
        }

        return filtered;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public Map<String, Double> analyzeScoreComponents(String goal, MemoryEntry entry) {
        Map<String, Double> components = new HashMap<>();
        components.put("keyword_score", calculateKeywordScore(goal, entry.getContent()));
        components.put("recency_score", calculateRecencyScore(entry.getVersion()));
        components.put("type_score", calculateTypeScore(entry.getType()));
        components.put("total_score", score(goal, entry));
        return Map.copyOf(components);
    }
}
