/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.embedding;

import java.util.Arrays;

/**
 * Represents the result of an embedding operation.
 */
public class EmbeddingResult {

    private final float[] embedding;
    private final int index;
    private final int tokensUsed;

    /**
     * Creates a new embedding result.
     *
     * @param embedding the embedding vector
     * @param index the index in the batch (0 for single embeddings)
     * @param tokensUsed the number of tokens used
     */
    public EmbeddingResult(float[] embedding, int index, int tokensUsed) {
        this.embedding = embedding;
        this.index = index;
        this.tokensUsed = tokensUsed;
    }

    /**
     * Creates a simple embedding result with default values.
     *
     * @param embedding the embedding vector
     * @return a new embedding result
     */
    public static EmbeddingResult of(float[] embedding) {
        return new EmbeddingResult(embedding, 0, 0);
    }

    /**
     * Returns the embedding vector.
     *
     * @return the embedding as a float array
     */
    public float[] getEmbedding() {
        return embedding;
    }

    /**
     * Returns the index in the batch.
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the number of tokens used.
     *
     * @return the tokens used
     */
    public int getTokensUsed() {
        return tokensUsed;
    }

    /**
     * Returns the dimension of the embedding.
     *
     * @return the dimension
     */
    public int getDimension() {
        return embedding != null ? embedding.length : 0;
    }

    @Override
    public String toString() {
        return "EmbeddingResult{" +
                "dimension=" + getDimension() +
                ", index=" + index +
                ", tokensUsed=" + tokensUsed +
                '}';
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(embedding);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EmbeddingResult other = (EmbeddingResult) obj;
        return Arrays.equals(embedding, other.embedding);
    }
}
