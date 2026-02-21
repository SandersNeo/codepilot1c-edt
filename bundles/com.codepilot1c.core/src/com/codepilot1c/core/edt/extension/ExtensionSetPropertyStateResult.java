package com.codepilot1c.core.edt.extension;

/**
 * Result of setting property state for adopted extension object.
 */
public record ExtensionSetPropertyStateResult(
        String extensionProject,
        String baseProject,
        String sourceObjectFqn,
        String adoptedObjectFqn,
        String propertyName,
        String state
) {
}
