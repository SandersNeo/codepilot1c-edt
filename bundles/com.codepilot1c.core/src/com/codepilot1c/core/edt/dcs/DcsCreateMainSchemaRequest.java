package com.codepilot1c.core.edt.dcs;

import com.codepilot1c.core.edt.metadata.MetadataOperationCode;
import com.codepilot1c.core.edt.metadata.MetadataOperationException;

/**
 * Request to create and bind main DCS schema for owner object.
 */
public record DcsCreateMainSchemaRequest(
        String projectName,
        String ownerFqn,
        String templateName,
        Boolean forceReplace
) {

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
    }

    public String normalizedProjectName() {
        return projectName == null ? null : projectName.trim();
    }

    public String normalizedOwnerFqn() {
        return ownerFqn == null ? null : ownerFqn.trim();
    }

    public String effectiveTemplateName() {
        if (templateName == null || templateName.isBlank()) {
            return "MainDataCompositionSchema"; //$NON-NLS-1$
        }
        return templateName.trim();
    }

    public boolean shouldForceReplace() {
        return Boolean.TRUE.equals(forceReplace);
    }
}
