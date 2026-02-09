/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.editor;

import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.IFileEditorInput;

import com.codepilot1c.core.diff.CodeChange;
import com.codepilot1c.core.diff.CodeDiffUtils;
import com.codepilot1c.core.diff.ICodeApplicator.ApplyResult;
import com.codepilot1c.ui.internal.VibeUiPlugin;

/**
 * Service for applying AI-generated code to editors.
 *
 * <p>Provides high-level methods for common code application scenarios:</p>
 * <ul>
 *   <li>Insert code at cursor position</li>
 *   <li>Replace selected code</li>
 *   <li>Apply code from AI response with confirmation</li>
 * </ul>
 */
public class CodeApplicationService {

    private static CodeApplicationService instance;

    private CodeApplicationService() {
    }

    /**
     * Returns the singleton instance.
     *
     * @return the instance
     */
    public static synchronized CodeApplicationService getInstance() {
        if (instance == null) {
            instance = new CodeApplicationService();
        }
        return instance;
    }

    /**
     * Inserts code at the current cursor position in the active editor.
     *
     * @param code the code to insert
     * @return true if successful
     */
    public boolean insertAtCursor(String code) {
        ITextEditor editor = getActiveTextEditor();
        if (editor == null || code == null || code.isEmpty()) {
            return false;
        }

        ITextSelection selection = getSelection(editor);
        if (selection == null) {
            return false;
        }

        EditorCodeApplicator applicator = new EditorCodeApplicator(editor);
        CodeChange change = CodeChange.insert(selection.getOffset(), code).build();
        ApplyResult result = applicator.apply(change);

        if (result.isSuccess()) {
            // Move caret to end of inserted code
            applicator.moveCaret(selection.getOffset() + code.length());
        }

        return result.isSuccess();
    }

    /**
     * Replaces the current selection with new code.
     *
     * @param newCode the replacement code
     * @return true if successful
     */
    public boolean replaceSelection(String newCode) {
        ITextEditor editor = getActiveTextEditor();
        if (editor == null || newCode == null) {
            return false;
        }

        ITextSelection selection = getSelection(editor);
        if (selection == null || selection.getLength() == 0) {
            return insertAtCursor(newCode);
        }

        EditorCodeApplicator applicator = new EditorCodeApplicator(editor);
        CodeChange change = CodeChange.replace(
                selection.getOffset(),
                selection.getLength(),
                selection.getText(),
                newCode).build();

        ApplyResult result = applicator.apply(change);

        if (result.isSuccess()) {
            // Move caret to end of replaced code
            applicator.moveCaret(selection.getOffset() + newCode.length());
        }

        return result.isSuccess();
    }

    /**
     * Applies code from an AI response, extracting code blocks automatically.
     *
     * @param aiResponse the AI response containing code
     * @param replaceSelection true to replace selection, false to insert
     * @return true if successful
     */
    public boolean applyFromResponse(String aiResponse, boolean replaceSelection) {
        String code = CodeDiffUtils.extractFirstCodeBlock(aiResponse);
        if (code == null || code.isEmpty()) {
            // Try using stripped markdown as fallback
            code = CodeDiffUtils.stripMarkdown(aiResponse);
        }

        if (code == null || code.isEmpty()) {
            showMessage("No code found", "Could not extract code from the AI response."); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }

        if (replaceSelection) {
            return replaceSelection(code);
        } else {
            return insertAtCursor(code);
        }
    }

    /**
     * Applies code from an AI response with user confirmation.
     *
     * @param aiResponse the AI response containing code
     * @param title dialog title
     * @param replaceSelection true to replace selection, false to insert
     * @return true if applied, false if cancelled or failed
     */
    public boolean applyWithConfirmation(String aiResponse, String title, boolean replaceSelection) {
        String code = CodeDiffUtils.extractFirstCodeBlock(aiResponse);
        if (code == null || code.isEmpty()) {
            code = CodeDiffUtils.stripMarkdown(aiResponse);
        }

        if (code == null || code.isEmpty()) {
            showMessage("No code found", "Could not extract code from the AI response."); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }

        // Show confirmation dialog
        Shell shell = getShell();
        if (shell == null) {
            return false;
        }

        String message = replaceSelection
                ? "Replace the selected code with the AI-generated code?" //$NON-NLS-1$
                : "Insert the AI-generated code at cursor position?"; //$NON-NLS-1$

        boolean confirmed = MessageDialog.openConfirm(shell, title, message + "\n\nCode preview:\n" + truncateForDisplay(code)); //$NON-NLS-1$

        if (!confirmed) {
            return false;
        }

        if (replaceSelection) {
            return replaceSelection(code);
        } else {
            return insertAtCursor(code);
        }
    }

