/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.rag.indexer;

import java.util.List;

import org.eclipse.core.resources.IFile;

import com.codepilot1c.core.index.CodeChunk;

/**
 * Interface for code chunkers that split source files into semantically meaningful chunks.
 *
 * <p>Code chunkers are language-specific and understand the structure of the code
 * to create chunks at natural boundaries (methods, classes, etc.).</p>
 */
public interface ICodeChunker {

    /**
     * Returns the unique identifier for this chunker.
     *
     * @return the chunker ID
     */
    String getId();

    /**
     * Returns the display name for this chunker.
     *
     * @return the display name
     */
    String getDisplayName();

    /**
     * Returns the language this chunker handles.
     *
     * @return the language identifier (e.g., "bsl", "sdbl")
     */
    String getLanguage();

    /**
     * Returns the priority of this chunker (higher = preferred).
     *
     * @return the priority
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Checks if this chunker can handle the given file.
     *
     * @param file the file to check
     * @return true if this chunker can handle the file
     */
    boolean canHandle(IFile file);

    /**
     * Chunks the content of a file into semantically meaningful pieces.
     *
     * @param file the file to chunk
     * @param content the file content
     * @param projectName the project name for metadata
     * @return list of code chunks
     * @throws ChunkingException if chunking fails
     */
    List<CodeChunk> chunk(IFile file, String content, String projectName) throws ChunkingException;

    /**
     * Returns the maximum chunk size in tokens.
     *
     * @return the max chunk size
     */
    default int getMaxChunkTokens() {
        return 512;
    }

    /**
     * Returns the chunk overlap in tokens (for sliding window approach).
     *
     * @return the overlap size
     */
    default int getChunkOverlap() {
        return 50;
    }

    /**
     * Exception thrown when code chunking fails.
     */
    class ChunkingException extends Exception {
        private static final long serialVersionUID = 1L;

        public ChunkingException(String message) {
            super(message);
        }

        public ChunkingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
