package com.codepilot1c.core.edt.external;

/**
 * Summary of external-object EDT project.
 */
public record ExternalProjectSummary(
        String baseProject,
        String externalProject,
        String path,
        String version
) {
}
