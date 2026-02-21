package com.codepilot1c.core.edt.external;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import com._1c.g5.v8.dt.platform.version.Version;
import com.codepilot1c.core.edt.metadata.MetadataOperationCode;
import com.codepilot1c.core.edt.metadata.MetadataOperationException;

/**
 * Request for creating an external report project-scoped object.
 */
public record ExternalCreateReportRequest(
        String projectName,
        String externalProjectName,
        String objectName,
        String projectPath,
        String version,
        String synonym,
        String comment
) {
    public void validate() {
        if (projectName == null || projectName.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.PROJECT_NOT_FOUND,
                    "project is required", false); //$NON-NLS-1$
        }
        if (externalProjectName == null || externalProjectName.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.EXTERNAL_OBJECT_PROJECT_NOT_FOUND,
                    "external_project is required", false); //$NON-NLS-1$
        }
        if (normalizedProjectName() != null && normalizedProjectName().equalsIgnoreCase(normalizedExternalProjectName())) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "external_project must differ from project", false); //$NON-NLS-1$
        }
        if (objectName == null || objectName.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.INVALID_METADATA_NAME,
                    "name is required", false); //$NON-NLS-1$
        }
        if (projectPath != null && !projectPath.isBlank()) {
            try {
                Path.of(projectPath.trim());
            } catch (InvalidPathException e) {
                throw new MetadataOperationException(
                        MetadataOperationCode.INVALID_PROPERTY_VALUE,
                        "Invalid project_path: " + e.getMessage(), false); //$NON-NLS-1$
            }
        }
        if (version != null && !version.isBlank()) {
            try {
                Version.create(version.trim());
            } catch (RuntimeException e) {
                throw new MetadataOperationException(
                        MetadataOperationCode.INVALID_PROPERTY_VALUE,
                        "Invalid version: " + version, false); //$NON-NLS-1$
            }
        }
    }

    public String normalizedProjectName() {
        return projectName == null ? null : projectName.trim();
    }

    public String normalizedExternalProjectName() {
        return externalProjectName == null ? null : externalProjectName.trim();
    }

    public String normalizedObjectName() {
        return objectName == null ? null : objectName.trim();
    }

    public Path effectiveProjectPath(Path defaultContainer) {
        if (projectPath == null || projectPath.isBlank()) {
            return defaultContainer.resolve(normalizedExternalProjectName());
        }
        return Path.of(projectPath.trim());
    }

    public Version effectiveVersion(Version fallback) {
        if (version == null || version.isBlank()) {
            return fallback != null ? fallback : Version.LATEST;
        }
        return Version.create(version.trim());
    }

    public String normalizedSynonym() {
        return synonym == null || synonym.isBlank() ? null : synonym.trim();
    }

    public String normalizedComment() {
        return comment == null || comment.isBlank() ? null : comment.trim();
    }
}
