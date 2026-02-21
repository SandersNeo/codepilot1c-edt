package com.codepilot1c.core.edt.extension;

import java.util.List;

/**
 * Result of listing objects in extension project.
 */
public record ExtensionObjectsResult(
        String extensionProject,
        int total,
        int returned,
        int offset,
        int limit,
        boolean hasMore,
        List<ExtensionObjectSummary> items
) {
}

