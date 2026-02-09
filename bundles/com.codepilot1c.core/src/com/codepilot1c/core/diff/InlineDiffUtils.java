/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility for computing inline (word/character level) diffs within a single line.
 *
 * <p>Used to highlight exactly which parts of a modified line changed,
 * similar to VS Code/Cursor diff view.</p>
 *
 * @since 1.4.0
 */
public class InlineDiffUtils {

    /** Maximum tokens per line before falling back to simpler algorithm */
    private static final int MAX_TOKENS = 200;

    /**
     * Represents a highlight range within a string.
     */
    public static class HighlightRange {
        private final int start;
        private final int length;

        public HighlightRange(int start, int length) {
            this.start = start;
            this.length = length;
        }

        public int getStart() {
            return start;
        }

        public int getLength() {
            return length;
        }

        public int getEnd() {
            return start + length;
        }
    }

    /**
     * Result of inline diff computation.
     */
    public static class InlineDiffResult {
        private final List<HighlightRange> leftRanges;
        private final List<HighlightRange> rightRanges;

        public InlineDiffResult(List<HighlightRange> leftRanges, List<HighlightRange> rightRanges) {
            this.leftRanges = Collections.unmodifiableList(new ArrayList<>(leftRanges));
            this.rightRanges = Collections.unmodifiableList(new ArrayList<>(rightRanges));
        }

        public List<HighlightRange> getLeftRanges() {
            return leftRanges;
        }

        public List<HighlightRange> getRightRanges() {
            return rightRanges;
        }

        public boolean hasChanges() {
            return !leftRanges.isEmpty() || !rightRanges.isEmpty();
        }
    }

    /**
     * Represents a token with its position in the original string.
     */
    private static class Token {
        final String text;
        final int start;
        final int length;

        Token(String text, int start, int length) {
            this.text = text;
            this.start = start;
            this.length = length;
        }
    }

    /**
     * Computes inline diff between two lines, returning highlight ranges for changed parts.
     *
     * @param left  the original line
     * @param right the modified line
     * @return diff result with highlight ranges for both sides
     */
    public static InlineDiffResult diff(String left, String right) {
        if (left == null) left = ""; //$NON-NLS-1$
        if (right == null) right = ""; //$NON-NLS-1$

        // Fast path: identical strings
        if (left.equals(right)) {
            return new InlineDiffResult(Collections.emptyList(), Collections.emptyList());
        }

        // Fast path: one side empty
        if (left.isEmpty()) {
            return new InlineDiffResult(
                Collections.emptyList(),
                right.isEmpty() ? Collections.emptyList()
                    : Collections.singletonList(new HighlightRange(0, right.length()))
            );
        }
        if (right.isEmpty()) {
            return new InlineDiffResult(
                Collections.singletonList(new HighlightRange(0, left.length())),
                Collections.emptyList()
            );
        }

        // Tokenize both lines
        List<Token> leftTokens = tokenize(left);
        List<Token> rightTokens = tokenize(right);

        // Check if too many tokens - fall back to prefix/suffix
        if (leftTokens.size() > MAX_TOKENS || rightTokens.size() > MAX_TOKENS) {
            return diffByPrefixSuffix(left, right);
        }

        // Compute LCS-based diff on tokens
        return diffByTokens(left, right, leftTokens, rightTokens);
    }

    /**
     * Tokenizes a string into words, whitespace, and punctuation tokens.
     */
    private static List<Token> tokenize(String text) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int len = text.length();

        while (i < len) {
            char c = text.charAt(i);
            int start = i;

            if (isWordChar(c)) {
                // Word token: letters, digits, underscore
                while (i < len && isWordChar(text.charAt(i))) {
                    i++;
                }
            } else if (Character.isWhitespace(c)) {
                // Whitespace token
                while (i < len && Character.isWhitespace(text.charAt(i))) {
                    i++;
                }
            } else {
                // Punctuation/operator token - group contiguous non-word, non-ws
                while (i < len) {
                    char cc = text.charAt(i);
                    if (isWordChar(cc) || Character.isWhitespace(cc)) {
                        break;
                    }
                    i++;
                }
            }

            tokens.add(new Token(text.substring(start, i), start, i - start));
        }

