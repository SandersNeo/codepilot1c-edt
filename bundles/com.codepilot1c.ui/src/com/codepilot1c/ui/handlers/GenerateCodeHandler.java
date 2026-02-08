/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import com.codepilot1c.ui.views.ChatView;

/**
 * Handler for generating code from description using AI.
 */
public class GenerateCodeHandler extends AbstractHandler {


    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        if (window == null) {
            return null;
        }

        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
            return null;
        }

        // Show input dialog for code description
        InputDialog dialog = new InputDialog(
                window.getShell(),
                "Генерация кода", //$NON-NLS-1$
                "Опишите, какой код нужно сгенерировать:", //$NON-NLS-1$
                "", //$NON-NLS-1$
                null);

        if (dialog.open() != Window.OK) {
            return null;
        }

        String description = dialog.getValue();
        if (description == null || description.trim().isEmpty()) {
            return null;
        }

        try {
            // Open chat view and send generate request
            ChatView chatView = (ChatView) page.showView(ChatView.ID);
            String message = buildGeneratePrompt(description);
            chatView.sendProgrammaticMessage(message);
        } catch (PartInitException e) {
            // Log error: "Failed to open Chat view", e //$NON-NLS-1$
            throw new ExecutionException("Failed to open Chat view", e); //$NON-NLS-1$
        }

        return null;
    }

    private String buildGeneratePrompt(String description) {
        return String.format("""
            Сгенерируй код на языке 1С для следующей задачи:

            %s

            Требования:
            - Используй русский синтаксис языка 1С
            - Добавь комментарии к коду
            - Следуй стандартам разработки 1С
            """, description); //$NON-NLS-1$
    }
}
