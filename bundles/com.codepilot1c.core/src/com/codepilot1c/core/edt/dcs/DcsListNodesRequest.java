package com.codepilot1c.core.edt.dcs;

import java.util.Locale;

import com.codepilot1c.core.edt.metadata.MetadataOperationCode;
import com.codepilot1c.core.edt.metadata.MetadataOperationException;

/**
 * Request for paged DCS nodes listing.
 */
public record DcsListNodesRequest(
        String projectName,
        String ownerFqn,
        String nodeKind,
        String nameContains,
        Integer limit,
        Integer offset
) {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;

    public void validate() {
        if (projectName == null || projectName.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "project is required",
                    false); //$NON-NLS-1$
        }
        if (ownerFqn == null || ownerFqn.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "owner_fqn is required",
                    false); //$NON-NLS-1$
        }
        String kind = normalizedNodeKind();
        if (!"all".equals(kind) //$NON-NLS-1$
                && !"dataset".equals(kind) //$NON-NLS-1$
                && !"parameter".equals(kind) //$NON-NLS-1$
                && !"calculated".equals(kind) //$NON-NLS-1$
                && !"variant".equals(kind)) { //$NON-NLS-1$
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "node_kind must be one of: all|dataset|parameter|calculated|variant",
                    false); //$NON-NLS-1$
        }
        if (limit != null && (limit.intValue() <= 0 || limit.intValue() > MAX_LIMIT)) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "limit must be in range 1.." + MAX_LIMIT,
                    false); //$NON-NLS-1$
        }
        if (offset != null && offset.intValue() < 0) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "offset must be >= 0",
                    false); //$NON-NLS-1$
        }
    }

    public String normalizedProjectName() {
        return projectName == null ? null : projectName.trim();
    }

    public String normalizedOwnerFqn() {
        return ownerFqn == null ? null : ownerFqn.trim();
    }

    public String normalizedNodeKind() {
        if (nodeKind == null || nodeKind.isBlank()) {
            return "all"; //$NON-NLS-1$
        }
        return nodeKind.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizedNameContains() {
        if (nameContains == null || nameContains.isBlank()) {
            return null;
        }
        return nameContains.trim().toLowerCase(Locale.ROOT);
    }

    public int effectiveLimit() {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit.intValue(), 1), MAX_LIMIT);
    }

    public int effectiveOffset() {
        if (offset == null) {
            return 0;
        }
        return Math.max(offset.intValue(), 0);
    }
}
