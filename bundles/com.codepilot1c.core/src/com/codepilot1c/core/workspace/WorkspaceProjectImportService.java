package com.codepilot1c.core.workspace;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * Imports externally located Eclipse/EDT projects into the current workspace.
 */
public class WorkspaceProjectImportService {

    public WorkspaceProjectImportResult importProject(Path projectPath) {
        return importProject(projectPath, true, true);
    }

    public WorkspaceProjectImportResult importProject(Path projectPath, boolean openProject, boolean refreshProject) {
        IWorkspace workspace = resolveWorkspace();
        return importProject(workspace, projectPath, openProject, refreshProject);
    }

    public WorkspaceProjectImportResult importProject(IWorkspace workspace, Path projectPath, boolean openProject,
            boolean refreshProject) {
        if (workspace == null) {
            throw new WorkspaceImportException(WorkspaceImportErrorCode.WORKSPACE_UNAVAILABLE,
                    "ResourcesPlugin workspace is unavailable"); //$NON-NLS-1$
        }
        if (projectPath == null) {
            throw new WorkspaceImportException(WorkspaceImportErrorCode.INVALID_ARGUMENT,
                    "projectPath is required"); //$NON-NLS-1$
        }

        Path normalized = projectPath.toAbsolutePath().normalize();
        Path projectFile = normalized.resolve(".project"); //$NON-NLS-1$
        if (!Files.isDirectory(normalized) || !Files.isRegularFile(projectFile)) {
            throw new WorkspaceImportException(WorkspaceImportErrorCode.PROJECT_DESCRIPTION_NOT_FOUND,
                    ".project not found: " + normalized); //$NON-NLS-1$
        }

        try {
            IProjectDescription description = workspace.loadProjectDescription(IPath.fromPath(projectFile));
            if (description == null || description.getName() == null || description.getName().isBlank()) {
                throw new WorkspaceImportException(WorkspaceImportErrorCode.PROJECT_DESCRIPTION_INVALID,
                        "Failed to read project description: " + normalized); //$NON-NLS-1$
            }

            String projectName = description.getName();
            IWorkspaceRoot root = workspace.getRoot();
            IProject project = root.getProject(projectName);
            boolean created = false;
            boolean opened = false;
            boolean refreshed = false;
            IPath locationPath = IPath.fromPath(normalized);
            if (!project.exists()) {
                description.setLocation(locationPath);
                project.create(description, new NullProgressMonitor());
                created = true;
            }
            if (openProject && !project.isOpen()) {
                project.open(new NullProgressMonitor());
                opened = true;
            }
            if (refreshProject) {
                project.refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
                refreshed = true;
            }
            return new WorkspaceProjectImportResult(projectName, normalized.toString(), created, opened, refreshed);
        } catch (WorkspaceImportException e) {
            throw e;
        } catch (CoreException e) {
            throw new WorkspaceImportException(WorkspaceImportErrorCode.IMPORT_FAILED,
                    "Failed to import workspace project from " + normalized + ": " + e.getMessage(), e); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    protected IWorkspace resolveWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }
}
