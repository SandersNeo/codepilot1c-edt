package com.codepilot1c.core.edt.extension;

/**
 * Request for listing configuration extension projects.
 */
public record ExtensionListProjectsRequest(
        String baseProjectName
) {
    public String normalizedBaseProjectName() {
        if (baseProjectName == null || baseProjectName.isBlank()) {
            return null;
        }
        return baseProjectName.trim();
    }
}

