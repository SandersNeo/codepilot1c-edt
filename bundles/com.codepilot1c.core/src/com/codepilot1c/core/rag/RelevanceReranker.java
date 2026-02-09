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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.codepilot1c.core.index.CodeChunk;
import com.codepilot1c.core.model.LlmMessage;
import com.codepilot1c.core.model.LlmRequest;
import com.codepilot1c.core.model.LlmResponse;
import com.codepilot1c.core.provider.ILlmProvider;
import com.codepilot1c.core.search.ISemanticSearch.SearchResult;

/**
 * Re-ranks search results using an LLM for better relevance.
 *
 * <p>Uses a small/fast LLM to score the relevance of each search result
 * against the user query, then re-orders results by these scores.</p>
 */
public class RelevanceReranker {

    private static final String RERANK_PROMPT_TEMPLATE =
            "Score the relevance of the following code snippet to the query.\n" + //$NON-NLS-1$
                    "Query: %s\n\n" + //$NON-NLS-1$
                    "Code snippet from %s:\n```\n%s\n```\n\n" + //$NON-NLS-1$
                    "Score from 0 to 10 (0 = not relevant, 10 = highly relevant).\n" + //$NON-NLS-1$
                    "Respond with ONLY the numeric score."; //$NON-NLS-1$

    private static final String BATCH_RERANK_PROMPT_TEMPLATE =
            "Score the relevance of each code snippet to the query.\n" + //$NON-NLS-1$
                    "Query: %s\n\n" + //$NON-NLS-1$
                    "Snippets:\n%s\n\n" + //$NON-NLS-1$
                    "For each snippet, provide a relevance score from 0-10.\n" + //$NON-NLS-1$
                    "Format: one score per line, in order."; //$NON-NLS-1$

    private final ILlmProvider llmProvider;
    private final int maxConcurrentRequests;
    private final boolean useBatchMode;

    /**
     * Creates a relevance reranker.
     *
     * @param llmProvider the LLM provider for scoring
     */
    public RelevanceReranker(ILlmProvider llmProvider) {
        this(llmProvider, 5, false);
    }

    /**
     * Creates a relevance reranker with options.
     *
     * @param llmProvider the LLM provider
     * @param maxConcurrentRequests max concurrent LLM requests
     * @param useBatchMode whether to use batch scoring (single LLM call)
     */
    public RelevanceReranker(ILlmProvider llmProvider, int maxConcurrentRequests, boolean useBatchMode) {
        this.llmProvider = llmProvider;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.useBatchMode = useBatchMode;
    }

    /**
     * Re-ranks search results based on relevance to the query.
     *
     * @param results the original search results
     * @param query the user query
     * @return re-ranked results
     */
    public List<SearchResult> rerank(List<SearchResult> results, String query) {
        if (results == null || results.isEmpty() || llmProvider == null) {
            return results;
        }

        try {
            if (useBatchMode) {
                return rerankBatch(results, query);
            } else {
                return rerankIndividual(results, query);
            }
        } catch (Exception e) {
            // On failure, return original results
            return results;
        }
    }

    /**
     * Re-ranks asynchronously.
     *
     * @param results the search results
     * @param query the query
     * @return future with re-ranked results
     */
    public CompletableFuture<List<SearchResult>> rerankAsync(List<SearchResult> results, String query) {
        return CompletableFuture.supplyAsync(() -> rerank(results, query));
    }

    /**
     * Re-ranks each result individually with separate LLM calls.
     */
    private List<SearchResult> rerankIndividual(List<SearchResult> results, String query) {
        List<RerankScore> scores = new ArrayList<>();

        // Score each result
        for (SearchResult result : results) {
            try {
                float score = scoreResult(result, query);
                scores.add(new RerankScore(result, score));
            } catch (Exception e) {
                // Keep original score on failure
                scores.add(new RerankScore(result, result.getScore()));
            }
        }

        // Sort by rerank score and convert back to SearchResult
        return scores.stream()
                .sorted(Comparator.comparingDouble(RerankScore::getScore).reversed())
                .map(rs -> new SearchResult(rs.getResult().getChunk(), rs.getScore()))
                .collect(Collectors.toList());
    }

