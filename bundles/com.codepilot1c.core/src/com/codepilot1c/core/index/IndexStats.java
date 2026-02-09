/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.index;

import java.time.Instant;

/**
 * Statistics about the codebase index.
 */
public class IndexStats {

    private final int totalChunks;
    private final int totalFiles;
    private final int totalProjects;
    private final long indexSizeBytes;
    private final Instant lastUpdated;
    private final int embeddingDimension;
    private final String embeddingModel;

    /**
     * Creates new index statistics.
     *
     * @param totalChunks the total number of indexed chunks
     * @param totalFiles the total number of indexed files
     * @param totalProjects the total number of indexed projects
     * @param indexSizeBytes the index size in bytes
     * @param lastUpdated the last update timestamp
     * @param embeddingDimension the embedding vector dimension
     * @param embeddingModel the embedding model used
     */
    public IndexStats(int totalChunks, int totalFiles, int totalProjects,
                      long indexSizeBytes, Instant lastUpdated,
                      int embeddingDimension, String embeddingModel) {
        this.totalChunks = totalChunks;
        this.totalFiles = totalFiles;
        this.totalProjects = totalProjects;
        this.indexSizeBytes = indexSizeBytes;
        this.lastUpdated = lastUpdated;
        this.embeddingDimension = embeddingDimension;
        this.embeddingModel = embeddingModel;
    }

    /**
     * Creates empty statistics for an uninitialized index.
     *
     * @return empty stats
     */
    public static IndexStats empty() {
        return new IndexStats(0, 0, 0, 0, null, 0, null);
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public int getTotalProjects() {
        return totalProjects;
    }

    public long getIndexSizeBytes() {
        return indexSizeBytes;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    /**
     * Returns whether the index is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return totalChunks == 0;
    }

    /**
     * Returns a human-readable size string.
     *
     * @return the size as a string
     */
    public String getIndexSizeFormatted() {
        if (indexSizeBytes < 1024) {
            return indexSizeBytes + " B"; //$NON-NLS-1$
        } else if (indexSizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", indexSizeBytes / 1024.0); //$NON-NLS-1$
        } else if (indexSizeBytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", indexSizeBytes / (1024.0 * 1024)); //$NON-NLS-1$
        } else {
            return String.format("%.1f GB", indexSizeBytes / (1024.0 * 1024 * 1024)); //$NON-NLS-1$
        }
    }

    @Override
    public String toString() {
        return "IndexStats{" +
                "totalChunks=" + totalChunks +
                ", totalFiles=" + totalFiles +
                ", totalProjects=" + totalProjects +
                ", indexSize=" + getIndexSizeFormatted() +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
