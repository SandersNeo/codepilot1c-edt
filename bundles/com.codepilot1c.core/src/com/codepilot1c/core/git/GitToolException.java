package com.codepilot1c.core.git;

/**
 * Error raised during a structured Git operation.
 */
public class GitToolException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final GitErrorCode code;

    public GitToolException(GitErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public GitToolException(GitErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public GitErrorCode getCode() {
        return code;
    }
}
