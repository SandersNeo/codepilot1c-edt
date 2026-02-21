package com.codepilot1c.core.edt.dcs;

/**
 * Result of main DCS schema create/bind mutation.
 */
public record DcsCreateMainSchemaResult(
        String projectName,
        String ownerFqn,
        String ownerKind,
        String templateName,
        boolean schemaCreated,
        boolean templateCreated,
        boolean mainBindingUpdated,
        String schemaSource
) {
}
