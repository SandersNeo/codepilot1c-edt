/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import com.codepilot1c.core.logging.VibeLogger;
import com.codepilot1c.ui.views.ChatView;

/**
 * Handler for fixing selected code using AI.
 *
 * <p>Sends selected code to AI with a "fix this code" prompt
 * and displays the result in the chat view.</p>
 *
 * <p>Future enhancement: Apply the fix directly to the editor.</p>
 */
public class FixCodeHandler extends AbstractHandler {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(FixCodeHandler.class);

    private static final String FIX_PROMPT_TEMPLATE =
            "Fix the following code. Identify any bugs, errors, or issues and provide the corrected version:\n\n```\n%s\n```"; //$NON-NLS-1$

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        LOG.info("execute: обработчик FixCode вызван"); //$NON-NLS-1$

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

        String selectedCode = getSelectedText(page);
        LOG.debug("execute: выделенный код длиной %d символов", selectedCode != null ? selectedCode.length() : 0); //$NON-NLS-1$

        if (selectedCode == null || selectedCode.trim().isEmpty()) {
            LOG.warn("execute: нет выделенного кода, выход"); //$NON-NLS-1$
            return null;
        }

        // Format the prompt
        String prompt = String.format(FIX_PROMPT_TEMPLATE, selectedCode);

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

    private String getSelectedText(IWorkbenchPage page) {
        IEditorPart editor = page.getActiveEditor();
        LOG.debug("getSelectedText: редактор=%s", editor != null ? editor.getClass().getName() : "null"); //$NON-NLS-1$ //$NON-NLS-2$

        if (editor == null) {
            return null;
        }

        // Попробуем несколько способов получить ITextEditor
        ITextEditor textEditor = null;

        if (editor instanceof ITextEditor) {
            textEditor = (ITextEditor) editor;
        }

        if (textEditor == null && editor instanceof IAdaptable) {
            textEditor = ((IAdaptable) editor).getAdapter(ITextEditor.class);
        }

        // Получить selection напрямую из редактора
        if (textEditor == null) {
            ISelectionProvider selectionProvider = editor.getSite().getSelectionProvider();
            if (selectionProvider != null) {
                ISelection selection = selectionProvider.getSelection();
                if (selection instanceof ITextSelection textSelection) {
                    return textSelection.getText();
                }
            }
        }

        if (textEditor != null) {
            ISelection selection = textEditor.getSelectionProvider().getSelection();
            if (selection instanceof ITextSelection textSelection) {
                return textSelection.getText();
            }
        }

        return null;
    }
}
