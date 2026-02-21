package com.codepilot1c.core.edt.extension;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;

import com._1c.g5.v8.dt.metadata.mdclass.CompatibilityMode;
import com._1c.g5.v8.dt.metadata.mdclass.ConfigurationExtensionPurpose;
import com._1c.g5.v8.dt.platform.version.Version;
import com.codepilot1c.core.edt.metadata.MetadataOperationCode;
import com.codepilot1c.core.edt.metadata.MetadataOperationException;

/**
 * Request for creating a new EDT extension project.
 */
public record ExtensionCreateProjectRequest(
        String baseProjectName,
        String extensionProjectName,
        String projectPath,
        String version,
        String configurationName,
        String purpose,
        String compatibilityMode
) {
    public void validate() {
        if (baseProjectName == null || baseProjectName.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.PROJECT_NOT_FOUND,
                    "base_project is required", false); //$NON-NLS-1$
        }
        if (extensionProjectName == null || extensionProjectName.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.EXTENSION_PROJECT_NOT_FOUND,
                    "extension_project is required", false); //$NON-NLS-1$
        }
        if (normalizedBaseProjectName().equalsIgnoreCase(normalizedExtensionProjectName())) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "extension_project must differ from base_project", false); //$NON-NLS-1$
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
        effectivePurpose();
        effectiveCompatibilityMode();
    }

    public String normalizedBaseProjectName() {
        return baseProjectName == null ? null : baseProjectName.trim();
    }

    public String normalizedExtensionProjectName() {
        return extensionProjectName == null ? null : extensionProjectName.trim();
    }

    public String effectiveConfigurationName() {
        if (configurationName == null || configurationName.isBlank()) {
            return normalizedExtensionProjectName();
        }
        return configurationName.trim();
    }

    public Path effectiveProjectPath(Path workspaceRoot) {
        if (projectPath == null || projectPath.isBlank()) {
            return workspaceRoot.resolve(normalizedExtensionProjectName());
        }
        return Path.of(projectPath.trim());
    }

    public Version effectiveVersion(Version fallback) {
        if (version == null || version.isBlank()) {
            return fallback != null ? fallback : Version.LATEST;
        }
        return Version.create(version.trim());
    }

    public ConfigurationExtensionPurpose effectivePurpose() {
        if (purpose == null || purpose.isBlank()) {
            return ConfigurationExtensionPurpose.ADD_ON;
        }
        String normalized = purpose.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if ("ADDON".equals(normalized)) { //$NON-NLS-1$
            normalized = "ADD_ON"; //$NON-NLS-1$
        }
        try {
            return ConfigurationExtensionPurpose.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new MetadataOperationException(
                    MetadataOperationCode.INVALID_PROPERTY_VALUE,
                    "Unsupported purpose: " + purpose, false); //$NON-NLS-1$
        }
    }

    public CompatibilityMode effectiveCompatibilityMode() {
        if (compatibilityMode == null || compatibilityMode.isBlank()) {
            return null;
        }
        String normalized = normalizeCompatibilityModeToken(compatibilityMode);
        try {
            return CompatibilityMode.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new MetadataOperationException(
                    MetadataOperationCode.INVALID_PROPERTY_VALUE,
                    "Unsupported compatibility_mode: " + compatibilityMode, false); //$NON-NLS-1$
        }
    }

    private String normalizeCompatibilityModeToken(String raw) {
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (normalized.startsWith("VERSION")) { //$NON-NLS-1$
            normalized = normalized.substring("VERSION".length()); //$NON-NLS-1$
        }
        if (normalized.matches("^\\d+\\.\\d+\\.\\d+$")) { //$NON-NLS-1$
            String[] parts = normalized.split("\\."); //$NON-NLS-1$
            return "VERSION" + parts[0] + "_" + parts[1] + parts[2]; //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (normalized.matches("^\\d+_\\d+_\\d+$")) { //$NON-NLS-1$
            String[] parts = normalized.split("_"); //$NON-NLS-1$
            return "VERSION" + parts[0] + "_" + parts[1] + parts[2]; //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (normalized.matches("^\\d+_\\d+$")) { //$NON-NLS-1$
            return "VERSION" + normalized; //$NON-NLS-1$
        }
        return normalized.startsWith("VERSION") ? normalized : "VERSION" + normalized; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
