/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.feedback;

/**
 * Types of feedback that can be sent to the AI service.
 *
 * <p>Based on patterns from 1C:Workmate feedback system.</p>
 */
public enum FeedbackType {

    /**
     * User accepted the completion suggestion (Tab key).
     */
    COMPLETION_ACCEPTED("completion_accepted"), //$NON-NLS-1$

    /**
     * User dismissed the completion suggestion (Escape key).
     */
    COMPLETION_DISMISSED("completion_dismissed"), //$NON-NLS-1$

    /**
     * User partially accepted the completion (word/line accept).
     */
    COMPLETION_PARTIAL("completion_partial"), //$NON-NLS-1$

    /**
     * User modified the accepted code afterward.
     */
    CODE_MODIFIED("code_modified"), //$NON-NLS-1$

    /**
     * User kept the accepted code unchanged (finalized).
     */
    CODE_FINALIZED("code_finalized"), //$NON-NLS-1$

    /**
     * User reported an issue with the suggestion.
     */
    ISSUE_REPORTED("issue_reported"), //$NON-NLS-1$

    /**
     * Positive feedback (thumbs up).
     */
    THUMBS_UP("thumbs_up"), //$NON-NLS-1$

    /**
     * Negative feedback (thumbs down).
     */
    THUMBS_DOWN("thumbs_down"); //$NON-NLS-1$

    private final String id;

    FeedbackType(String id) {
        this.id = id;
    }

    /**
     * Returns the feedback type identifier for API calls.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Finds a feedback type by its ID.
     *
     * @param id the id
     * @return the type, or null if not found
     */
    public static FeedbackType fromId(String id) {
        if (id == null) {
            return null;
        }
        for (FeedbackType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
