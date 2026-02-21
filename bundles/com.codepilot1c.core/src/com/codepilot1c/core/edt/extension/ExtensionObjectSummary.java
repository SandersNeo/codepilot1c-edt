package com.codepilot1c.core.edt.extension;

/**
 * Summary of metadata object located in extension configuration.
 */
public record ExtensionObjectSummary(
        String fqn,
        String name,
        String kind,
        String objectBelonging,
        String synonym,
        String extendedConfigurationObjectUuid,
        boolean hasExtensionData
) {
}

