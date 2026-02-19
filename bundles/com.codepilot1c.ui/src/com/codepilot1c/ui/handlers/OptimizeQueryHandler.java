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
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
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
 * Handler for optimizing SDBL queries using AI.
 *
 * <p>This handler analyzes selected SDBL query text and provides
 * optimization suggestions including:</p>
 * <ul>
 *   <li>Index usage recommendations</li>
 *   <li>Query structure improvements</li>
 *   <li>Performance optimizations</li>
 *   <li>Best practices for 1C query language</li>
 * </ul>
 */
public class OptimizeQueryHandler extends AbstractHandler {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(OptimizeQueryHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        LOG.info("execute: обработчик OptimizeQuery вызван"); //$NON-NLS-1$

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

        // Get selected text from editor
        String selectedText = getSelectedText(page);
        LOG.debug("execute: выделенный текст длиной %d символов", selectedText != null ? selectedText.length() : 0); //$NON-NLS-1$

        if (selectedText == null || selectedText.trim().isEmpty()) {
            LOG.warn("execute: нет выделенного текста, выход"); //$NON-NLS-1$
            return null;
        }

        try {
            // Open chat view and send optimize query request
            LOG.debug("execute: открытие ChatView..."); //$NON-NLS-1$
            ChatView chatView = (ChatView) page.showView(ChatView.ID);
            if (chatView == null) {
                LOG.error("execute: не удалось открыть ChatView (null)"); //$NON-NLS-1$
                return null;
            }
            String message = buildOptimizeQueryPrompt(selectedText);
            LOG.info("execute: отправка сообщения в ChatView (длина=%d)", message.length()); //$NON-NLS-1$
            chatView.sendProgrammaticMessage(message);
            LOG.debug("execute: сообщение отправлено"); //$NON-NLS-1$
        } catch (PartInitException e) {
            LOG.error("execute: ошибка при открытии ChatView: %s", e.getMessage()); //$NON-NLS-1$
            throw new ExecutionException("Failed to open Chat view", e); //$NON-NLS-1$
        }

        return null;
    }

    private String getSelectedText(IWorkbenchPage page) {
        IEditorPart editor = page.getActiveEditor();
        LOG.debug("getSelectedText: редактор=%s", editor != null ? editor.getClass().getName() : "null"); //$NON-NLS-1$ //$NON-NLS-2$

        if (editor == null) {
            return null;
        }

        ITextEditor textEditor = null;

        if (editor instanceof ITextEditor) {
            textEditor = (ITextEditor) editor;
        }

        if (textEditor == null && editor instanceof org.eclipse.core.runtime.IAdaptable) {
            textEditor = ((org.eclipse.core.runtime.IAdaptable) editor).getAdapter(ITextEditor.class);
        }

        if (textEditor == null) {
            org.eclipse.jface.viewers.ISelectionProvider selectionProvider = editor.getSite().getSelectionProvider();
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

        LOG.warn("getSelectedText: не удалось получить выделенный текст"); //$NON-NLS-1$
        return null;
    }

    private String buildOptimizeQueryPrompt(String queryText) {
        Map<String, String> variables = new HashMap<>();
        variables.put("query", queryText); //$NON-NLS-1$
        String preferenceKey = VibePreferenceConstants.PREF_PROMPT_TEMPLATE_OPTIMIZE_QUERY;
        return PromptTemplateService.getInstance().applyTemplate(
                preferenceKey,
                PromptCatalog.getDefaultTemplate(preferenceKey),
                variables,
                PromptCatalog.getRequiredPlaceholders(preferenceKey));
    }
}
