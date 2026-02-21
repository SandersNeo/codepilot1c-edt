package com.codepilot1c.core.edt.external;

import java.util.List;

/**
 * Result of external-object projects listing.
 */
public record ExternalProjectsResult(
        String baseProject,
        int total,
        int count,
        int offset,
        int limit,
        boolean hasMore,
        List<ExternalProjectSummary> items
) {
}
