/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.texteditor.ITextEditor;

import com.codepilot1c.core.diff.CodeChange;
import com.codepilot1c.core.diff.ICodeApplicator;
import com.codepilot1c.ui.internal.VibeUiPlugin;

/**
 * Code applicator for Eclipse text editors.
 *
 * <p>Applies code changes to IDocument with proper undo/redo support.
 * Changes are applied on the UI thread to ensure thread safety.</p>
 */
public class EditorCodeApplicator implements ICodeApplicator {

    private final ITextEditor editor;
    private final IDocument document;

    /**
     * Creates an applicator for the given editor.
     *
     * @param editor the text editor
     */
    public EditorCodeApplicator(ITextEditor editor) {
        this.editor = editor;
        this.document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
    }

    /**
     * Creates an applicator for the given document.
     *
     * @param document the document
     */
    public EditorCodeApplicator(IDocument document) {
        this.editor = null;
        this.document = document;
    }

    @Override
    public ApplyResult apply(CodeChange change) {
        return applyAll(List.of(change));
    }

    @Override
    public ApplyResult applyAll(List<CodeChange> changes) {
        return applyWithUndo(changes, "AI Code Change"); //$NON-NLS-1$
    }

    @Override
    public ApplyResult applyWithUndo(List<CodeChange> changes, String undoLabel) {
        if (document == null) {
            return ApplyResult.failure("No document available", null); //$NON-NLS-1$
        }

        if (changes == null || changes.isEmpty()) {
            return ApplyResult.success(0);
        }

        // Sort changes by offset descending to avoid offset invalidation
        List<CodeChange> sortedChanges = new ArrayList<>(changes);
        sortedChanges.sort(Comparator.comparingInt(CodeChange::getOffset).reversed());

        // Execute on UI thread
        final ApplyResult[] result = new ApplyResult[1];
        Display display = Display.getDefault();

        Runnable applyRunnable = () -> {
            result[0] = doApplyChanges(sortedChanges, undoLabel);
        };

        if (Display.getCurrent() != null) {
            applyRunnable.run();
        } else {
            display.syncExec(applyRunnable);
        }

        return result[0];
    }

    /**
     * Applies changes within a document rewrite session for better performance.
     */
    private ApplyResult doApplyChanges(List<CodeChange> changes, String undoLabel) {
        IDocumentUndoManager undoManager = DocumentUndoManagerRegistry.getDocumentUndoManager(document);
        DocumentRewriteSession rewriteSession = null;

        try {
            // Start undo compound
            if (undoManager != null) {
                undoManager.beginCompoundChange();
            }

            // Start rewrite session for better performance
            if (document instanceof IDocumentExtension4 ext4) {
                rewriteSession = ext4.startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
            }

            int applied = 0;
            int failed = 0;
            StringBuilder errors = new StringBuilder();

            for (CodeChange change : changes) {
                try {
                    applyChange(change);
                    applied++;
                } catch (BadLocationException e) {
                    failed++;
                    errors.append(String.format("Change at offset %d failed: %s\n", //$NON-NLS-1$
                            change.getOffset(), e.getMessage()));
                    VibeUiPlugin.log(e);
                }
            }

            if (failed > 0) {
                return ApplyResult.partial(applied, failed, errors.toString().trim());
            }

            return ApplyResult.success(applied);

        } finally {
            // End rewrite session
            if (rewriteSession != null && document instanceof IDocumentExtension4 ext4) {
                ext4.stopRewriteSession(rewriteSession);
            }

            // End undo compound
            if (undoManager != null) {
                undoManager.endCompoundChange();
            }
        }
    }

    /**
     * Applies a single change to the document.
     */
    private void applyChange(CodeChange change) throws BadLocationException {
        switch (change.getType()) {
            case INSERT:
                document.replace(change.getOffset(), 0, change.getNewText());
                break;

            case REPLACE:
                document.replace(change.getOffset(), change.getLength(), change.getNewText());
                break;

            case DELETE:
                document.replace(change.getOffset(), change.getLength(), ""); //$NON-NLS-1$
                break;
        }
    }

    @Override
    public boolean canApply(CodeChange change) {
        if (document == null || change == null) {
            return false;
        }

        int docLength = document.getLength();
        int offset = change.getOffset();
        int endOffset = change.getEndOffset();

        // Check bounds
        if (offset < 0 || offset > docLength) {
            return false;
        }
        if (endOffset < offset || endOffset > docLength) {
            return false;
        }

        // For replacements, verify the old text matches
        if (change.getType() == CodeChange.ChangeType.REPLACE && change.getOldText() != null) {
            try {
                String actual = document.get(offset, change.getLength());
                return actual.equals(change.getOldText());
            } catch (BadLocationException e) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean supportsUndo() {
        return DocumentUndoManagerRegistry.getDocumentUndoManager(document) != null;
    }

    @Override
    public String preview(List<CodeChange> changes) {
        if (document == null || changes == null || changes.isEmpty()) {
            return document != null ? document.get() : ""; //$NON-NLS-1$
        }

        // Work on a copy of the document content
        StringBuilder content = new StringBuilder(document.get());

        // Sort changes by offset descending
        List<CodeChange> sortedChanges = new ArrayList<>(changes);
        sortedChanges.sort(Comparator.comparingInt(CodeChange::getOffset).reversed());

        for (CodeChange change : sortedChanges) {
            int offset = change.getOffset();
            int endOffset = change.getEndOffset();

            if (offset < 0 || offset > content.length()) {
                continue;
            }

            switch (change.getType()) {
                case INSERT:
                    content.insert(offset, change.getNewText());
                    break;

                case REPLACE:
                    if (endOffset <= content.length()) {
                        content.replace(offset, endOffset, change.getNewText());
                    }
                    break;

                case DELETE:
                    if (endOffset <= content.length()) {
                        content.delete(offset, endOffset);
                    }
                    break;
            }
        }

        return content.toString();
    }

    /**
     * Returns the editor associated with this applicator.
     *
     * @return the editor, or null
     */
    public ITextEditor getEditor() {
        return editor;
    }

    /**
     * Returns the document associated with this applicator.
     *
     * @return the document
     */
    public IDocument getDocument() {
        return document;
    }

    /**
     * Moves the caret to the specified offset.
     *
     * @param offset the offset
     */
    public void moveCaret(int offset) {
        if (editor != null) {
            Display.getDefault().asyncExec(() -> {
                editor.selectAndReveal(offset, 0);
            });
        }
    }
}
