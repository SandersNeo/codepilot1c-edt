package com.codepilot1c.core.edt.dcs;

import com.codepilot1c.core.edt.metadata.MetadataOperationCode;
import com.codepilot1c.core.edt.metadata.MetadataOperationException;

/**
 * Request for creating/updating query dataset in DCS schema.
 */
public record DcsUpsertQueryDatasetRequest(
        String projectName,
        String ownerFqn,
        String datasetName,
        String query,
        String dataSource,
        Boolean autoFillAvailableFields,
        Boolean useQueryGroupIfPossible
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
        if (datasetName == null || datasetName.isBlank()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "dataset_name is required",
                    false); //$NON-NLS-1$
        }
    }

    public String normalizedProjectName() {
        return projectName == null ? null : projectName.trim();
    }

    public String normalizedOwnerFqn() {
        return ownerFqn == null ? null : ownerFqn.trim();
    }

    public String normalizedDatasetName() {
        return datasetName == null ? null : datasetName.trim();
    }

    public String normalizedQuery() {
        return query == null || query.isBlank() ? null : query.trim();
    }

    public String normalizedDataSource() {
        return dataSource == null || dataSource.isBlank() ? null : dataSource.trim();
    }
}
