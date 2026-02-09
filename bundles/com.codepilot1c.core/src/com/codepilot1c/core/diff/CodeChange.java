/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.diff;

/**
 * Represents a code change to be applied to a document.
 *
 * <p>A code change can be:</p>
 * <ul>
 *   <li>An insertion at a specific offset</li>
 *   <li>A replacement of a range of text</li>
 *   <li>A deletion of a range of text</li>
 * </ul>
 *
 * <p>Based on patterns from 1C:Workmate for code application.</p>
 */
public class CodeChange {

    /**
     * Type of code change.
     */
    public enum ChangeType {
        /** Insert new text at offset */
        INSERT,
        /** Replace existing text with new text */
        REPLACE,
        /** Delete text in range */
        DELETE
    }

    private final ChangeType type;
    private final int offset;
    private final int length;
    private final String oldText;
    private final String newText;
    private final String description;

    private CodeChange(Builder builder) {
        this.type = builder.type;
        this.offset = builder.offset;
        this.length = builder.length;
        this.oldText = builder.oldText;
        this.newText = builder.newText;
        this.description = builder.description;
    }

    /**
     * Returns the change type.
     *
     * @return the type
     */
    public ChangeType getType() {
        return type;
    }

    /**
     * Returns the document offset where the change starts.
     *
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Returns the length of text to be replaced/deleted.
     *
     * @return the length (0 for insertions)
     */
    public int getLength() {
        return length;
    }

    /**
     * Returns the original text being replaced/deleted.
     *
     * @return the old text, or null for insertions
     */
    public String getOldText() {
        return oldText;
    }

    /**
     * Returns the new text to insert/replace with.
     *
     * @return the new text, or null for deletions
     */
    public String getNewText() {
        return newText;
    }

    /**
     * Returns an optional description of the change.
     *
     * @return the description, or null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the end offset of the affected range.
     *
     * @return offset + length
     */
    public int getEndOffset() {
        return offset + length;
    }

    /**
     * Returns the net change in document length after applying this change.
     *
     * @return positive if document grows, negative if shrinks
     */
    public int getLengthDelta() {
        int oldLen = oldText != null ? oldText.length() : length;
        int newLen = newText != null ? newText.length() : 0;
        return newLen - oldLen;
    }

    /**
     * Creates a builder for an insertion change.
     *
     * @param offset the insertion offset
     * @param text the text to insert
     * @return a builder
     */
    public static Builder insert(int offset, String text) {
        return new Builder()
                .type(ChangeType.INSERT)
                .offset(offset)
                .length(0)
                .newText(text);
    }

    /**
     * Creates a builder for a replacement change.
     *
     * @param offset the start offset
     * @param length the length to replace
     * @param oldText the original text
     * @param newText the replacement text
     * @return a builder
     */
    public static Builder replace(int offset, int length, String oldText, String newText) {
        return new Builder()
                .type(ChangeType.REPLACE)
                .offset(offset)
                .length(length)
                .oldText(oldText)
                .newText(newText);
    }

    /**
     * Creates a builder for a deletion change.
     *
     * @param offset the start offset
     * @param length the length to delete
     * @param oldText the text being deleted
     * @return a builder
     */
    public static Builder delete(int offset, int length, String oldText) {
        return new Builder()
                .type(ChangeType.DELETE)
                .offset(offset)
                .length(length)
                .oldText(oldText)
                .newText(null);
    }

    /**
     * Creates a new builder.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("CodeChange[%s at %d, len=%d, delta=%d]", //$NON-NLS-1$
                type, offset, length, getLengthDelta());
    }

    /**
     * Builder for CodeChange.
     */
    public static class Builder {
        private ChangeType type = ChangeType.REPLACE;
        private int offset;
        private int length;
        private String oldText;
        private String newText;
        private String description;

        public Builder type(ChangeType type) {
            this.type = type;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public Builder length(int length) {
            this.length = length;
            return this;
        }

        public Builder oldText(String oldText) {
            this.oldText = oldText;
            return this;
        }

        public Builder newText(String newText) {
            this.newText = newText;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public CodeChange build() {
            return new CodeChange(this);
        }
    }
}
