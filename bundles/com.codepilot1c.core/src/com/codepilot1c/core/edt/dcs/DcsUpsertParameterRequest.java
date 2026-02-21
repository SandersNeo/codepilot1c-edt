package com.codepilot1c.core.edt.dcs;

import com.codepilot1c.core.edt.metadata.MetadataOperationCode;
import com.codepilot1c.core.edt.metadata.MetadataOperationException;

/**
 * Request for creating/updating DCS parameter.
 */
public record DcsUpsertParameterRequest(
        String projectName,
        String ownerFqn,
        String parameterName,
        String expression,
        Boolean availableAsField,
        Boolean valueListAllowed,
        Boolean denyIncompleteValues,
        Boolean useRestriction
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
        if (parameterName == null || parameterName.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "parameter_name is required",
                    false); //$NON-NLS-1$
        }
    }

    public String normalizedProjectName() {
        return projectName == null ? null : projectName.trim();
    }

    public String normalizedOwnerFqn() {
        return ownerFqn == null ? null : ownerFqn.trim();
    }

    public String normalizedParameterName() {
        return parameterName == null ? null : parameterName.trim();
    }

    public String normalizedExpression() {
        return expression == null || expression.isBlank() ? null : expression.trim();
    }
}
