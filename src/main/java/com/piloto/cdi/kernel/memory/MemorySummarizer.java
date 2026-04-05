package com.piloto.cdi.kernel.memory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Memory summarizer for context compression.
 * 
 * <p>Combines multiple memory entries into a concise aggregated context,
 * preventing context overflow in LLM prompts.</p>
 * 
 * <p><b>Phase 4 Implementation:</b></p>
 * <ul>
 *   <li>Deterministic: Uses deduplication + truncation (no LLM)</li>
 *   <li>Fast: O(N) processing</li>
 *   <li>Configurable: Max output length parameter</li>
 * </ul>
 * 
 * <p><b>Future Phases:</b></p>
 * <ul>
 *   <li>LLM-based abstractive summarization</li>
 *   <li>Importance-weighted selection</li>
 *   <li>Hierarchical summarization</li>
 * </ul>
 * 
 * @since Phase 4
 */
public class MemorySummarizer {
    
    private final int maxOutputLength;
    private static final String SEPARATOR = "\n\n---\n\n";
    private static final int DEFAULT_MAX_LENGTH = 4000; // chars
    
    /**
     * Constructs summarizer with specified max output length.
     * 
     * @param maxOutputLength maximum characters in output (must be > 0)
     */
    public MemorySummarizer(int maxOutputLength) {
        if (maxOutputLength <= 0) {
            throw new IllegalArgumentException("maxOutputLength must be > 0, got: " + maxOutputLength);
        }
        this.maxOutputLength = maxOutputLength;
    }
    
    /**
     * Default constructor with 4000 char limit.
     */
    public MemorySummarizer() {
        this(DEFAULT_MAX_LENGTH);
    }
    
    /**
     * Summarizes list of memory entries into concise aggregated context.
     * 
     * <p><b>Algorithm (Phase 4 Deterministic):</b></p>
     * <ol>
     *   <li>Deduplicate by content (exact match)</li>
     *   <li>Sort by relevance score (if present) or version (newer first)</li>
     *   <li>Concatenate with separators</li>
     *   <li>Truncate to maxOutputLength if needed</li>
     * </ol>
     * 
     * @param entries list of memory entries to summarize
     * @return CompletableFuture resolving to summarized text
     */
    public CompletableFuture<String> summarize(List<MemoryEntry> entries) {
        Objects.requireNonNull(entries, "entries cannot be null");
        
        return CompletableFuture.supplyAsync(() -> {
            if (entries.isEmpty()) {
                return "";
            }
            
            // Step 1: Deduplicate by content
            Set<String> seenContent = new HashSet<>();
            List<MemoryEntry> uniqueEntries = entries.stream()
                .filter(entry -> seenContent.add(entry.getContent()))
                .collect(Collectors.toList());
            
            // Step 2: Sort by relevance score (descending) or version (descending)
            uniqueEntries.sort((e1, e2) -> {
                // Prioritize relevance score if present
                if (e1.getRelevanceScore().isPresent() && e2.getRelevanceScore().isPresent()) {
                    return Double.compare(e2.getRelevanceScore().get(), e1.getRelevanceScore().get());
                }
                // Fall back to version (newer first)
                return Integer.compare(e2.getVersion(), e1.getVersion());
            });
            
            // Step 3: Concatenate with separators
            StringBuilder aggregated = new StringBuilder();
            for (MemoryEntry entry : uniqueEntries) {
                String content = entry.getContent();
                
                // Check if adding this entry would exceed limit
                int futureLength = aggregated.length() + content.length() + SEPARATOR.length();
                if (futureLength > maxOutputLength) {
                    // Truncate last entry to fit
                    int remainingSpace = maxOutputLength - aggregated.length() - SEPARATOR.length() - 20; // buffer
                    if (remainingSpace > 100) {
                        aggregated.append(content, 0, remainingSpace);
                        aggregated.append("... [truncated]");
                    }
                    break;
                }
                
                if (aggregated.length() > 0) {
                    aggregated.append(SEPARATOR);
                }
                aggregated.append(content);
            }
            
            return aggregated.toString();
        });
    }
    
    /**
     * Summarizes with custom separator.
     * 
     * @param entries list of memory entries
     * @param separator custom separator between entries
     * @return CompletableFuture resolving to summarized text
     */
    public CompletableFuture<String> summarizeWithSeparator(List<MemoryEntry> entries, String separator) {
        Objects.requireNonNull(entries, "entries cannot be null");
        Objects.requireNonNull(separator, "separator cannot be null");
        
        // Temporarily override separator (not thread-safe, but simple for Phase 4)
        return CompletableFuture.supplyAsync(() -> {
            if (entries.isEmpty()) {
                return "";
            }
            
            Set<String> seenContent = new HashSet<>();
            List<MemoryEntry> uniqueEntries = entries.stream()
                .filter(entry -> seenContent.add(entry.getContent()))
                .sorted((e1, e2) -> {
                    if (e1.getRelevanceScore().isPresent() && e2.getRelevanceScore().isPresent()) {
                        return Double.compare(e2.getRelevanceScore().get(), e1.getRelevanceScore().get());
                    }
                    return Integer.compare(e2.getVersion(), e1.getVersion());
                })
                .collect(Collectors.toList());
            
            StringBuilder aggregated = new StringBuilder();
            for (MemoryEntry entry : uniqueEntries) {
                String content = entry.getContent();
                int futureLength = aggregated.length() + content.length() + separator.length();
                
                if (futureLength > maxOutputLength) {
                    int remainingSpace = maxOutputLength - aggregated.length() - separator.length() - 20;
                    if (remainingSpace > 100) {
                        aggregated.append(content, 0, remainingSpace);
                        aggregated.append("... [truncated]");
                    }
                    break;
                }
                
                if (aggregated.length() > 0) {
                    aggregated.append(separator);
                }
                aggregated.append(content);
            }
            
            return aggregated.toString();
        });
    }
}