    /**
     * Applies a list of code changes to the active editor.
     *
     * @param changes the changes to apply
     * @param undoLabel the undo label
     * @return the apply result
     */
    public ApplyResult applyChanges(List<CodeChange> changes, String undoLabel) {
        ITextEditor editor = getActiveTextEditor();
        if (editor == null) {
            return ApplyResult.failure("No active text editor", null); //$NON-NLS-1$
        }

        EditorCodeApplicator applicator = new EditorCodeApplicator(editor);
        return applicator.applyWithUndo(changes, undoLabel);
    }

    /**
     * Gets the current text selection.
     *
     * @return the selection info, or null
     */
    public SelectionInfo getCurrentSelection() {
        ITextEditor editor = getActiveTextEditor();
        if (editor == null) {
            return null;
        }

        ITextSelection selection = getSelection(editor);
        if (selection == null) {
            return null;
        }

        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        String fileName = getEditorFileName(editor);

        return new SelectionInfo(
                selection.getOffset(),
                selection.getLength(),
                selection.getText(),
                document != null ? document.get() : null,
                fileName);
    }

    /**
     * Gets the file name of the editor.
     */
    private String getEditorFileName(ITextEditor editor) {
        try {
            if (editor.getEditorInput() != null) {
                // Prefer full path when available (makes prompt context much more useful).
                if (editor.getEditorInput() instanceof IFileEditorInput fileInput
                        && fileInput.getFile() != null
                        && fileInput.getFile().getLocation() != null) {
                    return fileInput.getFile().getLocation().toOSString();
                }
                if (editor.getEditorInput() instanceof IURIEditorInput uriInput
                        && uriInput.getURI() != null) {
                    return uriInput.getURI().toString();
                }
                return editor.getEditorInput().getName();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * Returns the active text editor.
     */
    private ITextEditor getActiveTextEditor() {
        try {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window == null) {
                return null;
            }

            IEditorPart editor = window.getActivePage().getActiveEditor();
            if (editor instanceof ITextEditor) {
                return (ITextEditor) editor;
            }
        } catch (Exception e) {
            VibeUiPlugin.log(e);
        }
        return null;
    }

    /**
     * Returns the current selection in the editor.
     */
    private ITextSelection getSelection(ITextEditor editor) {
        try {
            return (ITextSelection) editor.getSelectionProvider().getSelection();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the active shell.
     */
    private Shell getShell() {
        Display display = Display.getCurrent();
        if (display == null) {
            display = Display.getDefault();
        }
        return display.getActiveShell();
    }

    /**
     * Shows an information message.
     */
    private void showMessage(String title, String message) {
        Shell shell = getShell();
        if (shell != null) {
            MessageDialog.openInformation(shell, title, message);
        }
    }

    /**
     * Truncates code for display in dialog.
     */
    private String truncateForDisplay(String code) {
        if (code.length() <= 500) {
            return code;
        }
        return code.substring(0, 500) + "\n... (truncated)"; //$NON-NLS-1$
    }

    /**
     * Information about the current selection.
     */
    public static class SelectionInfo {
        private final int offset;
        private final int length;
        private final String selectedText;
        private final String documentText;
        private final String fileName;

        public SelectionInfo(int offset, int length, String selectedText, String documentText, String fileName) {
            this.offset = offset;
            this.length = length;
            this.selectedText = selectedText;
            this.documentText = documentText;
            this.fileName = fileName;
        }

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return length;
        }

        public String getSelectedText() {
            return selectedText;
        }

        public String getDocumentText() {
            return documentText;
        }

        public String getFileName() {
            return fileName;
        }

        public boolean hasSelection() {
            return length > 0;
        }

        public boolean hasDocument() {
            return documentText != null && !documentText.isEmpty();
        }
    }
}
