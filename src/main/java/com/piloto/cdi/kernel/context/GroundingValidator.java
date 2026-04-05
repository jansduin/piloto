package com.piloto.cdi.kernel.context;

import java.util.*;
import java.util.regex.Pattern;

public class GroundingValidator {

    private static final Pattern CATEGORICAL_PATTERN = Pattern.compile(
            "\\b(always|never|all|none|every|must|cannot|impossible|definitely|certainly)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Set<String> UNGROUNDED_INDICATORS = Set.of(
            "i think", "probably", "maybe", "it seems", "appears to",
            "might be", "could be", "as far as i know");

    public Map<String, Object> validate(String response, String context) {
        if (response == null)
            response = "";
        if (context == null)
            context = "";

        List<String> unsupportedClaims = new ArrayList<>();

        unsupportedClaims.addAll(detectUnsupportedFacts(response, context));
        unsupportedClaims.addAll(detectCategoricalClaims(response, context));
        unsupportedClaims.addAll(detectGoalChanges(response, context));

        boolean grounded = unsupportedClaims.isEmpty();

        Map<String, Object> result = new HashMap<>();
        result.put("grounded", grounded);
        result.put("unsupported_claims", List.copyOf(unsupportedClaims));
        result.put("confidence_score", calculateConfidenceScore(response, unsupportedClaims.size()));

        return Map.copyOf(result);
    }

    private List<String> detectUnsupportedFacts(String response, String context) {
        List<String> unsupported = new ArrayList<>();

        String[] responseSentences = response.split("[.!?]\\s+");
        Set<String> contextWords = extractSignificantWords(context.toLowerCase());

        for (String sentence : responseSentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty() || sentence.length() < 20) {
                continue;
            }

            if (containsSpecificClaim(sentence) && !isSupported(sentence, contextWords)) {
                unsupported.add("Unsupported factual claim: " + truncate(sentence, 80));
            }
        }

        return unsupported;
    }

    private List<String> detectCategoricalClaims(String response, String context) {
        List<String> unsupported = new ArrayList<>();

        String[] responseSentences = response.split("[.!?]\\s+");
        Set<String> contextWords = extractSignificantWords(context.toLowerCase());

        for (String sentence : responseSentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) {
                continue;
            }

            if (CATEGORICAL_PATTERN.matcher(sentence).find()) {
                if (!isSupported(sentence, contextWords)) {
                    unsupported.add("Categorical claim without context support: " +
                            truncate(sentence, 80));
                }
            }
        }

        return unsupported;
    }

    private List<String> detectGoalChanges(String response, String context) {
        List<String> unsupported = new ArrayList<>();

        String responseLower = response.toLowerCase();
        String contextLower = context.toLowerCase();

        if (contextLower.contains("goal:") || contextLower.contains("objective:")) {
            String[] contextLines = contextLower.split("\n");
            String originalGoal = null;

            for (String line : contextLines) {
                if (line.contains("goal:") || line.contains("objective:")) {
                    originalGoal = line;
                    break;
                }
            }

            if (originalGoal != null &&
                    (responseLower.contains("new goal") ||
                            responseLower.contains("change goal") ||
                            responseLower.contains("different objective"))) {
                unsupported.add("Response attempts to change original goal without justification");
            }
        }

        return unsupported;
    }

    private boolean containsSpecificClaim(String sentence) {
        sentence = sentence.toLowerCase();

        return sentence.matches(".*\\b(is|are|was|were|has|have|will|did)\\b.*") &&
                !sentence.matches(".*\\?.*") &&
                UNGROUNDED_INDICATORS.stream().noneMatch(sentence::contains);
    }

    private boolean isSupported(String claim, Set<String> contextWords) {
        Set<String> claimWords = extractSignificantWords(claim.toLowerCase());

        if (claimWords.isEmpty()) {
            return true;
        }

        long matchCount = claimWords.stream()
                .filter(contextWords::contains)
                .count();

        double supportRatio = (double) matchCount / claimWords.size();
        return supportRatio >= 0.6;
    }

    private Set<String> extractSignificantWords(String text) {
        Set<String> words = new HashSet<>();
        String[] tokens = text.split("\\s+");

        for (String token : tokens) {
            token = token.replaceAll("[^a-z0-9]", "");
            if (token.length() > 3) {
                words.add(token);
            }
        }

        return words;
    }

    private double calculateConfidenceScore(String response, int unsupportedCount) {
        if (response.isEmpty()) {
            return 0.0;
        }

        int sentenceCount = response.split("[.!?]\\s+").length;
        double penalty = Math.min(1.0, (double) unsupportedCount / Math.max(1, sentenceCount));

        return Math.max(0.0, 1.0 - penalty);
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
