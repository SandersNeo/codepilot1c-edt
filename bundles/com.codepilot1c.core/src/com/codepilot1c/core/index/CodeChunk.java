/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.index;

import java.util.Objects;

/**
 * Represents a chunk of code for indexing.
 *
 * <p>A chunk is a semantically meaningful unit of code, typically
 * a function, procedure, or related block of code.</p>
 */
public class CodeChunk {

    private final String id;
    private final String filePath;
    private final String projectName;
    private final String content;
    private final String symbolName;
    private final ChunkType chunkType;
    private final int startLine;
    private final int endLine;
    private final String metadataPath;
    private final String language;

    /**
     * Types of code chunks.
     */
    public enum ChunkType {
        /** A procedure or function */
        METHOD,
        /** A class or type definition */
        CLASS,
        /** Module-level variables */
        VARIABLES,
        /** A form module */
        FORM,
        /** A query definition */
        QUERY,
        /** Generic code block */
        BLOCK,
        /** Module summary (overview of entire module) */
        MODULE,
        /** Unknown or unclassified */
        UNKNOWN
    }

    private CodeChunk(Builder builder) {
        this.id = builder.id;
        this.filePath = builder.filePath;
        this.projectName = builder.projectName;
        this.content = builder.content;
        this.symbolName = builder.symbolName;
        this.chunkType = builder.chunkType;
        this.startLine = builder.startLine;
        this.endLine = builder.endLine;
        this.metadataPath = builder.metadataPath;
        this.language = builder.language;
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the unique identifier for this chunk.
     *
     * @return the chunk ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the file path containing this chunk.
     *
     * @return the file path
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Returns the project name.
     *
     * @return the project name
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Returns the code content.
     *
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the symbol name (e.g., function name).
     *
     * @return the symbol name
     */
    public String getSymbolName() {
        return symbolName;
    }

    /**
     * Returns the chunk type.
     *
     * @return the chunk type
     */
    public ChunkType getChunkType() {
        return chunkType;
    }

    /**
     * Returns the starting line number (0-based).
     *
     * @return the start line
     */
    public int getStartLine() {
        return startLine;
    }

    /**
     * Returns the ending line number (0-based).
     *
     * @return the end line
     */
    public int getEndLine() {
        return endLine;
    }

    /**
     * Returns the metadata path (e.g., "Catalogs.Products.Forms.ItemForm").
     *
     * @return the metadata path
     */
    public String getMetadataPath() {
        return metadataPath;
    }

    /**
     * Returns the language (e.g., "bsl", "sdbl").
     *
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Returns the number of lines in this chunk.
     *
     * @return the line count
     */
    public int getLineCount() {
        return endLine - startLine + 1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CodeChunk other = (CodeChunk) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public String toString() {
        return "CodeChunk{" +
                "id='" + id + '\'' +
                ", symbolName='" + symbolName + '\'' +
                ", chunkType=" + chunkType +
                ", lines=" + startLine + "-" + endLine +
                '}';
    }

    /**
     * Builder for CodeChunk.
     */
    public static class Builder {
        private String id;
        private String filePath = ""; //$NON-NLS-1$
        private String projectName = ""; //$NON-NLS-1$
        private String content = ""; //$NON-NLS-1$
        private String symbolName = ""; //$NON-NLS-1$
        private ChunkType chunkType = ChunkType.UNKNOWN;
        private int startLine = 0;
        private int endLine = 0;
        private String metadataPath = ""; //$NON-NLS-1$
        private String language = "bsl"; //$NON-NLS-1$

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder symbolName(String symbolName) {
            this.symbolName = symbolName;
            return this;
        }

        public Builder chunkType(ChunkType chunkType) {
            this.chunkType = chunkType;
            return this;
        }

        public Builder startLine(int startLine) {
            this.startLine = startLine;
            return this;
        }

        public Builder endLine(int endLine) {
            this.endLine = endLine;
            return this;
        }

        public Builder metadataPath(String metadataPath) {
            this.metadataPath = metadataPath;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        /**
         * Builds the CodeChunk instance.
         *
         * @return the built instance
         */
        public CodeChunk build() {
            if (id == null || id.isEmpty()) {
                // Generate ID from file path and line range
                id = filePath + ":" + startLine + "-" + endLine; //$NON-NLS-1$ //$NON-NLS-2$
            }
            return new CodeChunk(this);
        }
    }
}
