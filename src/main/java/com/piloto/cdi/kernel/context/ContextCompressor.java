package com.piloto.cdi.kernel.context;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ContextCompressor {

    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[.!?;]\\s+");
    private static final Set<String> CRITICAL_KEYWORDS = Set.of(
        "decision", "error", "failed", "success", "completed", "result",
        "critical", "important", "key", "must", "required", "goal"
    );

    public String compress(String text, int targetTokens) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        if (targetTokens <= 0) {
            return "";
        }

        int estimatedTokens = estimateTokens(text);
        
        if (estimatedTokens <= targetTokens) {
            return text;
        }

        String[] sentences = SENTENCE_PATTERN.split(text);
        
        List<ScoredSentence> scored = new ArrayList<>();
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            if (!sentence.isEmpty()) {
                scored.add(new ScoredSentence(sentence, scoreSentence(sentence), i));
            }
        }

        scored.sort(Comparator.comparingDouble(ScoredSentence::getScore).reversed());

        StringBuilder result = new StringBuilder();
        int usedTokens = 0;

        List<ScoredSentence> selected = new ArrayList<>();
        for (ScoredSentence ss : scored) {
            int sentenceTokens = estimateTokens(ss.getText());
            if (usedTokens + sentenceTokens <= targetTokens) {
                selected.add(ss);
                usedTokens += sentenceTokens;
            }
        }

        selected.sort(Comparator.comparingInt(ScoredSentence::getOriginalIndex));

        for (ScoredSentence ss : selected) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(ss.getText());
        }

        if (selected.size() < scored.size()) {
            result.append(" [...compressed]");
        }

        return result.toString();
    }

    private double scoreSentence(String sentence) {
        double score = 0.0;
        String lower = sentence.toLowerCase();

        for (String keyword : CRITICAL_KEYWORDS) {
            if (lower.contains(keyword)) {
                score += 1.0;
            }
        }

        if (sentence.matches(".*\\d+.*")) {
            score += 0.5;
        }

        if (sentence.contains("\"")) {
            score += 0.3;
        }

        if (sentence.length() < 50) {
            score += 0.2;
        }

        return score;
    }

    private int estimateTokens(String text) {
        return (int) Math.ceil(text.length() / 4.0);
    }

    public Map<String, Object> analyzeCompression(String original, String compressed) {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("original_tokens", estimateTokens(original));
        analysis.put("compressed_tokens", estimateTokens(compressed));
        analysis.put("compression_ratio", 
            (double) estimateTokens(compressed) / estimateTokens(original));
        analysis.put("sentences_original", 
            SENTENCE_PATTERN.split(original).length);
        analysis.put("sentences_compressed", 
            SENTENCE_PATTERN.split(compressed).length);
        return Map.copyOf(analysis);
    }

    private static class ScoredSentence {
        private final String text;
        private final double score;
        private final int originalIndex;

        public ScoredSentence(String text, double score, int originalIndex) {
            this.text = text;
            this.score = score;
            this.originalIndex = originalIndex;
        }

        public String getText() {
            return text;
        }

        public double getScore() {
            return score;
        }

        public int getOriginalIndex() {
            return originalIndex;
        }
    }
}
