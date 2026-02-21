package com.codepilot1c.core.edt.extension;

import java.util.List;

/**
 * Result of listing extension projects.
 */
public record ExtensionProjectsResult(
        int total,
        List<ExtensionProjectSummary> items
) {
}

