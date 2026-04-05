package com.piloto.cdi.gateway.llm.util;

public final class LLMJsonUtils {

    private LLMJsonUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Sanitizes a JSON string by removing markdown code blocks.
     * This handles cases where LLMs wrap JSON in ```json ... ``` blocks.
     * 
     * @param content The raw string content from the LLM.
     * @return The sanitized JSON string ready for parsing.
     */
    public static String cleanJson(String content) {
        if (content == null) {
            return null;
        }
        String cleaned = content.trim();

        // Step 1: Strip markdown code blocks
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        // Step 2: Extract JSON object/array from surrounding text
        // Some LLMs add commentary before/after the JSON payload
        int jsonStart = cleaned.indexOf('{');
        int arrayStart = cleaned.indexOf('[');
        if (jsonStart < 0 && arrayStart < 0) {
            return cleaned; // No JSON structure found, return as-is
        }
        int start;
        char openChar;
        char closeChar;
        if (arrayStart >= 0 && (jsonStart < 0 || arrayStart < jsonStart)) {
            start = arrayStart;
            openChar = '[';
            closeChar = ']';
        } else {
            start = jsonStart;
            openChar = '{';
            closeChar = '}';
        }
        // Find matching closing bracket
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        int end = -1;
        for (int i = start; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString) {
                if (c == openChar)
                    depth++;
                else if (c == closeChar) {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
        }
        if (end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }

        // Step 3: Fix double-escaped quotes (\\\" -> \")
        // This happens when the LLM wraps the JSON in a string literal
        if (cleaned.contains("\\\"")) {
            cleaned = cleaned.replace("\\\"", "\"");
            // After unescaping, re-extract the JSON if it was a string-wrapped blob
            cleaned = cleaned.trim();
            if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
            }
        }

        // Step 4: Fix escaped newlines and tabs
        cleaned = cleaned.replace("\\n", "\n").replace("\\t", "\t");

        return cleaned.trim();
    }
}
