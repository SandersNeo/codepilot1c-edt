/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.feedback;

import java.util.concurrent.CompletableFuture;

/**
 * Service for sending feedback about AI suggestions to improve model quality.
 *
 * <p>Based on patterns from 1C:Workmate ({@code IFeedbackService.java}).
 * Feedback is used to improve completion quality over time.</p>
 *
 * <p>All methods are asynchronous and fire-and-forget - failures are logged
 * but do not interrupt user workflow.</p>
 */
public interface IFeedbackService {

    /**
     * Reports that a completion was accepted by the user.
     *
     * @param sessionId the completion session ID
     * @param acceptedCode the code that was accepted
     * @param cursorOffsetBefore cursor offset before acceptance
     * @param cursorOffsetAfter cursor offset after acceptance
     * @return a future that completes when feedback is sent
     */
    CompletableFuture<Void> completionAccepted(String sessionId, String acceptedCode,
            int cursorOffsetBefore, int cursorOffsetAfter);

    /**
     * Reports that a completion was dismissed by the user.
     *
     * @param sessionId the completion session ID
     * @param dismissedCode the code that was dismissed
     * @param reason optional reason for dismissal
     * @return a future that completes when feedback is sent
     */
    CompletableFuture<Void> completionDismissed(String sessionId, String dismissedCode, String reason);

    /**
     * Reports that the user finalized accepted code (kept it unchanged).
     *
     * <p>Called after a delay when user moves away from accepted code
     * without modifying it.</p>
     *
     * @param sessionId the completion session ID
     * @param finalCode the final code in the editor
     * @return a future that completes when feedback is sent
     */
    CompletableFuture<Void> codeFinalized(String sessionId, String finalCode);

    /**
     * Reports that the user modified accepted code.
     *
     * @param sessionId the completion session ID
     * @param originalCode the originally accepted code
     * @param modifiedCode the code after user modification
     * @return a future that completes when feedback is sent
     */
    CompletableFuture<Void> codeModified(String sessionId, String originalCode, String modifiedCode);

    /**
     * Reports an issue with an AI suggestion.
     *
     * @param sessionId the session ID (completion or chat)
     * @param issueType the type of issue
     * @param description user description of the issue
     * @param context optional context (code snippet, etc.)
     * @return a future that completes when feedback is sent
     */
    CompletableFuture<Void> reportIssue(String sessionId, IssueType issueType,
            String description, String context);

    /**
     * Sends generic feedback (thumbs up/down).
     *
     * @param sessionId the session ID
     * @param type the feedback type
     * @param metadata optional additional metadata
     * @return a future that completes when feedback is sent
     */
    CompletableFuture<Void> sendFeedback(String sessionId, FeedbackType type, String metadata);

    /**
     * Issue types for reporting problems with AI suggestions.
     */
    enum IssueType {
        /** Code is incorrect or doesn't compile */
        INCORRECT_CODE("incorrect_code"), //$NON-NLS-1$

        /** Suggestion is irrelevant to context */
        IRRELEVANT("irrelevant"), //$NON-NLS-1$

        /** Code has security issues */
        SECURITY_CONCERN("security_concern"), //$NON-NLS-1$

        /** Suggestion is offensive or inappropriate */
        INAPPROPRIATE("inappropriate"), //$NON-NLS-1$

        /** Other issue */
        OTHER("other"); //$NON-NLS-1$

        private final String id;

        IssueType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
