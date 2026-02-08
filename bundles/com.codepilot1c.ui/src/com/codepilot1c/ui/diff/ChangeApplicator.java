/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.diff;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import com.codepilot1c.core.logging.VibeLogger;

/**
 * Applies accepted proposed changes to the workspace.
 *
 * <p>Changes are applied in a single workspace operation for atomicity
 * and to create a single undo point.</p>
 */
public class ChangeApplicator {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(ChangeApplicator.class);

    private static ChangeApplicator instance;

    private ChangeApplicator() {
    }

    /**
     * Returns the singleton instance.
     *
     * @return the instance
     */
    public static synchronized ChangeApplicator getInstance() {
        if (instance == null) {
            instance = new ChangeApplicator();
        }
        return instance;
    }

    /**
     * Applies all accepted changes in the set.
     *
     * @param changeSet the change set
     * @return result of the application
     */
    public ApplyResult applyAcceptedChanges(ProposedChangeSet changeSet) {
        List<ProposedChange> accepted = changeSet.getAcceptedChanges();
        LOG.info("applyAcceptedChanges: %d accepted changes to apply", accepted.size()); //$NON-NLS-1$

        if (accepted.isEmpty()) {
            LOG.warn("applyAcceptedChanges: no accepted changes to apply"); //$NON-NLS-1$
            return new ApplyResult(0, 0, List.of());
        }

        for (ProposedChange change : accepted) {
            LOG.debug("applyAcceptedChanges: will apply %s to %s (afterContent=%s)", //$NON-NLS-1$
                    change.getKind(), change.getFilePath(),
                    change.getAfterContent() != null ? change.getAfterContent().length() + " chars" : "NULL"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        try {
            // Run all changes in a single workspace operation
            IWorkspaceRunnable runnable = monitor -> {
                for (ProposedChange change : accepted) {
                    try {
                        LOG.debug("applyAcceptedChanges: applying change for %s", change.getFilePath()); //$NON-NLS-1$
                        applyChange(change);
                        change.markApplied();
                        LOG.info("applyAcceptedChanges: successfully applied %s", change.getFilePath()); //$NON-NLS-1$
                    } catch (CoreException e) {
                        LOG.error("applyAcceptedChanges: FAILED to apply %s: %s", //$NON-NLS-1$
                                change.getFilePath(), e.getMessage());
                        change.markFailed();
                        throw e;
                    }
                }
            };

            ResourcesPlugin.getWorkspace().run(runnable, new NullProgressMonitor());

            // Count results
            for (ProposedChange change : accepted) {
                if (change.getStatus() == ProposedChange.ChangeStatus.APPLIED) {
                    successCount++;
                } else {
                    failCount++;
                }
            }
        } catch (CoreException e) {
            LOG.error("applyAcceptedChanges: workspace operation failed: %s", e.getMessage()); //$NON-NLS-1$
            errors.add(e.getMessage());
            // Count what was applied before failure
            for (ProposedChange change : accepted) {
                if (change.getStatus() == ProposedChange.ChangeStatus.APPLIED) {
                    successCount++;
                } else {
                    failCount++;
                }
            }
        }

        LOG.info("applyAcceptedChanges: completed - success=%d, fail=%d", successCount, failCount); //$NON-NLS-1$
        return new ApplyResult(successCount, failCount, errors);
    }

    /**
     * Applies a single change.
     *
     * @param change the change to apply
     * @throws CoreException if application fails
     */
    public void applyChange(ProposedChange change) throws CoreException {
        switch (change.getKind()) {
            case CREATE -> createFile(change);
            case MODIFY, REPLACE -> modifyFile(change);
            case DELETE -> deleteFile(change);
        }
    }

    private void createFile(ProposedChange change) throws CoreException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        String normalizedPath = normalizePath(change.getFilePath());
        IFile file = root.getFile(Path.fromPortableString(normalizedPath));

        // Create parent folders if needed
        createParentFolders(file);

        // Create the file
        ByteArrayInputStream content = new ByteArrayInputStream(
                change.getAfterContent().getBytes(StandardCharsets.UTF_8));
        file.create(content, true, new NullProgressMonitor());
    }

    private void modifyFile(ProposedChange change) throws CoreException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        String normalizedPath = normalizePath(change.getFilePath());
        LOG.debug("modifyFile: original path=%s, normalized=%s", change.getFilePath(), normalizedPath); //$NON-NLS-1$

        IFile file = findFile(root, normalizedPath);
        LOG.debug("modifyFile: findFile returned %s", file != null ? file.getFullPath() : "NULL"); //$NON-NLS-1$ //$NON-NLS-2$

        if (file == null || !file.exists()) {
            LOG.error("modifyFile: file NOT FOUND - path=%s, normalized=%s", //$NON-NLS-1$
                    change.getFilePath(), normalizedPath);
            throw new CoreException(new org.eclipse.core.runtime.Status(
                    org.eclipse.core.runtime.IStatus.ERROR,
                    "com.codepilot1c.ui", //$NON-NLS-1$
                    "File not found: " + change.getFilePath())); //$NON-NLS-1$
        }

        // Check afterContent
        String afterContent = change.getAfterContent();
        if (afterContent == null) {
            LOG.error("modifyFile: afterContent is NULL for %s", change.getFilePath()); //$NON-NLS-1$
            throw new CoreException(new org.eclipse.core.runtime.Status(
                    org.eclipse.core.runtime.IStatus.ERROR,
                    "com.codepilot1c.ui", //$NON-NLS-1$
                    "afterContent is null for: " + change.getFilePath())); //$NON-NLS-1$
        }

        LOG.info("modifyFile: writing %d bytes to %s", afterContent.length(), file.getFullPath()); //$NON-NLS-1$

        // Update the file content
        ByteArrayInputStream content = new ByteArrayInputStream(
                afterContent.getBytes(StandardCharsets.UTF_8));
        file.setContents(content, true, true, new NullProgressMonitor());

        LOG.info("modifyFile: successfully wrote to %s", file.getFullPath()); //$NON-NLS-1$
    }

