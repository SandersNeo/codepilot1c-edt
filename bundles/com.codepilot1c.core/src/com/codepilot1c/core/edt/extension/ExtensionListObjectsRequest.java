package com.codepilot1c.core.edt.extension;

import java.util.Locale;

import com.codepilot1c.core.edt.metadata.MetadataOperationCode;
import com.codepilot1c.core.edt.metadata.MetadataOperationException;

/**
 * Request for listing objects inside extension project configuration.
 */
public record ExtensionListObjectsRequest(
        String extensionProjectName,
        String typeFilter,
        String nameContains,
        Integer limit,
        Integer offset
) {
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;

    public ExtensionListObjectsRequest {
        if (extensionProjectName == null || extensionProjectName.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.EXTENSION_PROJECT_NOT_FOUND,
                    "extension_project is required", false); //$NON-NLS-1$
        }
    }

    public String normalizedExtensionProjectName() {
        return extensionProjectName.trim();
    }

    public String normalizedTypeFilter() {
        if (typeFilter == null || typeFilter.isBlank()) {
            return null;
        }
        return typeFilter.trim().toLowerCase(Locale.ROOT);
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
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "limit must be between 1 and " + MAX_LIMIT, false); //$NON-NLS-1$
        }
        return limit.intValue();
    }

    public int effectiveOffset() {
        if (offset == null) {
            return 0;
        }
        if (offset < 0) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "offset must be >= 0", false); //$NON-NLS-1$
        }
        return offset.intValue();
    }
}

