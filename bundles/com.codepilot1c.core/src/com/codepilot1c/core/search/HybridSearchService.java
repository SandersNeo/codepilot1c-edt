/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.codepilot1c.core.index.CodeChunk;
import com.codepilot1c.core.index.ICodebaseIndex;
import com.codepilot1c.core.internal.VibeCorePlugin;
import com.codepilot1c.core.search.ISemanticSearch.SearchOptions;
import com.codepilot1c.core.search.ISemanticSearch.SearchResult;

/**
 * Hybrid search combining keyword and semantic search.
 *
 * <p>Uses Reciprocal Rank Fusion (RRF) to combine results from
 * both search methods for better recall and precision.</p>
 */
public class HybridSearchService {

    private static final float KEYWORD_WEIGHT = 0.4f;
    private static final float SEMANTIC_WEIGHT = 0.6f;
    private static final int RRF_K = 60; // RRF constant

    private final ICodebaseIndex codebaseIndex;
    private final ISemanticSearch semanticSearch;

    /**
     * Creates a hybrid search service.
     *
     * @param codebaseIndex the codebase index for keyword search
     * @param semanticSearch the semantic search service
     */
    public HybridSearchService(ICodebaseIndex codebaseIndex, ISemanticSearch semanticSearch) {
        this.codebaseIndex = codebaseIndex;
        this.semanticSearch = semanticSearch;
    }

    /**
     * Performs hybrid search combining keyword and semantic results.
     *
     * @param query the search query
     * @param topK the number of results to return
     * @return list of search results ordered by combined relevance
     */
    public List<SearchResult> search(String query, int topK) {
        return search(query, SearchOptions.defaults().topK(topK));
    }

    /**
     * Performs hybrid search with options.
     *
     * @param query the search query
     * @param options search options
     * @return list of search results
     */
    public List<SearchResult> search(String query, SearchOptions options) {
        try {
            // 1. Perform keyword search
            List<SearchResult> keywordResults = performKeywordSearch(query, options.getTopK() * 2);

            // 2. Perform semantic search
            List<SearchResult> semanticResults = semanticSearch.search(query, options);

            // 3. Combine results using RRF
            List<SearchResult> combined = reciprocalRankFusion(keywordResults, semanticResults);

            // 4. Apply filters and limit
            return combined.stream()
                    .filter(r -> r.getScore() >= options.getMinScore())
                    .limit(options.getTopK())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            VibeCorePlugin.logError("Hybrid search failed", e);
            // Fallback to semantic search only
            return semanticSearch.search(query, options);
        }
    }

    /**
     * Performs async hybrid search.
     *
     * @param query the search query
     * @param topK the number of results
     * @return future with search results
     */
    public CompletableFuture<List<SearchResult>> searchAsync(String query, int topK) {
        return CompletableFuture.supplyAsync(() -> search(query, topK));
    }

    /**
     * Performs keyword-based search using the codebase index.
     */
    private List<SearchResult> performKeywordSearch(String query, int topK) {
        List<SearchResult> results = new ArrayList<>();

        // Extract keywords from query
        String[] keywords = extractKeywords(query);

        if (keywords.length == 0) {
            return results;
        }

        // Search by content using index
        List<CodeChunk> chunks = codebaseIndex.searchByKeyword(String.join(" ", keywords), topK);

        for (int i = 0; i < chunks.size(); i++) {
            // Score based on position (BM25-like scoring simulation)
            float score = 1.0f / (1.0f + i);
            results.add(new SearchResult(chunks.get(i), score));
        }

        return results;
    }

    /**
     * Extracts search keywords from a query.
     */
    private String[] extractKeywords(String query) {
        // Remove common stop words and split
        String cleaned = query.toLowerCase()
                .replaceAll("[^a-zA-Zа-яА-Я0-9_\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String[] words = cleaned.split("\\s+");

        // Filter out very short words and common words
        Set<String> stopWords = Set.of(
                "the", "a", "an", "is", "are", "was", "were", "be", "been",
                "how", "what", "where", "when", "why", "which",
                "в", "на", "по", "для", "из", "как", "что", "где", "когда"
        );

        return java.util.Arrays.stream(words)
                .filter(w -> w.length() > 2)
                .filter(w -> !stopWords.contains(w))
                .toArray(String[]::new);
    }

    /**
     * Combines results using Reciprocal Rank Fusion (RRF).
     *
     * <p>RRF formula: score = sum(1 / (k + rank)) for each result list</p>
     */
    private List<SearchResult> reciprocalRankFusion(
            List<SearchResult> keywordResults,
            List<SearchResult> semanticResults) {

        Map<String, Float> combinedScores = new HashMap<>();
        Map<String, CodeChunk> chunks = new HashMap<>();

        // Process keyword results
        for (int i = 0; i < keywordResults.size(); i++) {
            SearchResult result = keywordResults.get(i);
            String id = result.getChunk().getId();
            float rrfScore = KEYWORD_WEIGHT / (RRF_K + i + 1);
            combinedScores.merge(id, rrfScore, Float::sum);
            chunks.putIfAbsent(id, result.getChunk());
        }

        // Process semantic results
        for (int i = 0; i < semanticResults.size(); i++) {
            SearchResult result = semanticResults.get(i);
            String id = result.getChunk().getId();
            float rrfScore = SEMANTIC_WEIGHT / (RRF_K + i + 1);
            combinedScores.merge(id, rrfScore, Float::sum);
            chunks.putIfAbsent(id, result.getChunk());
        }

        // Convert to results and sort by combined score
        return combinedScores.entrySet().stream()
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .map(entry -> new SearchResult(chunks.get(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Deduplicates results based on chunk ID.
     */
    public List<SearchResult> deduplicate(List<SearchResult> results) {
        Set<String> seen = new HashSet<>();
        List<SearchResult> unique = new ArrayList<>();

        for (SearchResult result : results) {
            if (seen.add(result.getChunk().getId())) {
                unique.add(result);
            }
        }

        return unique;
    }
}
