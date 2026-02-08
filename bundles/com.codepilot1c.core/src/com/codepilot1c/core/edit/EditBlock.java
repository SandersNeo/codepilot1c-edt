/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.edit;

import java.util.Objects;

/**
 * Represents a single edit operation within a file.
 *
 * <p>Contains the search text to find and the replacement text.</p>
 */
public final class EditBlock {

    private final String searchText;
    private final String replaceText;
    private final int blockIndex;

    /**
     * Creates a new edit block.
     *
     * @param searchText the text to search for
     * @param replaceText the replacement text
     * @param blockIndex the index of this block in the sequence (0-based)
     */
    public EditBlock(String searchText, String replaceText, int blockIndex) {
        this.searchText = Objects.requireNonNull(searchText, "searchText must not be null"); //$NON-NLS-1$
        this.replaceText = Objects.requireNonNull(replaceText, "replaceText must not be null"); //$NON-NLS-1$
        this.blockIndex = blockIndex;
    }

    /**
     * Creates a new edit block with index 0.
     *
     * @param searchText the text to search for
     * @param replaceText the replacement text
     */
    public EditBlock(String searchText, String replaceText) {
        this(searchText, replaceText, 0);
    }

    /**
     * Returns the text to search for.
     *
     * @return the search text
     */
    public String getSearchText() {
        return searchText;
    }

    /**
     * Returns the replacement text.
     *
     * @return the replace text
     */
    public String getReplaceText() {
        return replaceText;
    }

    /**
     * Returns the block index.
     *
     * @return the index (0-based)
     */
    public int getBlockIndex() {
        return blockIndex;
    }

    /**
     * Returns whether this block is a deletion (empty replacement).
     *
     * @return true if deletion
     */
    public boolean isDeletion() {
        return replaceText.isEmpty() && !searchText.isEmpty();
    }

    /**
     * Returns whether this block is an insertion (empty search).
     *
     * @return true if insertion
     */
    public boolean isInsertion() {
        return searchText.isEmpty() && !replaceText.isEmpty();
    }

    /**
     * Returns a summary of this edit block.
     *
     * @return human-readable summary
     */
    public String getSummary() {
        int searchLines = searchText.split("\n", -1).length; //$NON-NLS-1$
        int replaceLines = replaceText.split("\n", -1).length; //$NON-NLS-1$

        if (isDeletion()) {
            return String.format("Удалить %d строк", searchLines); //$NON-NLS-1$
        } else if (isInsertion()) {
            return String.format("Вставить %d строк", replaceLines); //$NON-NLS-1$
        } else {
            int delta = replaceLines - searchLines;
            if (delta > 0) {
                return String.format("Заменить %d строк (+%d)", searchLines, delta); //$NON-NLS-1$
            } else if (delta < 0) {
                return String.format("Заменить %d строк (%d)", searchLines, delta); //$NON-NLS-1$
            } else {
                return String.format("Заменить %d строк", searchLines); //$NON-NLS-1$
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EditBlock editBlock = (EditBlock) o;
        return searchText.equals(editBlock.searchText) && replaceText.equals(editBlock.replaceText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(searchText, replaceText);
    }

    @Override
    public String toString() {
        return String.format("EditBlock[search=%d chars, replace=%d chars, idx=%d]", //$NON-NLS-1$
                searchText.length(), replaceText.length(), blockIndex);
    }
}
