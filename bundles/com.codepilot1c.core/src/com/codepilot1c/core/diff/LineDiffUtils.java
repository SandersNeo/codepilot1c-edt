/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility for computing line-level diffs using Myers algorithm.
 *
 * <p>Provides functionality for:
 * <ul>
 *   <li>Computing line-by-line differences</li>
 *   <li>Building hunks with context lines</li>
 *   <li>Generating aligned side-by-side rows</li>
 * </ul>
 *
 * @since 1.3.0
 */
public class LineDiffUtils {

    /** Default number of context lines around changes */
    public static final int DEFAULT_CONTEXT_LINES = 3;

    /**
     * Type of diff line.
     */
    public enum LineType {
        /** Line exists only in original (deleted) */
        DELETED,
        /** Line exists only in modified (added) */
        ADDED,
        /** Line is unchanged */
        UNCHANGED
    }

    /**
     * Represents a single diff line.
     */
    public static class DiffLine {
        private final LineType type;
        private final String content;
        private final int oldLineNumber; // -1 if not applicable
        private final int newLineNumber; // -1 if not applicable

        public DiffLine(LineType type, String content, int oldLineNumber, int newLineNumber) {
            this.type = type;
            this.content = content;
            this.oldLineNumber = oldLineNumber;
            this.newLineNumber = newLineNumber;
        }

        public LineType getType() {
            return type;
        }

        public String getContent() {
            return content;
        }

        public int getOldLineNumber() {
            return oldLineNumber;
        }

        public int getNewLineNumber() {
            return newLineNumber;
        }

        public boolean isChanged() {
            return type != LineType.UNCHANGED;
        }
    }

    /**
     * Represents a hunk of changes with surrounding context.
     */
    public static class DiffHunk {
        private final int oldStart;
        private final int oldCount;
        private final int newStart;
        private final int newCount;
        private final List<DiffLine> lines;

        public DiffHunk(int oldStart, int oldCount, int newStart, int newCount, List<DiffLine> lines) {
            this.oldStart = oldStart;
            this.oldCount = oldCount;
            this.newStart = newStart;
            this.newCount = newCount;
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
        }

        public int getOldStart() {
            return oldStart;
        }

        public int getOldCount() {
            return oldCount;
        }

        public int getNewStart() {
            return newStart;
        }

        public int getNewCount() {
            return newCount;
        }

        public List<DiffLine> getLines() {
            return lines;
        }

        /**
         * Returns header in unified diff format.
         */
        public String getHeader() {
            return String.format("@@ -%d,%d +%d,%d @@", oldStart, oldCount, newStart, newCount); //$NON-NLS-1$
        }
    }

    /**
     * Represents an aligned row for side-by-side display.
     */
    public static class AlignedRow {
        private final String leftContent;
        private final String rightContent;
        private final int leftLineNumber;  // -1 if empty
        private final int rightLineNumber; // -1 if empty
        private final RowType type;

        public enum RowType {
            UNCHANGED,
            DELETED,     // Left only
            ADDED,       // Right only
            MODIFIED     // Both sides but different
        }

        public AlignedRow(String leftContent, String rightContent,
                          int leftLineNumber, int rightLineNumber, RowType type) {
            this.leftContent = leftContent;
            this.rightContent = rightContent;
            this.leftLineNumber = leftLineNumber;
            this.rightLineNumber = rightLineNumber;
            this.type = type;
        }

        public String getLeftContent() {
            return leftContent;
        }

        public String getRightContent() {
            return rightContent;
        }

        public int getLeftLineNumber() {
            return leftLineNumber;
        }

        public int getRightLineNumber() {
            return rightLineNumber;
        }

        public RowType getType() {
            return type;
        }

        public boolean isChanged() {
            return type != RowType.UNCHANGED;
        }

        public boolean hasLeft() {
            return leftLineNumber >= 0;
        }

        public boolean hasRight() {
            return rightLineNumber >= 0;
        }
    }

    /**
     * Result of diff computation.
     */
    public static class DiffResult {
        private final List<DiffLine> allLines;
        private final List<DiffHunk> hunks;
        private final List<AlignedRow> alignedRows;
        private final int addedCount;
        private final int deletedCount;

        public DiffResult(List<DiffLine> allLines, List<DiffHunk> hunks,
                          List<AlignedRow> alignedRows, int addedCount, int deletedCount) {
            this.allLines = Collections.unmodifiableList(new ArrayList<>(allLines));
            this.hunks = Collections.unmodifiableList(new ArrayList<>(hunks));
            this.alignedRows = Collections.unmodifiableList(new ArrayList<>(alignedRows));
            this.addedCount = addedCount;
            this.deletedCount = deletedCount;
        }

        public List<DiffLine> getAllLines() {
            return allLines;
        }

