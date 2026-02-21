package com.codepilot1c.core.edt.dcs;

/**
 * Result for DCS query dataset upsert mutation.
 */
public record DcsUpsertQueryDatasetResult(
        String projectName,
        String ownerFqn,
        String datasetName,
        boolean created,
        String query,
        String dataSource,
        boolean autoFillAvailableFields,
        boolean useQueryGroupIfPossible
) {
}
