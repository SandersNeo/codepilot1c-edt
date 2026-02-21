package com.codepilot1c.core.edt.extension;

/**
 * Result of extension project creation.
 */
public record ExtensionCreateProjectResult(
        String extensionProject,
        String baseProject,
        String projectPath,
        String version,
        String configurationName,
        String purpose,
        String compatibilityMode
) {
}
