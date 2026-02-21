package com.codepilot1c.core.edt.extension;

/**
 * Result of adopting metadata object into extension project.
 */
public record ExtensionAdoptObjectResult(
        String extensionProject,
        String baseProject,
        String sourceObjectFqn,
        String adoptedObjectFqn,
        String kind,
        String name,
        boolean alreadyAdopted,
        boolean updated
) {
}
