package com.codepilot1c.core.edt.imports;

import com.codepilot1c.core.diagnostics.DiagnosticsService.DiagnosticsSummary;

/**
 * Result of importing configuration from infobase into a project.
 */
public record ImportProjectFromInfobaseResult(
        String opId,
        String status,
        String sourceProjectName,
        String targetProjectName,
        String targetProjectPath,
        String platformVersion,
        String exportPath,
        boolean dryRun,
        StandaloneServerImportInfo standalone,
        DiagnosticsSummary diagnostics) {
}
