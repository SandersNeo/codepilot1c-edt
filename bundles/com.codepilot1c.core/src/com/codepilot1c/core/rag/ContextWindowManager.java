/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.rag;

/**
 * Manages the context window for LLM requests.
 *
 * <p>Handles token estimation and text truncation to fit within
 * the model's context window limits.</p>
 */
public class ContextWindowManager {

    /**
     * Average characters per token for code.
     * This is a rough estimate - actual tokenization varies by model.
     */
    private static final double CHARS_PER_TOKEN = 3.5;

    /**
     * Safety margin to prevent exceeding limits (10%).
     */
    private static final double SAFETY_MARGIN = 0.9;

    private final int maxTokens;
    private final int effectiveMaxTokens;

    /**
     * Creates a context window manager.
     *
     * @param maxTokens the maximum number of tokens for the context
     */
    public ContextWindowManager(int maxTokens) {
        this.maxTokens = maxTokens;
        this.effectiveMaxTokens = (int) (maxTokens * SAFETY_MARGIN);
    }

    /**
     * Estimates the number of tokens in a text.
     *
     * @param text the text to estimate
     * @return estimated token count
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Simple estimation based on character count
        // A more accurate implementation would use the model's tokenizer
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }

    /**
     * Checks if additional tokens can fit in the context window.
     *
     * @param currentTokens the current token count
     * @param additionalTokens the tokens to add
     * @return true if they fit
     */
    public boolean canFit(int currentTokens, int additionalTokens) {
        return currentTokens + additionalTokens <= effectiveMaxTokens;
    }

    /**
     * Checks if a total token count fits in the context window.
     *
     * @param totalTokens the total token count
     * @return true if it fits
     */
    public boolean canFit(int totalTokens) {
        return totalTokens <= effectiveMaxTokens;
    }

    /**
     * Returns the remaining tokens available.
     *
     * @param currentTokens the current token count
     * @return remaining available tokens
     */
    public int getRemainingTokens(int currentTokens) {
        return Math.max(0, effectiveMaxTokens - currentTokens);
    }

    /**
     * Truncates text to fit within remaining space.
     *
     * @param text the text to truncate
     * @param currentTokens the current token count
     * @return truncated text, or null if no space available
     */
    public String truncateToFit(String text, int currentTokens) {
        int remaining = getRemainingTokens(currentTokens);

        if (remaining <= 0) {
            return null;
        }

        int textTokens = estimateTokens(text);

        if (textTokens <= remaining) {
            return text;
        }

        // Calculate how many characters we can include
        int maxChars = (int) (remaining * CHARS_PER_TOKEN);

        if (maxChars <= 10) {
            return null;
        }

        // Truncate with ellipsis
        if (maxChars < text.length()) {
            return text.substring(0, maxChars - 3) + "..."; //$NON-NLS-1$
        }

        return text;
    }

    /**
     * Truncates text from the start to fit within token limit.
     *
     * @param text the text to truncate
     * @param maxTokens the maximum tokens allowed
     * @return truncated text with content from the end preserved
     */
    public String truncateFromStart(String text, int maxTokens) {
        int textTokens = estimateTokens(text);

        if (textTokens <= maxTokens) {
            return text;
        }

        // Calculate how many characters we can include from the end
        int maxChars = (int) (maxTokens * CHARS_PER_TOKEN);

        if (maxChars <= 10) {
            return ""; //$NON-NLS-1$
        }

        // Find a good break point (newline or space)
        int startIndex = text.length() - maxChars + 3; // +3 for "..."
        startIndex = Math.max(0, startIndex);

        // Try to find a newline to start from
        int newlineIndex = text.indexOf('\n', startIndex);
        if (newlineIndex > 0 && newlineIndex < startIndex + 100) {
            startIndex = newlineIndex + 1;
        }

        return "..." + text.substring(startIndex); //$NON-NLS-1$
    }

    /**
     * Splits text into chunks that fit within the token limit.
     *
     * @param text the text to split
     * @param chunkTokens the maximum tokens per chunk
     * @return list of chunks
     */
    public java.util.List<String> splitIntoChunks(String text, int chunkTokens) {
        java.util.List<String> chunks = new java.util.ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int chunkChars = (int) (chunkTokens * CHARS_PER_TOKEN);
        String[] lines = text.split("\n"); //$NON-NLS-1$

        StringBuilder currentChunk = new StringBuilder();
        int currentChars = 0;

        for (String line : lines) {
            int lineChars = line.length() + 1; // +1 for newline

            if (currentChars + lineChars > chunkChars && currentChunk.length() > 0) {
                // Start new chunk
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
                currentChars = 0;
            }

            currentChunk.append(line).append("\n"); //$NON-NLS-1$
            currentChars += lineChars;
        }

        // Add last chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    /**
     * Returns the maximum tokens allowed.
     *
     * @return max tokens
     */
    public int getMaxTokens() {
        return maxTokens;
    }

    /**
     * Returns the effective max tokens (after safety margin).
     *
     * @return effective max tokens
     */
    public int getEffectiveMaxTokens() {
        return effectiveMaxTokens;
    }

    /**
     * Calculates the percentage of context used.
     *
     * @param currentTokens the current token count
     * @return usage percentage (0-100)
     */
    public double getUsagePercent(int currentTokens) {
        return (double) currentTokens / effectiveMaxTokens * 100;
    }
}
