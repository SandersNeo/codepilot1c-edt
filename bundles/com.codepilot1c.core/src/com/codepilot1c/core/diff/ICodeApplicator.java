/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.diff;

import java.util.List;

/**
 * Interface for applying code changes to documents.
 *
 * <p>Implementations handle the mechanics of inserting, replacing, or deleting
 * code in specific document types (e.g., Eclipse IDocument, plain text files).</p>
 */
public interface ICodeApplicator {

    /**
     * Result of applying code changes.
     */
    class ApplyResult {
        private final boolean success;
        private final String message;
        private final int changesApplied;
        private final int changesFailed;
        private final Exception error;

        private ApplyResult(boolean success, String message, int applied, int failed, Exception error) {
            this.success = success;
            this.message = message;
            this.changesApplied = applied;
            this.changesFailed = failed;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getChangesApplied() {
            return changesApplied;
        }

        public int getChangesFailed() {
            return changesFailed;
        }

        public Exception getError() {
            return error;
        }

        public static ApplyResult success(int applied) {
            return new ApplyResult(true, "Applied " + applied + " change(s)", applied, 0, null); //$NON-NLS-1$ //$NON-NLS-2$
        }

        public static ApplyResult partial(int applied, int failed, String message) {
            return new ApplyResult(false, message, applied, failed, null);
        }

        public static ApplyResult failure(String message, Exception error) {
            return new ApplyResult(false, message, 0, 0, error);
        }
    }

    /**
     * Applies a single code change.
     *
     * @param change the change to apply
     * @return the result
     */
    ApplyResult apply(CodeChange change);

    /**
     * Applies multiple code changes in order.
     *
     * <p>Changes are typically applied in reverse offset order to avoid
     * offset invalidation.</p>
     *
     * @param changes the changes to apply
     * @return the result
     */
    ApplyResult applyAll(List<CodeChange> changes);

    /**
     * Applies changes within an undo group.
     *
     * <p>All changes can be undone with a single undo operation.</p>
     *
     * @param changes the changes to apply
     * @param undoLabel the label for the undo operation
     * @return the result
     */
    ApplyResult applyWithUndo(List<CodeChange> changes, String undoLabel);

    /**
     * Validates that a change can be applied.
     *
     * @param change the change to validate
     * @return true if the change can be applied
     */
    boolean canApply(CodeChange change);

    /**
     * Returns whether this applicator supports undo operations.
     *
     * @return true if undo is supported
     */
    boolean supportsUndo();

    /**
     * Previews changes without applying them.
     *
     * @param changes the changes to preview
     * @return the resulting document text (full or partial)
     */
    String preview(List<CodeChange> changes);
}
