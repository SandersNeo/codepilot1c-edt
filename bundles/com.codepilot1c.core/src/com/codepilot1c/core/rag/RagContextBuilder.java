/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.codepilot1c.core.index.CodeChunk;
import com.codepilot1c.core.search.ISemanticSearch.SearchResult;

/**
 * Builds context for RAG (Retrieval-Augmented Generation) from search results.
 *
 * <p>Combines retrieved code chunks with the user query to create
 * an enriched context for the LLM.</p>
 */
public class RagContextBuilder {

    private static final String CONTEXT_HEADER = "### Relevant Code Context\n\n"; //$NON-NLS-1$
    private static final String CHUNK_TEMPLATE = "**File:** `%s` (lines %d-%d)\n**Symbol:** %s\n```bsl\n%s\n```\n\n"; //$NON-NLS-1$
    private static final String QUERY_TEMPLATE = "### User Query\n\n%s\n\n"; //$NON-NLS-1$
    private static final String INSTRUCTIONS_TEMPLATE = "### Instructions\n\n%s\n\n"; //$NON-NLS-1$

    private final ContextWindowManager contextWindowManager;

    /**
     * Creates a RAG context builder with the given context window manager.
     *
     * @param contextWindowManager the context window manager
     */
    public RagContextBuilder(ContextWindowManager contextWindowManager) {
        this.contextWindowManager = contextWindowManager;
    }

    /**
     * Creates a RAG context builder with default settings.
     *
     * @param maxTokens the maximum number of tokens for context
     */
    public RagContextBuilder(int maxTokens) {
        this.contextWindowManager = new ContextWindowManager(maxTokens);
    }

    /**
     * Builds RAG context from search results and user query.
     *
     * @param searchResults the search results
     * @param userQuery the user query
     * @return the built context
     */
    public RagContext build(List<SearchResult> searchResults, String userQuery) {
        return build(searchResults, userQuery, null);
    }

    /**
     * Builds RAG context with custom instructions.
     *
     * @param searchResults the search results
     * @param userQuery the user query
     * @param instructions custom instructions for the LLM
     * @return the built context
     */
    public RagContext build(List<SearchResult> searchResults, String userQuery, String instructions) {
        StringBuilder contextBuilder = new StringBuilder();
        List<CodeChunk> includedChunks = new ArrayList<>();
        List<RenderedChunk> renderedChunks = new ArrayList<>();
        int totalTokens = 0;

        // Add instructions if provided
        if (instructions != null && !instructions.isEmpty()) {
            String instructionsSection = String.format(INSTRUCTIONS_TEMPLATE, instructions);
            int instructionsTokens = contextWindowManager.estimateTokens(instructionsSection);
            if (contextWindowManager.canFit(totalTokens + instructionsTokens)) {
                contextBuilder.append(instructionsSection);
                totalTokens += instructionsTokens;
            }
        }

        // Add context header
        String header = CONTEXT_HEADER;
        int headerTokens = contextWindowManager.estimateTokens(header);
        if (contextWindowManager.canFit(totalTokens + headerTokens)) {
            contextBuilder.append(header);
            totalTokens += headerTokens;
        }

        // Add search results (ordered by relevance)
        List<SearchResult> sortedResults = searchResults.stream()
                .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
                .collect(Collectors.toList());

        for (SearchResult result : sortedResults) {
            CodeChunk chunk = result.getChunk();
            // Format chunk with token-aware truncation of *content* (not the whole template).
            // This prevents a mismatch where the caller re-renders the full content and
            // unintentionally exceeds the token budget.
            String chunkText = formatChunk(chunk, chunk.getContent());
            int chunkTokens = contextWindowManager.estimateTokens(chunkText);

            if (contextWindowManager.canFit(totalTokens + chunkTokens)) {
                contextBuilder.append(chunkText);
                includedChunks.add(chunk);
                totalTokens += chunkTokens;
                renderedChunks.add(new RenderedChunk(chunk, chunk.getContent(), false, chunkTokens));
            } else {
                // Try to fit a truncated version by truncating only the code content.
                String overhead = formatChunk(chunk, ""); //$NON-NLS-1$
                int overheadTokens = contextWindowManager.estimateTokens(overhead);
                if (!contextWindowManager.canFit(totalTokens + overheadTokens)) {
                    break;
                }

                String truncatedContent = contextWindowManager.truncateToFit(
                        chunk.getContent(), totalTokens + overheadTokens);
                if (truncatedContent == null || truncatedContent.isEmpty()) {
                    break;
                }

                boolean truncated = !truncatedContent.equals(chunk.getContent());
                String rendered = formatChunk(chunk, truncatedContent);
                int renderedTokens = contextWindowManager.estimateTokens(rendered);
                if (contextWindowManager.canFit(totalTokens + renderedTokens)) {
                    contextBuilder.append(rendered);
                    includedChunks.add(chunk);
                    totalTokens += renderedTokens;
                    renderedChunks.add(new RenderedChunk(chunk, truncatedContent, truncated, renderedTokens));
                }
                break;
            }
        }

        // Add user query
        String querySection = String.format(QUERY_TEMPLATE, userQuery);
        int queryTokens = contextWindowManager.estimateTokens(querySection);
        if (contextWindowManager.canFit(totalTokens + queryTokens)) {
            contextBuilder.append(querySection);
            totalTokens += queryTokens;
        }

        return new RagContext(
                contextBuilder.toString(),
                includedChunks,
                renderedChunks,
                totalTokens,
                searchResults.size(),
                includedChunks.size()
        );
    }

