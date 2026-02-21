package com.codepilot1c.core.edt.dcs;

/**
 * Result for DCS calculated field upsert mutation.
 */
public record DcsUpsertCalculatedFieldResult(
        String projectName,
        String ownerFqn,
        String dataPath,
        boolean created,
        String expression,
        String presentationExpression
) {
}
