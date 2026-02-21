package com.codepilot1c.core.edt.external;

/**
 * Result of creating external report/processing project.
 */
public record ExternalCreateObjectResult(
        String project,
        String externalProject,
        String objectFqn,
        String kind,
        String objectName,
        String projectPath,
        String version
) {
}
