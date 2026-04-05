package com.piloto.cdi.kernel.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Deterministic Ingress Sanitizer to prevent Adversarial Attacks (Jailbreaks).
 * Implements heuristic-based filtering before the reasoning context is built.
 */
public class AdversarialSanitizer {
    private static final Logger logger = LoggerFactory.getLogger(AdversarialSanitizer.class);

    private final List<Pattern> blacklistedPatterns;

    public AdversarialSanitizer() {
        this.blacklistedPatterns = new ArrayList<>();
        // Basic Jailbreak Heuristics
        blacklistedPatterns.add(Pattern.compile("(?i).*ignore previous instructions.*"));
        blacklistedPatterns.add(Pattern.compile("(?i).*you are now a.*"));
        blacklistedPatterns.add(Pattern.compile("(?i).*DAN mode.*"));
        blacklistedPatterns.add(Pattern.compile("(?i).*bypass safety.*"));
        blacklistedPatterns.add(Pattern.compile("(?i).*system override.*"));
        blacklistedPatterns.add(Pattern.compile("(?i).*developer mode enabled.*"));
    }

    /**
     * Sanitizes the input goal/prompt.
     * Throws SecurityException if an adversarial pattern is detected.
     */
    public String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        for (Pattern pattern : blacklistedPatterns) {
            if (pattern.matcher(input).find()) {
                logger.warn("Adversarial attack detected: Input matches blacklisted pattern '{}'", pattern.pattern());
                throw new SecurityException("Adversarial attack detected. Operation aborted for safety.");
            }
        }

        return input;
    }
}