        public List<DiffHunk> getHunks() {
            return hunks;
        }

        public List<AlignedRow> getAlignedRows() {
            return alignedRows;
        }

        public int getAddedCount() {
            return addedCount;
        }

        public int getDeletedCount() {
            return deletedCount;
        }

        public boolean hasChanges() {
            return addedCount > 0 || deletedCount > 0;
        }

        /**
         * Returns aligned rows filtered to show only changed sections with context.
         *
         * @param contextLines number of context lines around changes
         * @return filtered list of aligned rows
         */
        public List<AlignedRow> getAlignedRowsWithContext(int contextLines) {
            return filterWithContext(alignedRows, contextLines);
        }
    }

    /**
     * Computes diff between two texts.
     *
     * @param oldText original text
     * @param newText modified text
     * @return diff result with hunks and aligned rows
     */
    public static DiffResult computeDiff(String oldText, String newText) {
        return computeDiff(oldText, newText, DEFAULT_CONTEXT_LINES);
    }

    /**
     * Computes diff between two texts with specified context lines.
     *
     * @param oldText original text
     * @param newText modified text
     * @param contextLines number of context lines around changes
     * @return diff result with hunks and aligned rows
     */
    public static DiffResult computeDiff(String oldText, String newText, int contextLines) {
        String[] oldLines = splitLines(oldText);
        String[] newLines = splitLines(newText);

        // Compute LCS-based diff using Myers-like algorithm
        List<DiffLine> diffLines = computeDiffLines(oldLines, newLines);

        // Build hunks with context
        List<DiffHunk> hunks = buildHunks(diffLines, contextLines);

        // Build aligned rows for side-by-side display
        List<AlignedRow> alignedRows = buildAlignedRows(diffLines);

        // Count changes
        int added = 0, deleted = 0;
        for (DiffLine line : diffLines) {
            if (line.getType() == LineType.ADDED) added++;
            if (line.getType() == LineType.DELETED) deleted++;
        }

        return new DiffResult(diffLines, hunks, alignedRows, added, deleted);
    }

    /**
     * Splits text into lines, preserving empty trailing line.
     */
    private static String[] splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        // Normalize line endings
        text = text.replace("\r\n", "\n").replace("\r", "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        return text.split("\n", -1); //$NON-NLS-1$
    }

    /**
     * Computes diff lines using LCS-based algorithm.
     */
    private static List<DiffLine> computeDiffLines(String[] oldLines, String[] newLines) {
        int m = oldLines.length;
        int n = newLines.length;

        // Compute LCS using dynamic programming
        int[][] lcs = new int[m + 1][n + 1];
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                if (oldLines[i].equals(newLines[j])) {
                    lcs[i][j] = 1 + lcs[i + 1][j + 1];
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }

        // Backtrack to build diff
        List<DiffLine> result = new ArrayList<>();
        int i = 0, j = 0;
        int oldLineNum = 1, newLineNum = 1;

        while (i < m || j < n) {
            if (i < m && j < n && oldLines[i].equals(newLines[j])) {
                // Lines match - unchanged
                result.add(new DiffLine(LineType.UNCHANGED, oldLines[i], oldLineNum++, newLineNum++));
                i++;
                j++;
            } else if (j < n && (i >= m || lcs[i][j + 1] >= lcs[i + 1][j])) {
                // Line added in new
                result.add(new DiffLine(LineType.ADDED, newLines[j], -1, newLineNum++));
                j++;
            } else if (i < m) {
                // Line deleted from old
                result.add(new DiffLine(LineType.DELETED, oldLines[i], oldLineNum++, -1));
                i++;
            }
        }

        return result;
    }

