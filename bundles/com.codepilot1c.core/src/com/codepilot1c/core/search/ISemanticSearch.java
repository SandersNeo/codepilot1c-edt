/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.search;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.codepilot1c.core.index.CodeChunk;

/**
 * Interface for semantic search across the codebase.
 *
 * <p>Provides vector-based similarity search using embeddings.</p>
 */
public interface ISemanticSearch {

    /**
     * Performs semantic search for the given query.
     *
     * @param query the search query (natural language or code)
     * @param topK the maximum number of results to return
     * @return list of matching code chunks ordered by relevance
     */
    List<SearchResult> search(String query, int topK);

    /**
     * Performs asynchronous semantic search.
     *
     * @param query the search query
     * @param topK the maximum number of results
     * @return future with search results
     */
    CompletableFuture<List<SearchResult>> searchAsync(String query, int topK);

    /**
     * Performs semantic search with options.
     *
     * @param query the search query
     * @param options search options
     * @return list of matching code chunks
     */
    List<SearchResult> search(String query, SearchOptions options);

    /**
     * Checks if the search service is ready (index exists and is loaded).
     *
     * @return true if ready for search
     */
    boolean isReady();

    /**
     * Search result containing a code chunk and its relevance score.
     */
    public static class SearchResult {
        private final CodeChunk chunk;
        private final float score;
        private final String highlightedContent;

        public SearchResult(CodeChunk chunk, float score) {
            this(chunk, score, null);
        }

        public SearchResult(CodeChunk chunk, float score, String highlightedContent) {
            this.chunk = chunk;
            this.score = score;
            this.highlightedContent = highlightedContent;
        }

        public CodeChunk getChunk() {
            return chunk;
        }

        public float getScore() {
            return score;
        }

        public String getHighlightedContent() {
            return highlightedContent;
        }

        @Override
        public String toString() {
            return String.format("SearchResult[score=%.3f, chunk=%s]", score, chunk.getId());
        }
    }

    /**
     * Options for customizing search behavior.
     */
    public static class SearchOptions {
        private int topK = 10;
        private float minScore = 0.0f;
        private String filePattern;
        private String entityKind;
        private boolean expandContext = true;
        private int contextLines = 5;

        public static SearchOptions defaults() {
            return new SearchOptions();
        }

        public SearchOptions topK(int topK) {
            this.topK = topK;
            return this;
        }

        public SearchOptions minScore(float minScore) {
            this.minScore = minScore;
            return this;
        }

        public SearchOptions filePattern(String pattern) {
            this.filePattern = pattern;
            return this;
        }

        public SearchOptions entityKind(String kind) {
            this.entityKind = kind;
            return this;
        }

        public SearchOptions expandContext(boolean expand) {
            this.expandContext = expand;
            return this;
        }

        public SearchOptions contextLines(int lines) {
            this.contextLines = lines;
            return this;
        }

        public int getTopK() {
            return topK;
        }

        public float getMinScore() {
            return minScore;
        }

        public String getFilePattern() {
            return filePattern;
        }

        public String getEntityKind() {
            return entityKind;
        }

        public boolean isExpandContext() {
            return expandContext;
        }

        public int getContextLines() {
            return contextLines;
        }
    }
}
