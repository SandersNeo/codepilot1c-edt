package com.codepilot1c.core.git;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds allowlisted Git command lines without exposing arbitrary shell access.
 */
public final class GitCommandBuilder {

    private final List<String> args = new ArrayList<>();

    private GitCommandBuilder() {
        args.add("git"); //$NON-NLS-1$
    }

    public static GitCommandBuilder create() {
        return new GitCommandBuilder();
    }

    public GitCommandBuilder arg(String value) {
        if (value != null && !value.isBlank()) {
            args.add(value);
        }
        return this;
    }

    public GitCommandBuilder args(String... values) {
        if (values == null) {
            return this;
        }
        for (String value : values) {
            arg(value);
        }
        return this;
    }

    public List<String> build() {
        return Collections.unmodifiableList(new ArrayList<>(args));
    }
}
