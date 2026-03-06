package com.codepilot1c.core.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.codepilot1c.core.internal.VibeCorePlugin;
import com.codepilot1c.core.logging.VibeLogger;

/**
 * Collects diagnostics (workspace markers) for EDT projects.
 */
public class DiagnosticsService {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(DiagnosticsService.class);

    public record DiagnosticItem(
            String path,
            String severity,
            int line,
            String message
    ) { }

    public record DiagnosticsSummary(
            String scope,
            String projectName,
            int errorCount,
            int warningCount,
            int infoCount,
            List<DiagnosticItem> items
    ) {
        public List<DiagnosticItem> items() {
            return items == null ? List.of() : Collections.unmodifiableList(items);
        }
    }

    public DiagnosticsSummary collectProjectDiagnostics(String projectName, int maxItems, long waitMs) {
        if (waitMs > 0) {
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = projectName == null ? null : root.getProject(projectName);
        IResource scopeResource = project != null && project.exists() ? project : root;

        int limit = maxItems <= 0 ? Integer.MAX_VALUE : maxItems;
        List<DiagnosticItem> items = new ArrayList<>();
        int errors = 0;
        int warnings = 0;
        int infos = 0;

        try {
            IMarker[] markers = scopeResource.findMarkers(
                    IMarker.PROBLEM,
                    true,
                    IResource.DEPTH_INFINITE);
            for (IMarker marker : markers) {
                int severity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
                String severityName = severityToName(severity);
                switch (severity) {
                    case IMarker.SEVERITY_ERROR -> errors++;
                    case IMarker.SEVERITY_WARNING -> warnings++;
                    default -> infos++;
                }

                if (items.size() < limit) {
                    String path = marker.getResource() != null
                            ? marker.getResource().getFullPath().toString()
                            : ""; //$NON-NLS-1$
                    int line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
                    String message = marker.getAttribute(IMarker.MESSAGE, ""); //$NON-NLS-1$
                    items.add(new DiagnosticItem(path, severityName, line, message));
                }
            }
        } catch (CoreException e) {
            LOG.warn("Diagnostics collection failed: %s", e.getMessage()); //$NON-NLS-1$
            items.add(new DiagnosticItem(
                    "",
                    "error", //$NON-NLS-1$
                    -1,
                    "Diagnostics collection failed: " + e.getMessage())); //$NON-NLS-1$
            errors++;
        }

        String scope = project != null && project.exists() ? "project" : "workspace"; //$NON-NLS-1$ //$NON-NLS-2$
        String effectiveProjectName = project != null && project.exists() ? project.getName() : null;
        if (effectiveProjectName == null && VibeCorePlugin.getDefault() != null) {
            effectiveProjectName = projectName;
        }

        return new DiagnosticsSummary(scope, effectiveProjectName, errors, warnings, infos, items);
    }

    private String severityToName(int severity) {
        return switch (severity) {
            case IMarker.SEVERITY_ERROR -> "error"; //$NON-NLS-1$
            case IMarker.SEVERITY_WARNING -> "warning"; //$NON-NLS-1$
            default -> "info"; //$NON-NLS-1$
        };
    }
}
