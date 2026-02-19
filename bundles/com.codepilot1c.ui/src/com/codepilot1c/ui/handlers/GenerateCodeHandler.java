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
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import com.codepilot1c.core.settings.PromptCatalog;
import com.codepilot1c.core.settings.PromptTemplateService;
import com.codepilot1c.core.settings.VibePreferenceConstants;
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
        Map<String, String> variables = new HashMap<>();
        variables.put("description", description); //$NON-NLS-1$
        String preferenceKey = VibePreferenceConstants.PREF_PROMPT_TEMPLATE_GENERATE_CODE;
        return PromptTemplateService.getInstance().applyTemplate(
                preferenceKey,
                PromptCatalog.getDefaultTemplate(preferenceKey),
                variables,
                PromptCatalog.getRequiredPlaceholders(preferenceKey));
    }
}
