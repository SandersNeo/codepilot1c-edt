package com.codepilot1c.core.git;

import java.util.Locale;

/**
 * Allowlisted Git operations exposed through tools.
 */
public enum GitOperation {
    STATUS(false),
    BRANCH_LIST(false),
    REMOTE_LIST(false),
    LOG(false),
    DIFF_SUMMARY(false),
    INIT(true),
    CLONE(true),
    REMOTE_ADD(true),
    REMOTE_SET_URL(true),
    FETCH(true),
    PULL(true),
    PUSH(true),
    CHECKOUT(true),
    CREATE_BRANCH(true),
    ADD(true),
    COMMIT(true);

    private final boolean mutating;

    GitOperation(boolean mutating) {
        this.mutating = mutating;
    }

    public boolean isMutating() {
        return mutating;
    }

    public static GitOperation from(String value) {
        if (value == null || value.isBlank()) {
            throw new GitToolException(GitErrorCode.INVALID_ARGUMENT, "operation is required"); //$NON-NLS-1$
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if ("CREATE".equals(normalized) || "CREATE_REPO".equals(normalized)) { //$NON-NLS-1$ //$NON-NLS-2$
            normalized = "INIT"; //$NON-NLS-1$
        }
        try {
            return GitOperation.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new GitToolException(GitErrorCode.INVALID_ARGUMENT, "Unsupported git operation: " + value, e); //$NON-NLS-1$
        }
    }
}
