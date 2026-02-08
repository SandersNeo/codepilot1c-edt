/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.handlers;

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
import com.codepilot1c.ui.views.ChatView;

/**
 * Handler for finding similar code in the codebase using RAG.
 *
 * <p>This handler takes the selected code and uses semantic search
 * to find similar code patterns in the indexed codebase.</p>
 */
public class FindSimilarCodeHandler extends AbstractHandler {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(FindSimilarCodeHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        LOG.info("execute: обработчик FindSimilarCode вызван"); //$NON-NLS-1$

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
            // Open chat view and send find similar request
            LOG.debug("execute: открытие ChatView..."); //$NON-NLS-1$
            ChatView chatView = (ChatView) page.showView(ChatView.ID);
            if (chatView == null) {
                LOG.error("execute: не удалось открыть ChatView (null)"); //$NON-NLS-1$
                return null;
            }
            String message = buildFindSimilarPrompt(selectedText);
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

    private String buildFindSimilarPrompt(String code) {
        return String.format("""
            Найди похожий код в кодовой базе для следующего фрагмента:

            ```bsl
            %s
            ```

            Используй семантический поиск (RAG) чтобы найти:
            1. Похожие паттерны кода
            2. Примеры использования похожей логики
            3. Возможные места для переиспользования
            """, code); //$NON-NLS-1$
    }
}
