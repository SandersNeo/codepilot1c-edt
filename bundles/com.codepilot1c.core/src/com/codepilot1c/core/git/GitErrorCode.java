package com.codepilot1c.core.git;

/**
 * Stable error codes for structured Git operations.
 */
public enum GitErrorCode {
    INVALID_ARGUMENT,
    GIT_EXECUTABLE_NOT_FOUND,
    REPOSITORY_NOT_FOUND,
    NOT_A_GIT_REPOSITORY,
    REMOTE_ALREADY_EXISTS,
    AUTH_FAILED,
    COMMAND_FAILED,
    TIMEOUT
}
