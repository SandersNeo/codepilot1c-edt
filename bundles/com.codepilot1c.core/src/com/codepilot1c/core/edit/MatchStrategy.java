/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.edit;

/**
 * Matching strategy for fuzzy text matching.
 *
 * <p>Strategies are tried in order from most strict to most lenient.</p>
 */
public enum MatchStrategy {

    /**
     * Exact byte-for-byte match.
     */
    EXACT("Точное совпадение"), //$NON-NLS-1$

    /**
     * Match ignoring trailing whitespace and line ending differences.
     */
    NORMALIZE_WHITESPACE("Нормализация пробелов"), //$NON-NLS-1$

    /**
     * Match ignoring leading indentation differences.
     */
    NORMALIZE_INDENTATION("Нормализация отступов"), //$NON-NLS-1$

    /**
     * Similarity-based fuzzy match using longest common subsequence.
     */
    SIMILARITY("Поиск по сходству"); //$NON-NLS-1$

    private final String displayName;

    MatchStrategy(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the display name for UI.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
