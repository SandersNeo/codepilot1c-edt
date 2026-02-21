package com.codepilot1c.core.edt.dcs;

import com.codepilot1c.core.edt.metadata.MetadataOperationCode;
import com.codepilot1c.core.edt.metadata.MetadataOperationException;

/**
 * Request for creating/updating DCS calculated field.
 */
public record DcsUpsertCalculatedFieldRequest(
        String projectName,
        String ownerFqn,
        String dataPath,
        String expression,
        String presentationExpression
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
        if (dataPath == null || dataPath.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "data_path is required",
                    false); //$NON-NLS-1$
        }
    }

    public String normalizedProjectName() {
        return projectName == null ? null : projectName.trim();
    }

    public String normalizedOwnerFqn() {
        return ownerFqn == null ? null : ownerFqn.trim();
    }

    public String normalizedDataPath() {
        return dataPath == null ? null : dataPath.trim();
    }

    public String normalizedExpression() {
        return expression == null || expression.isBlank() ? null : expression.trim();
    }

    public String normalizedPresentationExpression() {
        return presentationExpression == null || presentationExpression.isBlank() ? null : presentationExpression.trim();
    }
}
