package com.codepilot1c.core.workspace;

/**
 * Error raised during workspace project import.
 */
public class WorkspaceImportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final WorkspaceImportErrorCode code;

    public WorkspaceImportException(WorkspaceImportErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public WorkspaceImportException(WorkspaceImportErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public WorkspaceImportErrorCode getCode() {
        return code;
    }
}
