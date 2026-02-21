package com.codepilot1c.core.edt.external;

import java.util.Locale;

import com.codepilot1c.core.edt.metadata.MetadataOperationCode;
import com.codepilot1c.core.edt.metadata.MetadataOperationException;

/**
 * Request for listing external objects in project scope.
 */
public record ExternalListObjectsRequest(
        String projectName,
        String externalProjectName,
        String typeFilter,
        String nameContains,
        Integer limit,
        Integer offset
) {
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;

    public void validate() {
        if (projectName == null || projectName.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.PROJECT_NOT_FOUND,
                    "project is required", false); //$NON-NLS-1$
        }
        if (limit != null && (limit.intValue() < 1 || limit.intValue() > MAX_LIMIT)) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "limit must be between 1 and " + MAX_LIMIT, false); //$NON-NLS-1$
        }
        if (offset != null && offset.intValue() < 0) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "offset must be >= 0", false); //$NON-NLS-1$
        }
    }

    public String normalizedProjectName() {
        return projectName == null ? null : projectName.trim();
    }

    public String normalizedExternalProjectName() {
        if (externalProjectName == null || externalProjectName.isBlank()) {
            return null;
        }
        return externalProjectName.trim();
    }

    public String normalizedTypeFilter() {
        if (typeFilter == null || typeFilter.isBlank()) {
            return null;
        }
        return normalizeToken(typeFilter);
    }

    public String normalizedNameContains() {
        if (nameContains == null || nameContains.isBlank()) {
            return null;
        }
        return nameContains.trim().toLowerCase(Locale.ROOT);
    }

    public int effectiveLimit() {
        return limit == null ? DEFAULT_LIMIT : limit.intValue();
    }

    public int effectiveOffset() {
        return offset == null ? 0 : offset.intValue();
    }

    private String normalizeToken(String value) {
        String lowered = value.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
        StringBuilder sb = new StringBuilder(lowered.length());
        for (int i = 0; i < lowered.length(); i++) {
            char ch = lowered.charAt(i);
            if (ch == '_' || ch == '-' || ch == '.' || Character.isWhitespace(ch)) {
                continue;
            }
            sb.append(ch);
        }
        return sb.toString();
    }
}
