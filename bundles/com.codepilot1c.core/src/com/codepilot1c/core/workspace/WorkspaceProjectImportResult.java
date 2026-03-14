package com.codepilot1c.core.workspace;

/**
 * Structured result of importing a project into the current workspace.
 *
 * @param projectName project name from the Eclipse description
 * @param projectPath imported filesystem path
 * @param created whether the workspace project entry was created
 * @param opened whether the project was opened during the operation
 * @param refreshed whether the project was refreshed after import
 */
public record WorkspaceProjectImportResult(
        String projectName,
        String projectPath,
        boolean created,
        boolean opened,
        boolean refreshed) {
}
