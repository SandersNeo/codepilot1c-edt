/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
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
 * Handler for reviewing/criticising selected code using AI.
 *
 * <p>Sends selected code to AI for code review and displays
 * the critique in the chat view.</p>
 */
public class CriticiseCodeHandler extends AbstractHandler {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(CriticiseCodeHandler.class);

    private static final String REVIEW_PROMPT_TEMPLATE =
            "Review and critique the following code. Identify potential issues, suggest improvements, " //$NON-NLS-1$
            + "and highlight any best practice violations:\n\n```\n%s\n```"; //$NON-NLS-1$

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        LOG.info("execute: обработчик CriticiseCode вызван"); //$NON-NLS-1$

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
        String prompt = String.format(REVIEW_PROMPT_TEMPLATE, selectedCode);

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
        if (editor == null) {
            return null;
        }

        ITextEditor textEditor = null;

        if (editor instanceof ITextEditor) {
            textEditor = (ITextEditor) editor;
        }

        if (textEditor == null && editor instanceof IAdaptable) {
            textEditor = ((IAdaptable) editor).getAdapter(ITextEditor.class);
        }

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