    /**
     * Builds hunks from diff lines with context.
     */
    private static List<DiffHunk> buildHunks(List<DiffLine> diffLines, int contextLines) {
        if (diffLines.isEmpty()) {
            return Collections.emptyList();
        }

        // Find ranges of changes
        List<int[]> changeRanges = new ArrayList<>();
        int changeStart = -1;

        for (int i = 0; i < diffLines.size(); i++) {
            if (diffLines.get(i).isChanged()) {
                if (changeStart < 0) {
                    changeStart = i;
                }
            } else if (changeStart >= 0) {
                changeRanges.add(new int[] { changeStart, i - 1 });
                changeStart = -1;
            }
        }
        if (changeStart >= 0) {
            changeRanges.add(new int[] { changeStart, diffLines.size() - 1 });
        }

        if (changeRanges.isEmpty()) {
            return Collections.emptyList();
        }

        // Merge overlapping ranges (considering context)
        List<int[]> mergedRanges = new ArrayList<>();
        int[] currentRange = null;

        for (int[] range : changeRanges) {
            int start = Math.max(0, range[0] - contextLines);
            int end = Math.min(diffLines.size() - 1, range[1] + contextLines);

            if (currentRange == null) {
                currentRange = new int[] { start, end };
            } else if (start <= currentRange[1] + 1) {
                // Ranges overlap or are adjacent - merge
                currentRange[1] = Math.max(currentRange[1], end);
            } else {
                mergedRanges.add(currentRange);
                currentRange = new int[] { start, end };
            }
        }
        if (currentRange != null) {
            mergedRanges.add(currentRange);
        }

        // Build hunks from merged ranges
        List<DiffHunk> hunks = new ArrayList<>();
        for (int[] range : mergedRanges) {
            List<DiffLine> hunkLines = new ArrayList<>();
            int oldStart = -1, newStart = -1;
            int oldCount = 0, newCount = 0;

            for (int i = range[0]; i <= range[1]; i++) {
                DiffLine line = diffLines.get(i);
                hunkLines.add(line);

                switch (line.getType()) {
                    case UNCHANGED:
                        if (oldStart < 0) oldStart = line.getOldLineNumber();
                        if (newStart < 0) newStart = line.getNewLineNumber();
                        oldCount++;
                        newCount++;
                        break;
                    case DELETED:
                        if (oldStart < 0) oldStart = line.getOldLineNumber();
                        oldCount++;
                        break;
                    case ADDED:
                        if (newStart < 0) newStart = line.getNewLineNumber();
                        newCount++;
                        break;
                }
            }

            if (oldStart < 0) oldStart = 1;
            if (newStart < 0) newStart = 1;

            hunks.add(new DiffHunk(oldStart, oldCount, newStart, newCount, hunkLines));
        }

        return hunks;
    }

    /**
     * Builds aligned rows for side-by-side display.
     */
    private static List<AlignedRow> buildAlignedRows(List<DiffLine> diffLines) {
        List<AlignedRow> rows = new ArrayList<>();

        int i = 0;
        while (i < diffLines.size()) {
            DiffLine line = diffLines.get(i);

            switch (line.getType()) {
                case UNCHANGED:
                    rows.add(new AlignedRow(
                            line.getContent(),
                            line.getContent(),
                            line.getOldLineNumber(),
                            line.getNewLineNumber(),
                            AlignedRow.RowType.UNCHANGED
                    ));
                    i++;
                    break;

                case DELETED:
                    // Check if next line is ADDED (modification)
                    if (i + 1 < diffLines.size() && diffLines.get(i + 1).getType() == LineType.ADDED) {
                        DiffLine addedLine = diffLines.get(i + 1);
                        rows.add(new AlignedRow(
                                line.getContent(),
                                addedLine.getContent(),
                                line.getOldLineNumber(),
                                addedLine.getNewLineNumber(),
                                AlignedRow.RowType.MODIFIED
                        ));
                        i += 2;
                    } else {
                        // Pure deletion
                        rows.add(new AlignedRow(
                                line.getContent(),
                                "", //$NON-NLS-1$
                                line.getOldLineNumber(),
                                -1,
                                AlignedRow.RowType.DELETED
                        ));
                        i++;
                    }
                    break;

                case ADDED:
                    // Pure addition (no preceding delete)
                    rows.add(new AlignedRow(
                            "", //$NON-NLS-1$
                            line.getContent(),
                            -1,
                            line.getNewLineNumber(),
                            AlignedRow.RowType.ADDED
                    ));
                    i++;
                    break;
            }
        }

        return rows;
    }

    /**
     * Filters aligned rows to show only changed sections with context.
     */
    private static List<AlignedRow> filterWithContext(List<AlignedRow> rows, int contextLines) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        // Find indices of changed rows
        List<Integer> changeIndices = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).isChanged()) {
                changeIndices.add(i);
            }
        }

        if (changeIndices.isEmpty()) {
            return Collections.emptyList();
        }

        // Build set of indices to include
        boolean[] include = new boolean[rows.size()];
        for (int changeIdx : changeIndices) {
            int start = Math.max(0, changeIdx - contextLines);
            int end = Math.min(rows.size() - 1, changeIdx + contextLines);
            for (int i = start; i <= end; i++) {
                include[i] = true;
            }
        }

        // Build result, adding separator markers for gaps
        List<AlignedRow> result = new ArrayList<>();
        boolean inGap = false;

        for (int i = 0; i < rows.size(); i++) {
            if (include[i]) {
                if (inGap && !result.isEmpty()) {
                    // Add separator row
                    result.add(new AlignedRow(
                            "...", //$NON-NLS-1$
                            "...", //$NON-NLS-1$
                            -1,
                            -1,
                            AlignedRow.RowType.UNCHANGED
                    ));
                }
                result.add(rows.get(i));
                inGap = false;
            } else {
                inGap = true;
            }
        }

        return result;
    }
}
