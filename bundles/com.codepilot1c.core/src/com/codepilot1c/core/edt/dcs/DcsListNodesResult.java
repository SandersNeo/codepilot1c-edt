package com.codepilot1c.core.edt.dcs;

import java.util.List;

/**
 * Paged DCS nodes result.
 */
public record DcsListNodesResult(
        String project,
        String ownerFqn,
        String nodeKind,
        int total,
        int count,
        int offset,
        int limit,
        boolean hasMore,
        List<DcsNodeItem> items
) {
}
