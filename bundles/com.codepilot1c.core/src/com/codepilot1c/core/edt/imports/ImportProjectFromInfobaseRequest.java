package com.codepilot1c.core.edt.imports;

import java.nio.file.Path;

import com.codepilot1c.core.edt.runtime.EdtToolErrorCode;
import com.codepilot1c.core.edt.runtime.EdtToolException;

/**
 * Request for importing configuration from an associated infobase into a new EDT project.
 */
public record ImportProjectFromInfobaseRequest(
        String sourceProjectName,
        String targetProjectName,
        String projectPath,
        String version,
        String baseProjectName,
        boolean startServer,
        Integer clusterPort,
        String clusterRegistryDirectory,
        String publicationPath,
        boolean dryRun,
        int diagnosticsWaitMs,
        int diagnosticsMaxItems) {

    private static final int DEFAULT_CLUSTER_PORT = 1541;
    private static final int DEFAULT_DIAGNOSTICS_WAIT_MS = 1500;
    private static final int DEFAULT_DIAGNOSTICS_MAX_ITEMS = 100;

    public void validate() {
        String source = normalizedSourceProjectName();
        if (source == null) {
            throw new EdtToolException(EdtToolErrorCode.INVALID_ARGUMENT, "source_project_name is required"); //$NON-NLS-1$
        }
        String target = normalizedTargetProjectName();
        if (target == null) {
            throw new EdtToolException(EdtToolErrorCode.INVALID_ARGUMENT, "target_project_name is required"); //$NON-NLS-1$
        }
        Path location = resolveProjectLocation();
        if (location != null) {
            Path fileName = location.getFileName();
            if (fileName == null || !target.equals(fileName.toString().trim())) {
                throw new EdtToolException(EdtToolErrorCode.INVALID_ARGUMENT,
                        "project_path must end with target_project_name"); //$NON-NLS-1$
            }
        }
    }

    public String normalizedSourceProjectName() {
        return normalize(sourceProjectName);
    }

    public String normalizedTargetProjectName() {
        return normalize(targetProjectName);
    }

    public String normalizedVersion() {
        return normalize(version);
    }

    public String normalizedBaseProjectName() {
        return normalize(baseProjectName);
    }

    public Path resolveProjectLocation() {
        String normalized = normalize(projectPath);
        return normalized == null ? null : Path.of(normalized).toAbsolutePath().normalize();
    }

    public String effectiveVersion(String fallbackVersion) {
        String explicit = normalizedVersion();
        if (explicit != null) {
            return explicit;
        }
        if (fallbackVersion != null && !fallbackVersion.isBlank()) {
            return fallbackVersion;
        }
        return ""; //$NON-NLS-1$
    }

    public int effectiveClusterPort() {
        return clusterPort == null || clusterPort.intValue() <= 0 ? DEFAULT_CLUSTER_PORT : clusterPort.intValue();
    }

    public int effectiveDiagnosticsWaitMs() {
        return diagnosticsWaitMs <= 0 ? DEFAULT_DIAGNOSTICS_WAIT_MS : diagnosticsWaitMs;
    }

    public int effectiveDiagnosticsMaxItems() {
        return diagnosticsMaxItems <= 0 ? DEFAULT_DIAGNOSTICS_MAX_ITEMS : diagnosticsMaxItems;
    }

    public String effectiveClusterRegistryDirectory(Path operationRoot) {
        String explicit = normalize(clusterRegistryDirectory);
        return explicit != null ? explicit : operationRoot.resolve("cluster-registry").toString(); //$NON-NLS-1$
    }

    public String effectivePublicationPath(Path operationRoot) {
        String explicit = normalize(publicationPath);
        return explicit != null ? explicit : operationRoot.resolve("publication").toString(); //$NON-NLS-1$
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
