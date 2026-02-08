/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.handlers;

import java.nio.file.Path;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.codepilot1c.core.embedding.EmbeddingProviderRegistry;
import com.codepilot1c.core.embedding.IEmbeddingProvider;
import com.codepilot1c.core.rag.RagService;
import com.codepilot1c.core.state.VibeStateService;
import com.codepilot1c.rag.indexer.CodebaseIndexer;
import com.codepilot1c.ui.internal.Messages;

/**
 * Handler for indexing the codebase for RAG functionality.
 */
public class IndexCodebaseHandler extends AbstractHandler {

    private static final int DEFAULT_EMBEDDING_DIMENSION = 1536;
    private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small"; //$NON-NLS-1$

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);

        // Check if already indexing
        RagService ragService = RagService.getInstance();
        if (ragService.isIndexingInProgress()) {
            MessageDialog.openInformation(shell,
                    Messages.IndexCodebaseHandler_Title,
                    Messages.IndexCodebaseHandler_AlreadyIndexing);
            return null;
        }

        // Check embedding provider
        IEmbeddingProvider embeddingProvider = EmbeddingProviderRegistry.getInstance().getActiveProvider();
        if (embeddingProvider == null || !embeddingProvider.isConfigured()) {
            MessageDialog.openWarning(shell,
                    Messages.IndexCodebaseHandler_Title,
                    Messages.IndexCodebaseHandler_NoProvider);
            return null;
        }

        // Get workspace path and index path
        Path workspacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toPath();
        Path indexPath = RagService.getDefaultIndexPath(workspacePath);

        // Get embedding dimensions from provider or use default
        int dimensions = embeddingProvider.getDimensions();
        if (dimensions <= 0) {
            dimensions = DEFAULT_EMBEDDING_DIMENSION;
        }

        // Create indexer
        CodebaseIndexer indexer = CodebaseIndexer.create(indexPath, dimensions, DEFAULT_EMBEDDING_MODEL);
        if (indexer == null) {
            MessageDialog.openError(shell,
                    Messages.IndexCodebaseHandler_Title,
                    Messages.IndexCodebaseHandler_CreateFailed);
            return null;
        }

        // Update RagService with new index
        ragService.setCodebaseIndex(indexer.getIndex());
        ragService.setIndexingInProgress(true);

        // Update state service for status bar
        VibeStateService.getInstance().setIndexing(Messages.IndexCodebaseHandler_Indexing);

        // Add job completion listener
        indexer.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent jobEvent) {
                ragService.setIndexingInProgress(false);
                Display.getDefault().asyncExec(() -> {
                    if (jobEvent.getResult().getSeverity() == IStatus.OK) {
                        // Re-initialize RAG service with the new index
                        ragService.initialize(workspacePath);
                        VibeStateService.getInstance().setIdle();
                        MessageDialog.openInformation(shell,
                                Messages.IndexCodebaseHandler_Title,
                                Messages.IndexCodebaseHandler_Complete);
                    } else if (jobEvent.getResult().getSeverity() == IStatus.ERROR) {
                        VibeStateService.getInstance().setError(jobEvent.getResult().getMessage());
                        MessageDialog.openError(shell,
                                Messages.IndexCodebaseHandler_Title,
                                String.format(Messages.IndexCodebaseHandler_Failed,
                                        jobEvent.getResult().getMessage()));
                    }
                });
            }
        });

        // Schedule the job
        indexer.setUser(true);
        indexer.setPriority(Job.LONG);
        indexer.schedule();

        return null;
    }
}