    /**
     * Re-ranks all results with a single batch LLM call.
     */
    private List<SearchResult> rerankBatch(List<SearchResult> results, String query) {
        // Build snippets list
        StringBuilder snippetsBuilder = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            CodeChunk chunk = result.getChunk();
            snippetsBuilder.append(String.format(
                    "[%d] %s:\n%s\n\n", //$NON-NLS-1$
                    i + 1,
                    chunk.getFilePath(),
                    truncateContent(chunk.getContent(), 200)
            ));
        }

        String prompt = String.format(BATCH_RERANK_PROMPT_TEMPLATE, query, snippetsBuilder.toString());

        try {
            LlmRequest request = LlmRequest.builder()
                    .addMessage(LlmMessage.user(prompt))
                    .maxTokens(100)
                    .temperature(0.0f)
                    .build();

            LlmResponse response = llmProvider.complete(request).get();
            String[] scoreLines = response.getContent().trim().split("\n"); //$NON-NLS-1$

            List<RerankScore> scores = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                float score = results.get(i).getScore(); // Default to original
                if (i < scoreLines.length) {
                    try {
                        score = parseScore(scoreLines[i]) / 10.0f; // Normalize to 0-1
                    } catch (Exception ignored) {
                        // Keep original score
                    }
                }
                scores.add(new RerankScore(results.get(i), score));
            }

            return scores.stream()
                    .sorted(Comparator.comparingDouble(RerankScore::getScore).reversed())
                    .map(rs -> new SearchResult(rs.getResult().getChunk(), rs.getScore()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            return results;
        }
    }

    /**
     * Scores a single result against the query.
     */
    private float scoreResult(SearchResult result, String query) {
        CodeChunk chunk = result.getChunk();
        String prompt = String.format(RERANK_PROMPT_TEMPLATE,
                query,
                chunk.getFilePath(),
                truncateContent(chunk.getContent(), 500)
        );

        try {
            LlmRequest request = LlmRequest.builder()
                    .addMessage(LlmMessage.user(prompt))
                    .maxTokens(10)
                    .temperature(0.0f)
                    .build();

            LlmResponse response = llmProvider.complete(request).get();
            return parseScore(response.getContent()) / 10.0f; // Normalize to 0-1
        } catch (Exception e) {
            return result.getScore(); // Keep original score
        }
    }

    /**
     * Parses a numeric score from LLM response.
     */
    private float parseScore(String response) {
        // Extract first number from response
        String cleaned = response.trim().replaceAll("[^0-9.]", ""); //$NON-NLS-1$ //$NON-NLS-2$
        if (cleaned.isEmpty()) {
            return 5.0f; // Default middle score
        }

        float score = Float.parseFloat(cleaned);
        return Math.max(0, Math.min(10, score)); // Clamp to 0-10
    }

    /**
     * Truncates content to a maximum length.
     */
    private String truncateContent(String content, int maxChars) {
        if (content == null || content.length() <= maxChars) {
            return content;
        }
        return content.substring(0, maxChars) + "..."; //$NON-NLS-1$
    }

    /**
     * Helper class for storing rerank scores.
     */
    private static class RerankScore {
        private final SearchResult result;
        private final float score;

        RerankScore(SearchResult result, float score) {
            this.result = result;
            this.score = score;
        }

        SearchResult getResult() {
            return result;
        }

        float getScore() {
            return score;
        }
    }

    /**
     * Filters results by minimum rerank score.
     *
     * @param results the results to filter
     * @param minScore the minimum score (0-1)
     * @return filtered results
     */
    public List<SearchResult> filterByScore(List<SearchResult> results, float minScore) {
        return results.stream()
                .filter(r -> r.getScore() >= minScore)
                .collect(Collectors.toList());
    }
}