    /**
     * Builds context for code completion (FIM - Fill In the Middle).
     *
     * @param prefix the code before cursor
     * @param suffix the code after cursor
     * @param relevantChunks relevant code from the project
     * @return the built context
     */
    public RagContext buildCompletionContext(String prefix, String suffix, List<SearchResult> relevantChunks) {
        StringBuilder contextBuilder = new StringBuilder();
        List<CodeChunk> includedChunks = new ArrayList<>();
        List<RenderedChunk> renderedChunks = new ArrayList<>();
        int totalTokens = 0;

        // Add relevant context first
        if (relevantChunks != null && !relevantChunks.isEmpty()) {
            contextBuilder.append("// Relevant code from the project:\n\n"); //$NON-NLS-1$
            totalTokens += contextWindowManager.estimateTokens("// Relevant code from the project:\n\n"); //$NON-NLS-1$

            for (SearchResult result : relevantChunks) {
                CodeChunk chunk = result.getChunk();
                String chunkComment = String.format(
                        "// From %s:\n%s\n\n", //$NON-NLS-1$
                        chunk.getFilePath(),
                        chunk.getContent()
                );
                int chunkTokens = contextWindowManager.estimateTokens(chunkComment);

                if (contextWindowManager.canFit(totalTokens + chunkTokens)) {
                    contextBuilder.append(chunkComment);
                    includedChunks.add(chunk);
                    totalTokens += chunkTokens;
                    renderedChunks.add(new RenderedChunk(chunk, chunk.getContent(), false, chunkTokens));
                }
            }
        }

        // Add separator
        contextBuilder.append("// Current file:\n"); //$NON-NLS-1$
        totalTokens += contextWindowManager.estimateTokens("// Current file:\n"); //$NON-NLS-1$

        // Add prefix (truncate from the beginning if necessary)
        int prefixTokens = contextWindowManager.estimateTokens(prefix);
        int suffixTokens = contextWindowManager.estimateTokens(suffix);
        int remainingTokens = contextWindowManager.getRemainingTokens(totalTokens);

        // Reserve some space for suffix
        int suffixReserve = Math.min(suffixTokens, remainingTokens / 4);
        int prefixAllowance = remainingTokens - suffixReserve;

        if (prefixTokens > prefixAllowance) {
            // Truncate prefix from the beginning
            prefix = contextWindowManager.truncateFromStart(prefix, prefixAllowance);
            prefixTokens = contextWindowManager.estimateTokens(prefix);
        }

        contextBuilder.append(prefix);
        totalTokens += prefixTokens;

        // Add cursor marker
        contextBuilder.append("<|cursor|>"); //$NON-NLS-1$
        totalTokens += 1;

        // Add suffix (truncate from the end if necessary)
        int remainingForSuffix = contextWindowManager.getRemainingTokens(totalTokens);
        if (suffixTokens > remainingForSuffix) {
            suffix = contextWindowManager.truncateToFit(suffix, totalTokens);
            if (suffix == null) {
                suffix = ""; //$NON-NLS-1$
            }
        }

        contextBuilder.append(suffix);
        totalTokens += contextWindowManager.estimateTokens(suffix);

        return new RagContext(
                contextBuilder.toString(),
                includedChunks,
                renderedChunks,
                totalTokens,
                relevantChunks != null ? relevantChunks.size() : 0,
                includedChunks.size()
        );
    }

    /**
     * Formats a code chunk for inclusion in context.
     */
    private String formatChunk(CodeChunk chunk, String content) {
        String symbol = chunk.getSymbolName();
        if (symbol == null) {
            symbol = ""; //$NON-NLS-1$
        }
        return String.format(CHUNK_TEMPLATE,
                chunk.getFilePath(),
                chunk.getStartLine(),
                chunk.getEndLine(),
                symbol,
                content != null ? content : "" //$NON-NLS-1$
        );
    }

    /**
     * Result of building RAG context.
     */
    public static class RagContext {
        private final String context;
        private final List<CodeChunk> includedChunks;
        private final List<RenderedChunk> renderedChunks;
        private final int totalTokens;
        private final int totalResults;
        private final int includedResults;

        public RagContext(String context, List<CodeChunk> includedChunks, List<RenderedChunk> renderedChunks,
                          int totalTokens, int totalResults, int includedResults) {
            this.context = context;
            this.includedChunks = includedChunks;
            this.renderedChunks = renderedChunks;
            this.totalTokens = totalTokens;
            this.totalResults = totalResults;
            this.includedResults = includedResults;
        }

        public String getContext() {
            return context;
        }

        public List<CodeChunk> getIncludedChunks() {
            return includedChunks;
        }

        /**
         * Returns the chunks that were actually included in the context, with the exact code content
         * that fit the token budget (may be truncated).
         */
        public List<RenderedChunk> getRenderedChunks() {
            return renderedChunks;
        }

        public int getTotalTokens() {
            return totalTokens;
        }

        public int getTotalResults() {
            return totalResults;
        }

        public int getIncludedResults() {
            return includedResults;
        }

        public boolean hasContext() {
            return context != null && !context.isEmpty() && renderedChunks != null && !renderedChunks.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("RagContext[tokens=%d, included=%d/%d chunks]",
                    totalTokens, includedResults, totalResults);
        }
    }

    /**
     * A chunk selected for inclusion, with the exact content used in the prompt.
     */
    public static class RenderedChunk {
        private final CodeChunk chunk;
        private final String content;
        private final boolean truncated;
        private final int tokens;

        public RenderedChunk(CodeChunk chunk, String content, boolean truncated, int tokens) {
            this.chunk = chunk;
            this.content = content;
            this.truncated = truncated;
            this.tokens = tokens;
        }

        public CodeChunk getChunk() {
            return chunk;
        }

        public String getContent() {
            return content;
        }

        public boolean isTruncated() {
            return truncated;
        }

        public int getTokens() {
            return tokens;
        }
    }
}
