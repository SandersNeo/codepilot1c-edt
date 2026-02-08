/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import com.codepilot1c.ui.views.ChatView;

/**
 * Handler for opening the AI Chat view.
 */
public class OpenChatHandler extends AbstractHandler {


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

        try {
            page.showView(ChatView.ID);
        } catch (PartInitException e) {
            // Log error: "Failed to open Chat view", e //$NON-NLS-1$
            throw new ExecutionException("Failed to open Chat view", e); //$NON-NLS-1$
        }

        return null;
    }
}
