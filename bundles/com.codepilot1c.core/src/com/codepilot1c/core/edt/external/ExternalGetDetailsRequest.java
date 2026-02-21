package com.codepilot1c.core.edt.external;

import com.codepilot1c.core.edt.metadata.MetadataOperationCode;
import com.codepilot1c.core.edt.metadata.MetadataOperationException;

/**
 * Request for external object details.
 */
public record ExternalGetDetailsRequest(
        String projectName,
        String objectFqn,
        String externalProjectName
) {
    public void validate() {
        if (projectName == null || projectName.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.PROJECT_NOT_FOUND,
                    "project is required", false); //$NON-NLS-1$
        }
        if (objectFqn == null || objectFqn.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.EXTERNAL_OBJECT_NOT_FOUND,
                    "object_fqn is required", false); //$NON-NLS-1$
        }
    }

    public String normalizedProjectName() {
        return projectName == null ? null : projectName.trim();
    }

    public String normalizedObjectFqn() {
        return objectFqn == null ? null : objectFqn.trim();
    }

    public String normalizedExternalProjectName() {
        if (externalProjectName == null || externalProjectName.isBlank()) {
            return null;
        }
        return externalProjectName.trim();
    }
}
