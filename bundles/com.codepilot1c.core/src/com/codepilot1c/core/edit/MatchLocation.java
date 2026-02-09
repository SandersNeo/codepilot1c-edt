/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.edit;

/**
 * Location of a match in document content.
 *
 * <p>Provides both character offset and line-based positions.</p>
 */
public final class MatchLocation {

    private final int startOffset;
    private final int endOffset;
    private final int startLine;
    private final int endLine;
    private final String matchedText;

    /**
     * Creates a new match location.
     *
     * @param startOffset character offset of match start
     * @param endOffset character offset of match end (exclusive)
     * @param startLine line number of match start (1-based)
     * @param endLine line number of match end (1-based)
     * @param matchedText the actual matched text
     */
    public MatchLocation(int startOffset, int endOffset, int startLine, int endLine, String matchedText) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.startLine = startLine;
        this.endLine = endLine;
        this.matchedText = matchedText;
    }

    /**
     * Returns the character offset of match start.
     *
     * @return start offset (0-based)
     */
    public int getStartOffset() {
        return startOffset;
    }

    /**
     * Returns the character offset of match end.
     *
     * @return end offset (exclusive, 0-based)
     */
    public int getEndOffset() {
        return endOffset;
    }

    /**
     * Returns the length of the matched region.
     *
     * @return length in characters
     */
    public int getLength() {
        return endOffset - startOffset;
    }

    /**
     * Returns the line number where match starts.
     *
     * @return start line (1-based)
     */
    public int getStartLine() {
        return startLine;
    }

    /**
     * Returns the line number where match ends.
     *
     * @return end line (1-based)
     */
    public int getEndLine() {
        return endLine;
    }

    /**
     * Returns the actual matched text from the document.
     *
     * @return the matched text
     */
    public String getMatchedText() {
        return matchedText;
    }

    @Override
    public String toString() {
        return String.format("MatchLocation[offset=%d-%d, lines=%d-%d, len=%d]", //$NON-NLS-1$
                startOffset, endOffset, startLine, endLine, getLength());
    }
}
