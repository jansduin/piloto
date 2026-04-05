package com.piloto.cdi.kernel.context;

import com.piloto.cdi.kernel.memory.MemoryEntry;

import java.util.*;
import java.util.stream.Collectors;

public class ContextRanker {

    private final RelevanceScorer scorer;

    public ContextRanker(RelevanceScorer scorer) {
        if (scorer == null) {
            throw new IllegalArgumentException("RelevanceScorer cannot be null");
        }
        this.scorer = scorer;
    }

    public List<MemoryEntry> rank(
        String goal,
        List<MemoryEntry> entries,
        int topK
    ) {
        if (goal == null || goal.isEmpty()) {
            throw new IllegalArgumentException("Goal cannot be null or empty");
        }

        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be positive");
        }

        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        Map<MemoryEntry, Double> scores = entries.stream()
            .collect(Collectors.toMap(
                entry -> entry,
                entry -> scorer.score(goal, entry)
            ));

        return entries.stream()
            .sorted(Comparator.comparingDouble(scores::get).reversed())
            .limit(topK)
            .collect(Collectors.toUnmodifiableList());
    }

    public Map<MemoryEntry, Double> rankWithScores(
        String goal,
        List<MemoryEntry> entries,
        int topK
    ) {
        if (goal == null || goal.isEmpty()) {
            throw new IllegalArgumentException("Goal cannot be null or empty");
        }

        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be positive");
        }

        if (entries == null || entries.isEmpty()) {
            return Map.of();
        }

        Map<MemoryEntry, Double> scores = entries.stream()
            .collect(Collectors.toMap(
                entry -> entry,
                entry -> scorer.score(goal, entry)
            ));

        return entries.stream()
            .sorted(Comparator.comparingDouble(scores::get).reversed())
            .limit(topK)
            .collect(Collectors.toMap(
                entry -> entry,
                scores::get,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    public RelevanceScorer getScorer() {
        return scorer;
    }
}
