package com.codepilot1c.core.edt.extension;

/**
 * Summary of extension project.
 */
public record ExtensionProjectSummary(
        String extensionProject,
        String baseProject,
        String configurationName,
        String purpose,
        String compatibilityMode
) {
}

