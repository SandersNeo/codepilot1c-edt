package com.codepilot1c.core.edt.dcs;

/**
 * Compact DCS summary for an owner object.
 */
public record DcsSummaryResult(
        String project,
        String ownerFqn,
        String ownerKind,
        boolean schemaPresent,
        String schemaSource,
        int dataSetsCount,
        int parametersCount,
        int calculatedFieldsCount,
        int settingsVariantsCount,
        int dcsTemplatesCount
) {
}
