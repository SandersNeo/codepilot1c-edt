/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.embedding;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for embedding providers that convert text into vector representations.
 *
 * <p>Embeddings are used for semantic search in the codebase index.</p>
 */
public interface IEmbeddingProvider {

    /**
     * Returns the unique identifier for this provider.
     *
     * @return the provider ID
     */
    String getId();

    /**
     * Returns a human-readable display name.
     *
     * @return the display name
     */
    String getDisplayName();

    /**
     * Returns whether this provider is properly configured.
     *
     * @return true if configured and ready to use
     */
    boolean isConfigured();

    /**
     * Returns the dimension of embedding vectors produced by this provider.
     *
     * @return the embedding dimension
     */
    int getDimensions();

    /**
     * Embeds a single text string.
     *
     * @param text the text to embed
     * @return a future containing the embedding result
     */
    CompletableFuture<EmbeddingResult> embed(String text);

    /**
     * Embeds multiple texts in a batch for efficiency.
     *
     * @param texts the texts to embed
     * @return a future containing a list of embedding results
     */
    CompletableFuture<List<EmbeddingResult>> embedBatch(List<String> texts);

    /**
     * Returns the maximum number of texts that can be embedded in a single batch.
     *
     * @return the maximum batch size
     */
    default int getMaxBatchSize() {
        return 100;
    }

    /**
     * Returns the maximum number of tokens per text.
     *
     * @return the maximum tokens
     */
    default int getMaxTokens() {
        return 8191;
    }

    /**
     * Cancels any ongoing operations.
     */
    void cancel();

    /**
     * Disposes resources held by this provider.
     */
    void dispose();
}
