package com.codepilot1c.core.edt.extension;

import java.util.Locale;

import com._1c.g5.v8.dt.metadata.mdclass.extension.type.MdPropertyState;
import com.codepilot1c.core.edt.metadata.MetadataOperationCode;
import com.codepilot1c.core.edt.metadata.MetadataOperationException;

/**
 * Request for setting property state for adopted object in extension.
 */
public record ExtensionSetPropertyStateRequest(
        String extensionProjectName,
        String baseProjectName,
        String sourceObjectFqn,
        String propertyName,
        String state
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
        if (propertyName == null || propertyName.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.EXTENSION_PROPERTY_STATE_INVALID,
                    "property_name is required", false); //$NON-NLS-1$
        }
        parseState();
    }

    public String normalizedExtensionProjectName() {
        return extensionProjectName.trim();
    }

    public String normalizedBaseProjectName() {
        if (baseProjectName == null || baseProjectName.isBlank()) {
            return null;
        }
        return baseProjectName.trim();
    }

    public String normalizedSourceObjectFqn() {
        return sourceObjectFqn.trim();
    }

    public String normalizedPropertyName() {
        return propertyName.trim();
    }

    public MdPropertyState parseState() {
        if (state == null || state.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.EXTENSION_PROPERTY_STATE_INVALID,
                    "state is required", false); //$NON-NLS-1$
        }
        String normalized = state.trim().toUpperCase(Locale.ROOT);
        try {
            return MdPropertyState.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new MetadataOperationException(
                    MetadataOperationCode.EXTENSION_PROPERTY_STATE_INVALID,
                    "Unsupported state: " + state, false); //$NON-NLS-1$
        }
    }
}