        return tokens;
    }

    /**
     * Checks if character is a word character (letter, digit, or underscore).
     */
    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * Computes diff using LCS on tokens.
     */
    private static InlineDiffResult diffByTokens(String left, String right,
            List<Token> leftTokens, List<Token> rightTokens) {
        int m = leftTokens.size();
        int n = rightTokens.size();

        // Build LCS table
        int[][] lcs = new int[m + 1][n + 1];
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                if (leftTokens.get(i).text.equals(rightTokens.get(j).text)) {
                    lcs[i][j] = 1 + lcs[i + 1][j + 1];
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }

        // Backtrack to find non-matching tokens
        List<HighlightRange> leftRanges = new ArrayList<>();
        List<HighlightRange> rightRanges = new ArrayList<>();

        int i = 0, j = 0;
        while (i < m || j < n) {
            if (i < m && j < n && leftTokens.get(i).text.equals(rightTokens.get(j).text)) {
                // Tokens match
                i++;
                j++;
            } else if (j < n && (i >= m || lcs[i][j + 1] >= lcs[i + 1][j])) {
                // Token added on right
                Token t = rightTokens.get(j);
                rightRanges.add(new HighlightRange(t.start, t.length));
                j++;
            } else if (i < m) {
                // Token removed from left
                Token t = leftTokens.get(i);
                leftRanges.add(new HighlightRange(t.start, t.length));
                i++;
            }
        }

        // Merge adjacent ranges
        return new InlineDiffResult(mergeAdjacentRanges(leftRanges), mergeAdjacentRanges(rightRanges));
    }

    /**
     * Fallback diff using common prefix/suffix detection.
     */
    private static InlineDiffResult diffByPrefixSuffix(String left, String right) {
        int prefixLen = commonPrefixLength(left, right);
        int suffixLen = commonSuffixLength(left, right, prefixLen);

        int leftDiffStart = prefixLen;
        int leftDiffEnd = left.length() - suffixLen;
        int rightDiffStart = prefixLen;
        int rightDiffEnd = right.length() - suffixLen;

        List<HighlightRange> leftRanges = new ArrayList<>();
        List<HighlightRange> rightRanges = new ArrayList<>();

        if (leftDiffEnd > leftDiffStart) {
            leftRanges.add(new HighlightRange(leftDiffStart, leftDiffEnd - leftDiffStart));
        }
        if (rightDiffEnd > rightDiffStart) {
            rightRanges.add(new HighlightRange(rightDiffStart, rightDiffEnd - rightDiffStart));
        }

        return new InlineDiffResult(leftRanges, rightRanges);
    }

    /**
     * Returns length of common prefix between two strings.
     */
    private static int commonPrefixLength(String a, String b) {
        int maxLen = Math.min(a.length(), b.length());
        int i = 0;
        while (i < maxLen && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return i;
    }

    /**
     * Returns length of common suffix between two strings, not overlapping with prefix.
     */
    private static int commonSuffixLength(String a, String b, int prefixLen) {
        int maxLen = Math.min(a.length() - prefixLen, b.length() - prefixLen);
        int i = 0;
        while (i < maxLen && a.charAt(a.length() - 1 - i) == b.charAt(b.length() - 1 - i)) {
            i++;
        }
        return i;
    }

    /**
     * Merges adjacent or overlapping highlight ranges.
     */
    private static List<HighlightRange> mergeAdjacentRanges(List<HighlightRange> ranges) {
        if (ranges.size() <= 1) {
            return ranges;
        }

        List<HighlightRange> merged = new ArrayList<>();
        HighlightRange current = ranges.get(0);

        for (int i = 1; i < ranges.size(); i++) {
            HighlightRange next = ranges.get(i);
            if (next.getStart() <= current.getEnd()) {
                // Merge overlapping/adjacent
                int newEnd = Math.max(current.getEnd(), next.getEnd());
                current = new HighlightRange(current.getStart(), newEnd - current.getStart());
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        return merged;
    }
}
