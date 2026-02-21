package com.codepilot1c.core.edt.extension;

import com.codepilot1c.core.edt.metadata.MetadataOperationCode;
import com.codepilot1c.core.edt.metadata.MetadataOperationException;

/**
 * Request for adopting a base configuration object into extension project.
 */
public record ExtensionAdoptObjectRequest(
        String baseProjectName,
        String extensionProjectName,
        String sourceObjectFqn,
        Boolean updateIfExists
) {
    public void validate() {
        if (extensionProjectName == null || extensionProjectName.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.EXTENSION_PROJECT_NOT_FOUND,
                    "extension_project is required", false); //$NON-NLS-1$
        }
        if (sourceObjectFqn == null || sourceObjectFqn.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.METADATA_NOT_FOUND,
                    "source_object_fqn is required", false); //$NON-NLS-1$
        }
    }

    public String normalizedBaseProjectName() {
        if (baseProjectName == null || baseProjectName.isBlank()) {
            return null;
        }
        return baseProjectName.trim();
    }

    public String normalizedExtensionProjectName() {
        return extensionProjectName.trim();
    }

    public String normalizedSourceObjectFqn() {
        return sourceObjectFqn.trim();
    }

    public boolean shouldUpdateIfExists() {
        return updateIfExists != null && updateIfExists.booleanValue();
    }
}
