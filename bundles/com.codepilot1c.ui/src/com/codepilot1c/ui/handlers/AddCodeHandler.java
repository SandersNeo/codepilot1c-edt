/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.handlers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import com.codepilot1c.core.logging.VibeLogger;
import com.codepilot1c.core.settings.PromptCatalog;
import com.codepilot1c.core.settings.PromptTemplateService;
import com.codepilot1c.core.settings.VibePreferenceConstants;
import com.codepilot1c.ui.views.ChatView;

/**
 * Handler for adding code at cursor position using AI.
 *
 * <p>Opens a dialog for the user to describe what code to add,
 * then sends the request to AI and displays the result in chat view.</p>
 *
 * <p>Future enhancement: Insert the generated code directly at cursor position.</p>
 */
public class AddCodeHandler extends AbstractHandler {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(AddCodeHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        LOG.info("execute: обработчик AddCode вызван"); //$NON-NLS-1$

        // Get window - try from event first, then fall back to PlatformUI
        IWorkbenchWindow window = null;
        if (event != null) {
            window = HandlerUtil.getActiveWorkbenchWindow(event);
            LOG.debug("execute: окно из события=%s", window != null ? "получено" : "null"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        if (window == null) {
            window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            LOG.debug("execute: окно из PlatformUI=%s", window != null ? "получено" : "null"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        if (window == null) {
            LOG.error("execute: не удалось получить окно workbench, выход"); //$NON-NLS-1$
            return null;
        }

        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
            LOG.error("execute: активная страница = null, выход"); //$NON-NLS-1$
            return null;
        }

        IEditorPart editor = page.getActiveEditor();
        LOG.debug("execute: редактор=%s", editor != null ? editor.getClass().getName() : "null"); //$NON-NLS-1$ //$NON-NLS-2$

        if (editor == null) {
            LOG.warn("execute: редактор = null, выход"); //$NON-NLS-1$
            return null;
        }

        // Получаем selection напрямую из редактора
        ISelectionProvider selectionProvider = editor.getSite().getSelectionProvider();
        if (selectionProvider == null) {
            LOG.warn("execute: selectionProvider = null, выход"); //$NON-NLS-1$
            return null;
        }

        ISelection selection = selectionProvider.getSelection();
        LOG.debug("execute: selection=%s", selection != null ? selection.getClass().getName() : "null"); //$NON-NLS-1$ //$NON-NLS-2$

        if (!(selection instanceof ITextSelection)) {
            LOG.warn("execute: selection не является ITextSelection, выход"); //$NON-NLS-1$
            return null;
        }

        ITextSelection textSelection = (ITextSelection) selection;
        LOG.debug("execute: offset=%d, length=%d", textSelection.getOffset(), textSelection.getLength()); //$NON-NLS-1$

        // Get surrounding context if text editor is available
        String context = ""; //$NON-NLS-1$
        ITextEditor textEditor = null;
        if (editor instanceof ITextEditor) {
            textEditor = (ITextEditor) editor;
        } else if (editor instanceof IAdaptable) {
            textEditor = ((IAdaptable) editor).getAdapter(ITextEditor.class);
        }
        if (textEditor != null) {
            context = getSurroundingContext(textEditor, textSelection.getOffset());
        }
        LOG.debug("execute: контекст длиной %d символов", context.length()); //$NON-NLS-1$

        // Ask user what to add
        LOG.debug("execute: открытие диалога ввода..."); //$NON-NLS-1$
        InputDialog dialog = new InputDialog(
                window.getShell(),
                "Add Code", //$NON-NLS-1$
                "Describe the code you want to add:", //$NON-NLS-1$
                "", //$NON-NLS-1$
                null);

        if (dialog.open() != Window.OK) {
            LOG.debug("execute: пользователь отменил диалог"); //$NON-NLS-1$
            return null;
        }

        String description = dialog.getValue();
        LOG.debug("execute: описание от пользователя: '%s'", description); //$NON-NLS-1$

        if (description == null || description.trim().isEmpty()) {
            LOG.warn("execute: пустое описание, выход"); //$NON-NLS-1$
            return null;
        }

        // Format the prompt
        Map<String, String> variables = new HashMap<>();
        variables.put("context", context); //$NON-NLS-1$
        variables.put("request", description); //$NON-NLS-1$
        String preferenceKey = VibePreferenceConstants.PREF_PROMPT_TEMPLATE_ADD_CODE;
        String prompt = PromptTemplateService.getInstance().applyTemplate(
                preferenceKey,
                PromptCatalog.getDefaultTemplate(preferenceKey),
                variables,
                PromptCatalog.getRequiredPlaceholders(preferenceKey));

        // Open chat view and send the prompt
        try {
            LOG.debug("execute: открытие ChatView..."); //$NON-NLS-1$
            ChatView chatView = (ChatView) page.showView(ChatView.VIEW_ID);
            if (chatView == null) {
                LOG.error("execute: не удалось открыть ChatView (null)"); //$NON-NLS-1$
                return null;
            }
            LOG.info("execute: отправка сообщения в ChatView (длина=%d)", prompt.length()); //$NON-NLS-1$
            chatView.sendMessage(prompt);
            LOG.debug("execute: сообщение отправлено"); //$NON-NLS-1$
        } catch (PartInitException e) {
            LOG.error("execute: ошибка при открытии ChatView: %s", e.getMessage()); //$NON-NLS-1$
            throw new ExecutionException("Failed to open chat view", e); //$NON-NLS-1$
        }

        return null;
    }

    private String getSurroundingContext(ITextEditor editor, int offset) {
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        if (document == null) {
            LOG.warn("getSurroundingContext: document = null"); //$NON-NLS-1$
            return ""; //$NON-NLS-1$
        }

        try {
            // Get a few lines before and after cursor
            int line = document.getLineOfOffset(offset);
            int startLine = Math.max(0, line - 5);
            int endLine = Math.min(document.getNumberOfLines() - 1, line + 5);

            int startOffset = document.getLineOffset(startLine);
            int endOffset = document.getLineOffset(endLine) + document.getLineLength(endLine);

            return document.get(startOffset, endOffset - startOffset);
        } catch (BadLocationException e) {
            LOG.warn("getSurroundingContext: BadLocationException: %s", e.getMessage()); //$NON-NLS-1$
            return ""; //$NON-NLS-1$
        }
    }
}
