package com.codepilot1c.core.edt.external;

/**
 * Summary of external object in external-object project.
 */
public record ExternalObjectSummary(
        String project,
        String externalProject,
        String fqn,
        String name,
        String kind,
        String synonym,
        String objectBelonging
) {
}
