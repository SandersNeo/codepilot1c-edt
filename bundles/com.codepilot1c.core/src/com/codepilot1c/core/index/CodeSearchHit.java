/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.index;

/**
 * Represents a search result from the codebase index.
 */
public class CodeSearchHit {

    private final String chunkId;
    private final String filePath;
    private final String projectName;
    private final String content;
    private final String symbolName;
    private final CodeChunk.ChunkType chunkType;
    private final int startLine;
    private final int endLine;
    private final String metadataPath;
    private final float score;

    /**
     * Creates a new search hit.
     *
     * @param chunkId the chunk ID
     * @param filePath the file path
     * @param projectName the project name
     * @param content the code content
     * @param symbolName the symbol name
     * @param chunkType the chunk type
     * @param startLine the start line
     * @param endLine the end line
     * @param metadataPath the metadata path
     * @param score the similarity score
     */
    public CodeSearchHit(String chunkId, String filePath, String projectName,
                         String content, String symbolName, CodeChunk.ChunkType chunkType,
                         int startLine, int endLine, String metadataPath, float score) {
        this.chunkId = chunkId;
        this.filePath = filePath;
        this.projectName = projectName;
        this.content = content;
        this.symbolName = symbolName;
        this.chunkType = chunkType;
        this.startLine = startLine;
        this.endLine = endLine;
        this.metadataPath = metadataPath;
        this.score = score;
    }

    /**
     * Creates a search hit from a CodeChunk.
     *
     * @param chunk the code chunk
     * @param score the similarity score
     * @return a new search hit
     */
    public static CodeSearchHit fromChunk(CodeChunk chunk, float score) {
        return new CodeSearchHit(
                chunk.getId(),
                chunk.getFilePath(),
                chunk.getProjectName(),
                chunk.getContent(),
                chunk.getSymbolName(),
                chunk.getChunkType(),
                chunk.getStartLine(),
                chunk.getEndLine(),
                chunk.getMetadataPath(),
                score
        );
    }

    public String getChunkId() {
        return chunkId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getContent() {
        return content;
    }

    public String getSymbolName() {
        return symbolName;
    }

    public CodeChunk.ChunkType getChunkType() {
        return chunkType;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public String getMetadataPath() {
        return metadataPath;
    }

    /**
     * Returns the similarity score (0.0 to 1.0, higher is more similar).
     *
     * @return the score
     */
    public float getScore() {
        return score;
    }

    /**
     * Converts this search hit to a CodeChunk.
     *
     * @return a CodeChunk with the same data
     */
    public CodeChunk toChunk() {
        return CodeChunk.builder()
                .id(chunkId)
                .filePath(filePath)
                .projectName(projectName)
                .content(content)
                .symbolName(symbolName)
                .chunkType(chunkType)
                .startLine(startLine)
                .endLine(endLine)
                .metadataPath(metadataPath)
                .build();
    }

    /**
     * Alias for toChunk() for convenience.
     *
     * @return a CodeChunk with the same data
     */
    public CodeChunk getChunk() {
        return toChunk();
    }

    /**
     * Returns the entity kind (chunk type name).
     *
     * @return the entity kind
     */
    public String getEntityKind() {
        return chunkType != null ? chunkType.name() : ""; //$NON-NLS-1$
    }

    @Override
    public String toString() {
        return "CodeSearchHit{" +
                "symbolName='" + symbolName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", score=" + score +
                '}';
    }
}
