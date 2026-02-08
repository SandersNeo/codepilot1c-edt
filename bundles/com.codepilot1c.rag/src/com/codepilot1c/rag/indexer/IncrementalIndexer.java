/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.rag.indexer;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.codepilot1c.rag.internal.VibeRagPlugin;

/**
 * Listens for resource changes and updates the index incrementally.
 *
 * <p>This listener debounces rapid changes to avoid excessive reindexing.</p>
 */
public class IncrementalIndexer implements IResourceChangeListener {

    private static final long DEBOUNCE_DELAY_MS = 2000;

    private final CodebaseIndexer indexer;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> pendingUpdates;
    private final Set<String> pendingDeletes;

    private boolean active = false;

    /**
     * Creates a new incremental indexer.
     *
     * @param indexer the codebase indexer to use for updates
     */
    public IncrementalIndexer(CodebaseIndexer indexer) {
        this.indexer = indexer;
        this.scheduler = new ScheduledThreadPoolExecutor(1);
        this.pendingUpdates = new ConcurrentHashMap<>();
        this.pendingDeletes = ConcurrentHashMap.newKeySet();
    }

    /**
     * Starts listening for resource changes.
     */
    public void start() {
        if (!active) {
            ResourcesPlugin.getWorkspace().addResourceChangeListener(
                    this,
                    IResourceChangeEvent.POST_CHANGE
            );
            active = true;
            VibeRagPlugin.logInfo("IncrementalIndexer started"); //$NON-NLS-1$
        }
    }

    /**
     * Stops listening for resource changes.
     */
    public void stop() {
        if (active) {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
            active = false;

            // Cancel pending updates
            for (ScheduledFuture<?> future : pendingUpdates.values()) {
                future.cancel(false);
            }
            pendingUpdates.clear();
            pendingDeletes.clear();

            scheduler.shutdown();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            VibeRagPlugin.logInfo("IncrementalIndexer stopped"); //$NON-NLS-1$
        }
    }

    /**
     * Returns whether the indexer is active.
     *
     * @return true if active
     */
    public boolean isActive() {
        return active;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getDelta() == null) {
            return;
        }

        try {
            event.getDelta().accept(new IResourceDeltaVisitor() {
                @Override
                public boolean visit(IResourceDelta delta) throws CoreException {
                    IResource resource = delta.getResource();

                    if (!(resource instanceof IFile)) {
                        return true;
                    }

                    IFile file = (IFile) resource;
                    String filePath = file.getFullPath().toString();

                    // Check if this file type is indexable
                    Optional<ICodeChunker> chunker = CodeChunkerRegistry.getInstance()
                            .getChunkerForFile(file);
                    if (!chunker.isPresent()) {
                        return true;
                    }

                    switch (delta.getKind()) {
                        case IResourceDelta.ADDED:
                        case IResourceDelta.CHANGED:
                            scheduleUpdate(file, filePath);
                            break;

                        case IResourceDelta.REMOVED:
                            scheduleDelete(filePath);
                            break;

                        default:
                            break;
                    }

                    return true;
                }
            });
        } catch (CoreException e) {
            VibeRagPlugin.logError("Failed to process resource change", e); //$NON-NLS-1$
        }
    }

    /**
     * Schedules a file update with debouncing.
     */
    private void scheduleUpdate(IFile file, String filePath) {
        // Cancel any pending delete for this file
        pendingDeletes.remove(filePath);

        // Cancel existing pending update
        ScheduledFuture<?> existing = pendingUpdates.remove(filePath);
        if (existing != null) {
            existing.cancel(false);
        }

        // Schedule new update
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                pendingUpdates.remove(filePath);
                indexer.indexFile(file);
                VibeRagPlugin.logInfo("Reindexed: " + filePath); //$NON-NLS-1$
            } catch (Exception e) {
                VibeRagPlugin.logError("Failed to reindex: " + filePath, e); //$NON-NLS-1$
            }
        }, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);

        pendingUpdates.put(filePath, future);
    }

    /**
     * Schedules a file deletion with debouncing.
     */
    private void scheduleDelete(String filePath) {
        // Cancel any pending update for this file
        ScheduledFuture<?> existing = pendingUpdates.remove(filePath);
        if (existing != null) {
            existing.cancel(false);
        }

        // Mark for deletion
        pendingDeletes.add(filePath);

        // Schedule deletion
        scheduler.schedule(() -> {
            if (pendingDeletes.remove(filePath)) {
                indexer.removeFile(filePath);
                VibeRagPlugin.logInfo("Removed from index: " + filePath); //$NON-NLS-1$
            }
        }, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the number of pending updates.
     *
     * @return pending update count
     */
    public int getPendingUpdateCount() {
        return pendingUpdates.size();
    }

    /**
     * Returns the number of pending deletes.
     *
     * @return pending delete count
     */
    public int getPendingDeleteCount() {
        return pendingDeletes.size();
    }

    /**
     * Flushes all pending operations immediately.
     */
    public void flush() {
        // Process all pending deletes
        Set<String> deletesToProcess = new HashSet<>(pendingDeletes);
        pendingDeletes.clear();
        for (String filePath : deletesToProcess) {
            indexer.removeFile(filePath);
        }

        // Note: pending updates require the IFile which we don't have here
        // They will be processed when their scheduled time arrives
    }
}
