/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.edit;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Result of a fuzzy match operation.
 *
 * <p>Contains either a successful match location or diagnostic feedback
 * for the LLM to retry with corrected input.</p>
 */
public final class MatchResult {

    private final boolean success;
    private final MatchLocation location;
    private final MatchStrategy strategy;
    private final double similarity;
    private final String errorMessage;
    private final List<SimilarMatch> candidates;

    private MatchResult(boolean success, MatchLocation location, MatchStrategy strategy,
                        double similarity, String errorMessage, List<SimilarMatch> candidates) {
        this.success = success;
        this.location = location;
        this.strategy = strategy;
        this.similarity = similarity;
        this.errorMessage = errorMessage;
        this.candidates = candidates != null ? candidates : Collections.emptyList();
    }

    /**
     * Creates a successful match result.
     *
     * @param location the match location
     * @param strategy the strategy that succeeded
     * @return the result
     */
    public static MatchResult success(MatchLocation location, MatchStrategy strategy) {
        return new MatchResult(true, location, strategy, 1.0, null, null);
    }

    /**
     * Creates a successful match result with similarity score.
     *
     * @param location the match location
     * @param strategy the strategy that succeeded
     * @param similarity the similarity score (0.0 - 1.0)
     * @return the result
     */
    public static MatchResult success(MatchLocation location, MatchStrategy strategy, double similarity) {
        return new MatchResult(true, location, strategy, similarity, null, null);
    }

    /**
     * Creates a failure result with error message.
     *
     * @param errorMessage the error message
     * @return the result
     */
    public static MatchResult failure(String errorMessage) {
        return new MatchResult(false, null, null, 0.0, errorMessage, null);
    }

    /**
     * Creates a failure result with candidates for feedback.
     *
     * @param errorMessage the error message
     * @param candidates list of similar matches found
     * @return the result
     */
    public static MatchResult failure(String errorMessage, List<SimilarMatch> candidates) {
        return new MatchResult(false, null, null, 0.0, errorMessage, candidates);
    }

    /**
     * Creates a failure result indicating ambiguous match.
     *
     * @param candidates multiple equally good candidates
     * @return the result
     */
    public static MatchResult ambiguous(List<SimilarMatch> candidates) {
        String message = String.format(
                "Найдено %d похожих совпадений. Добавьте больше контекста для уникальной идентификации.", //$NON-NLS-1$
                candidates.size());
        return new MatchResult(false, null, null, 0.0, message, candidates);
    }

    /**
     * Returns whether the match was successful.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the match location if successful.
     *
     * @return the location
     */
    public Optional<MatchLocation> getLocation() {
        return Optional.ofNullable(location);
    }

    /**
     * Returns the strategy that produced this match.
     *
     * @return the strategy, or null if failed
     */
    public MatchStrategy getStrategy() {
        return strategy;
    }

    /**
     * Returns the similarity score (0.0 - 1.0).
     *
     * @return the similarity score
     */
    public double getSimilarity() {
        return similarity;
    }

    /**
     * Returns the error message if failed.
     *
     * @return the error message, or null if successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns similar candidate matches for feedback.
     *
     * @return list of candidates, empty if none found
     */
    public List<SimilarMatch> getCandidates() {
        return candidates;
    }

    /**
     * Generates actionable feedback for the LLM to retry.
     *
     * @return feedback string with suggestions
     */
    public String generateFeedback() {
        if (success) {
            return "Совпадение найдено."; //$NON-NLS-1$
        }

        StringBuilder sb = new StringBuilder();
        sb.append("ОШИБКА: ").append(errorMessage).append("\n\n"); //$NON-NLS-1$ //$NON-NLS-2$

        if (!candidates.isEmpty()) {
            sb.append("Найдены похожие фрагменты:\n"); //$NON-NLS-1$
            int shown = Math.min(candidates.size(), 3);
            for (int i = 0; i < shown; i++) {
                SimilarMatch candidate = candidates.get(i);
                sb.append(String.format("\n--- Кандидат %d (строки %d-%d, сходство: %.0f%%) ---\n", //$NON-NLS-1$
                        i + 1, candidate.startLine(), candidate.endLine(),
                        candidate.similarity() * 100));
                // Show first few lines
                String[] lines = candidate.text().split("\n", 6); //$NON-NLS-1$
                for (int j = 0; j < Math.min(lines.length, 5); j++) {
                    sb.append(lines[j]).append("\n"); //$NON-NLS-1$
                }
                if (lines.length > 5) {
                    sb.append("...\n"); //$NON-NLS-1$
                }
            }

            sb.append("\nПОДСКАЗКА: Используйте ТОЧНЫЙ текст из одного из кандидатов в SEARCH блоке."); //$NON-NLS-1$
        } else {
            sb.append("ПОДСКАЗКА: Проверьте, что искомый текст существует в файле. "); //$NON-NLS-1$
            sb.append("Возможно, файл был изменён или текст отличается пробелами/отступами."); //$NON-NLS-1$
        }

        return sb.toString();
    }

    /**
     * Represents a similar match candidate.
     *
     * @param text the matched text
     * @param startLine start line number
     * @param endLine end line number
     * @param similarity similarity score (0.0 - 1.0)
     */
    public record SimilarMatch(String text, int startLine, int endLine, double similarity) {
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("MatchResult[success, %s, similarity=%.2f]", strategy, similarity); //$NON-NLS-1$
        } else {
            return String.format("MatchResult[failed: %s, candidates=%d]", errorMessage, candidates.size()); //$NON-NLS-1$
        }
    }
}
