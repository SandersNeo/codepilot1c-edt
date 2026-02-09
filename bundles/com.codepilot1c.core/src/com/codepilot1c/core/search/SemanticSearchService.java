/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.codepilot1c.core.embedding.EmbeddingResult;
import com.codepilot1c.core.embedding.IEmbeddingProvider;
import com.codepilot1c.core.index.CodeChunk;
import com.codepilot1c.core.index.CodeSearchHit;
import com.codepilot1c.core.index.ICodebaseIndex;
import com.codepilot1c.core.logging.LogSanitizer;
import com.codepilot1c.core.logging.VibeLogger;

/**
 * Semantic search service using vector embeddings.
 *
 * <p>Converts queries to embeddings and searches the vector index
 * for similar code chunks.</p>
 */
public class SemanticSearchService implements ISemanticSearch {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(SemanticSearchService.class);

    private final ICodebaseIndex codebaseIndex;
    private final IEmbeddingProvider embeddingProvider;

    /**
     * Creates a semantic search service.
     *
     * @param codebaseIndex the codebase index for vector search
     * @param embeddingProvider the embedding provider for query encoding
     */
    public SemanticSearchService(ICodebaseIndex codebaseIndex, IEmbeddingProvider embeddingProvider) {
        this.codebaseIndex = codebaseIndex;
        this.embeddingProvider = embeddingProvider;
    }

    @Override
    public boolean isReady() {
        return codebaseIndex != null && embeddingProvider != null && embeddingProvider.isConfigured();
    }

    @Override
    public List<SearchResult> search(String query, int topK) {
        return search(query, SearchOptions.defaults().topK(topK));
    }

    @Override
    public CompletableFuture<List<SearchResult>> searchAsync(String query, int topK) {
        return CompletableFuture.supplyAsync(() -> search(query, topK));
    }

    @Override
    public List<SearchResult> search(String query, SearchOptions options) {
        if (!isReady()) {
            LOG.warn("Семантический поиск не готов: индекс или embedding провайдер не настроен"); //$NON-NLS-1$
            return Collections.emptyList();
        }

        long startTime = System.currentTimeMillis();
        LOG.debug("Семантический поиск: query=%s, topK=%d, minScore=%.2f", //$NON-NLS-1$
                LogSanitizer.truncate(query, 80), options.getTopK(), options.getMinScore());

        try {
            // 1. Generate query embedding
            long embedStart = System.currentTimeMillis();
            EmbeddingResult queryEmbedding = embeddingProvider.embed(query).get();
            LOG.debug("Embedding запроса получен за %s, размерность=%d", //$NON-NLS-1$
                    LogSanitizer.formatDuration(System.currentTimeMillis() - embedStart),
                    queryEmbedding.getEmbedding().length);

            // 2. Search vector index using KNN
            long knnStart = System.currentTimeMillis();
            List<CodeSearchHit> hits = codebaseIndex.searchKnn(
                    queryEmbedding.getEmbedding(),
                    options.getTopK() * 2 // Get more results for filtering
            );
            LOG.debug("KNN поиск: %d результатов за %s", //$NON-NLS-1$
                    hits.size(), LogSanitizer.formatDuration(System.currentTimeMillis() - knnStart));

            // 3. Convert to SearchResults and apply filters
            int filteredByScore = 0;
            int filteredByPattern = 0;
            int filteredByKind = 0;
            List<SearchResult> results = new ArrayList<>();

            for (CodeSearchHit hit : hits) {
                // Apply minimum score filter
                if (hit.getScore() < options.getMinScore()) {
                    filteredByScore++;
                    continue;
                }

                // Apply file pattern filter
                if (options.getFilePattern() != null && !options.getFilePattern().isEmpty()) {
                    if (!matchesPattern(hit.getChunk().getFilePath(), options.getFilePattern())) {
                        filteredByPattern++;
                        continue;
                    }
                }

                // Apply entity kind filter
                if (options.getEntityKind() != null && !options.getEntityKind().isEmpty()) {
                    if (!options.getEntityKind().equalsIgnoreCase(hit.getEntityKind())) {
                        filteredByKind++;
                        continue;
                    }
                }

                // Optionally expand context
                CodeChunk chunk = hit.getChunk();
                if (options.isExpandContext() && options.getContextLines() > 0) {
                    chunk = expandChunkContext(chunk, options.getContextLines());
                }

                results.add(new SearchResult(chunk, hit.getScore()));

                // Stop if we have enough results
                if (results.size() >= options.getTopK()) {
                    break;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            LOG.debug("Поиск завершён за %s: %d результатов (отфильтровано: по score=%d, по паттерну=%d, по типу=%d)", //$NON-NLS-1$
                    LogSanitizer.formatDuration(duration), results.size(),
                    filteredByScore, filteredByPattern, filteredByKind);

            return results;

        } catch (Exception e) {
            LOG.error("Ошибка семантического поиска: %s", e.getMessage()); //$NON-NLS-1$
            return Collections.emptyList();
        }
    }

    /**
     * Expands a chunk to include surrounding context lines.
     */
    private CodeChunk expandChunkContext(CodeChunk chunk, int contextLines) {
        // For now, return the original chunk
        // TODO: Implement context expansion by reading surrounding lines from file
        return chunk;
    }

    /**
     * Checks if a file path matches a glob pattern.
     */
    private boolean matchesPattern(String filePath, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return true;
        }

        // Simple glob matching
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");

        return filePath.matches(".*" + regex + ".*");
    }

    /**
     * Combines multiple search results, removing duplicates.
     */
    public List<SearchResult> mergeResults(List<SearchResult> results1, List<SearchResult> results2) {
        List<SearchResult> merged = new ArrayList<>(results1);

        for (SearchResult result : results2) {
            boolean isDuplicate = merged.stream()
                    .anyMatch(r -> r.getChunk().getId().equals(result.getChunk().getId()));
            if (!isDuplicate) {
                merged.add(result);
            }
        }

        // Sort by score descending
        merged.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));

        return merged;
    }
}