    private void deleteFile(ProposedChange change) throws CoreException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        String normalizedPath = normalizePath(change.getFilePath());
        IFile file = findFile(root, normalizedPath);

        if (file != null && file.exists()) {
            file.delete(true, new NullProgressMonitor());
        }
    }

    private void createParentFolders(IFile file) throws CoreException {
        org.eclipse.core.resources.IContainer parent = file.getParent();
        java.util.Stack<IFolder> foldersToCreate = new java.util.Stack<>();

        while (parent instanceof IFolder && !parent.exists()) {
            foldersToCreate.push((IFolder) parent);
            parent = parent.getParent();
        }

        while (!foldersToCreate.isEmpty()) {
            foldersToCreate.pop().create(true, true, new NullProgressMonitor());
        }
    }

    private String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        String normalized = path;

        // Strip line number suffix like :123 or :123-456
        int colonIdx = normalized.lastIndexOf(':');
        if (colonIdx > 0) {
            String suffix = normalized.substring(colonIdx + 1);
            if (suffix.matches("[0-9\\-]+")) { //$NON-NLS-1$
                normalized = normalized.substring(0, colonIdx);
                LOG.debug("normalizePath: stripped line numbers from %s -> %s", path, normalized); //$NON-NLS-1$
            }
        }

        // Remove leading slash if present
        if (normalized.startsWith("/") && !normalized.startsWith("//")) { //$NON-NLS-1$ //$NON-NLS-2$
            normalized = normalized.substring(1);
        }
        // Use forward slashes for Eclipse
        normalized = normalized.replace('\\', '/');

        LOG.debug("normalizePath: %s -> %s", path, normalized); //$NON-NLS-1$
        return normalized;
    }

    private IFile findFile(IWorkspaceRoot root, String path) {
        LOG.debug("findFile: searching for path=%s", path); //$NON-NLS-1$

        // Try direct lookup
        try {
            IResource resource = root.findMember(path);
            LOG.debug("findFile: findMember returned %s", resource != null ? resource.getClass().getSimpleName() : "null"); //$NON-NLS-1$ //$NON-NLS-2$
            if (resource instanceof IFile) {
                LOG.debug("findFile: found via findMember: %s", resource.getFullPath()); //$NON-NLS-1$
                return (IFile) resource;
            }
        } catch (Exception e) {
            LOG.debug("findFile: findMember exception: %s", e.getMessage()); //$NON-NLS-1$
        }

        // Try with Path
        try {
            IFile file = root.getFile(Path.fromPortableString(path));
            LOG.debug("findFile: getFile returned %s, exists=%b", file.getFullPath(), file.exists()); //$NON-NLS-1$
            if (file.exists()) {
                LOG.debug("findFile: found via getFile: %s", file.getFullPath()); //$NON-NLS-1$
                return file;
            }
        } catch (Exception e) {
            LOG.debug("findFile: getFile exception: %s", e.getMessage()); //$NON-NLS-1$
        }

        LOG.warn("findFile: NOT FOUND: %s", path); //$NON-NLS-1$
        return null;
    }

    /**
     * Result of applying changes.
     */
    public record ApplyResult(int successCount, int failCount, List<String> errors) {

        /**
         * Returns whether all changes were applied successfully.
         *
         * @return true if no failures
         */
        public boolean isFullySuccessful() {
            return failCount == 0 && errors.isEmpty();
        }

        /**
         * Returns whether any changes were applied.
         *
         * @return true if at least one success
         */
        public boolean hasSuccesses() {
            return successCount > 0;
        }

        /**
         * Returns the total number of changes processed.
         *
         * @return total count
         */
        public int totalCount() {
            return successCount + failCount;
        }

        /**
         * Returns a summary message.
         *
         * @return human-readable summary
         */
        public String getSummary() {
            if (isFullySuccessful()) {
                return String.format("Применено изменений: %d", successCount); //$NON-NLS-1$
            } else if (successCount > 0) {
                return String.format("Применено: %d, ошибок: %d", successCount, failCount); //$NON-NLS-1$
            } else {
                return String.format("Не удалось применить изменения: %d ошибок", failCount); //$NON-NLS-1$
            }
        }
    }
}
