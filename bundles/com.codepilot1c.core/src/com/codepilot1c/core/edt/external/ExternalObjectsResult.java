package com.codepilot1c.core.edt.external;

import java.util.List;

/**
 * Result of external objects listing.
 */
public record ExternalObjectsResult(
        String project,
        int total,
        int returned,
        int offset,
        int limit,
        boolean hasMore,
        List<ExternalObjectSummary> items
) {
}
