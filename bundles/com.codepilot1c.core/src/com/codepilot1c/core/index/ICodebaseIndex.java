/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.index;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for the codebase index used in RAG (Retrieval-Augmented Generation).
 *
 * <p>The index stores code chunks with their vector embeddings and supports
 * semantic search using KNN (K-Nearest Neighbors) queries.</p>
 */
public interface ICodebaseIndex {

    /**
     * Initializes the index.
     *
     * @throws IndexException if initialization fails
     */
    void initialize() throws IndexException;

    /**
     * Returns whether the index is initialized and ready.
     *
     * @return true if ready
     */
    boolean isReady();

    /**
     * Inserts or updates a chunk in the index.
     *
     * @param chunk the code chunk
     * @param embedding the embedding vector
     */
    void upsertChunk(CodeChunk chunk, float[] embedding);

    /**
     * Inserts or updates multiple chunks in the index.
     *
     * @param chunks the code chunks
     * @param embeddings the embedding vectors (same order as chunks)
     */
    void upsertChunks(List<CodeChunk> chunks, List<float[]> embeddings);

    /**
     * Deletes all chunks for a specific file.
     *
     * @param filePath the file path
     * @return the number of deleted chunks
     */
    int deleteByFile(String filePath);

    /**
     * Deletes all chunks for a specific project.
     *
     * @param projectName the project name
     * @return the number of deleted chunks
     */
    int deleteByProject(String projectName);

    /**
     * Deletes a specific chunk by ID.
     *
     * @param chunkId the chunk ID
     * @return true if deleted
     */
    boolean deleteChunk(String chunkId);

    /**
     * Searches for similar code chunks using KNN.
     *
     * @param queryEmbedding the query embedding vector
     * @param k the maximum number of results
     * @return list of search hits ordered by similarity
     */
    List<CodeSearchHit> searchKnn(float[] queryEmbedding, int k);

    /**
     * Searches for similar code chunks with a project filter.
     *
     * @param queryEmbedding the query embedding vector
     * @param k the maximum number of results
     * @param projectName optional project name filter (null for all)
     * @return list of search hits ordered by similarity
     */
    List<CodeSearchHit> searchKnn(float[] queryEmbedding, int k, String projectName);

    /**
     * Searches for similar code chunks asynchronously.
     *
     * @param queryEmbedding the query embedding vector
     * @param k the maximum number of results
     * @return future with search hits
     */
    default CompletableFuture<List<CodeSearchHit>> searchKnnAsync(float[] queryEmbedding, int k) {
        return CompletableFuture.supplyAsync(() -> searchKnn(queryEmbedding, k));
    }

    /**
     * Gets a specific chunk by ID.
     *
     * @param chunkId the chunk ID
     * @return the chunk, or null if not found
     */
    CodeChunk getChunk(String chunkId);

    /**
     * Gets all chunks for a specific file.
     *
     * @param filePath the file path
     * @return list of chunks
     */
    List<CodeChunk> getChunksByFile(String filePath);

    /**
     * Searches for chunks by keyword in content.
     *
     * @param keyword the keyword to search for
     * @param maxResults maximum number of results
     * @return list of matching chunks
     */
    List<CodeChunk> searchByKeyword(String keyword, int maxResults);

    /**
     * Returns index statistics.
     *
     * @return the index stats
     */
    IndexStats getStats();

    /**
     * Commits pending changes to persistent storage.
     */
    void commit();

    /**
     * Optimizes the index for better search performance.
     */
    void optimize();

    /**
     * Clears all data from the index.
     */
    void clear();

    /**
     * Closes the index and releases resources.
     */
    void close();

    /**
     * Exception thrown when index operations fail.
     */
    class IndexException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public IndexException(String message) {
            super(message);
        }

        public IndexException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
