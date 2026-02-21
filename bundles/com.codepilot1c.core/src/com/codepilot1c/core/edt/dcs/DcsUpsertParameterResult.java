package com.codepilot1c.core.edt.dcs;

/**
 * Result for DCS parameter upsert mutation.
 */
public record DcsUpsertParameterResult(
        String projectName,
        String ownerFqn,
        String parameterName,
        boolean created,
        String expression,
        boolean availableAsField,
        boolean valueListAllowed,
        boolean denyIncompleteValues,
        boolean useRestriction
) {
}
