package com.codepilot1c.core.edt.dcs;

/**
 * Generic DCS node projection for agent-level reasoning.
 */
public record DcsNodeItem(
        String nodeKind,
        String name,
        String details
) {
}
