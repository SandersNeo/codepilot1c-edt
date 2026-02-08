/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.diff;

import java.util.Objects;

/**
 * Represents a proposed file change from AI tool execution.
 *
 * <p>Used to preview changes before applying them, allowing users to
 * accept or reject individual modifications.</p>
 */
public class ProposedChange {

    private final String filePath;
    private final String beforeContent;
    private final String afterContent;
    private final ChangeKind kind;
    private final String toolCallId;
    private ChangeStatus status;
    private String description;

    /**
     * Kind of proposed change.
     */
    public enum ChangeKind {
        /** Create a new file */
        CREATE,
        /** Modify existing file content */
        MODIFY,
        /** Replace entire file content */
        REPLACE,
        /** Delete a file */
        DELETE
    }

    /**
     * Status of the proposed change.
     */
    public enum ChangeStatus {
        /** Pending user review */
        PENDING,
        /** Accepted by user */
        ACCEPTED,
        /** Rejected by user */
        REJECTED,
        /** Successfully applied */
        APPLIED,
        /** Failed to apply */
        FAILED
    }

    /**
     * Creates a new proposed change.
     *
     * @param filePath the file path (workspace-relative)
     * @param beforeContent the content before change (null for CREATE)
     * @param afterContent the content after change (null for DELETE)
     * @param kind the kind of change
     * @param toolCallId the tool call ID that generated this change
     */
    public ProposedChange(String filePath, String beforeContent, String afterContent,
                          ChangeKind kind, String toolCallId) {
        this.filePath = Objects.requireNonNull(filePath, "filePath must not be null"); //$NON-NLS-1$
        this.beforeContent = beforeContent;
        this.afterContent = afterContent;
        this.kind = Objects.requireNonNull(kind, "kind must not be null"); //$NON-NLS-1$
        this.toolCallId = toolCallId;
        this.status = ChangeStatus.PENDING;
    }

    // === Factory Methods ===

    /**
     * Creates a proposed file creation.
     *
     * @param filePath the file path
     * @param content the new file content
     * @param toolCallId the tool call ID
     * @return the proposed change
     */
    public static ProposedChange create(String filePath, String content, String toolCallId) {
        return new ProposedChange(filePath, null, content, ChangeKind.CREATE, toolCallId);
    }

    /**
     * Creates a proposed file modification (search & replace).
     *
     * @param filePath the file path
     * @param beforeContent the content before change
     * @param afterContent the content after change
     * @param toolCallId the tool call ID
     * @return the proposed change
     */
    public static ProposedChange modify(String filePath, String beforeContent,
                                        String afterContent, String toolCallId) {
        return new ProposedChange(filePath, beforeContent, afterContent, ChangeKind.MODIFY, toolCallId);
    }

    /**
     * Creates a proposed full file replacement.
     *
     * @param filePath the file path
     * @param beforeContent the original content
     * @param afterContent the new content
     * @param toolCallId the tool call ID
     * @return the proposed change
     */
    public static ProposedChange replace(String filePath, String beforeContent,
                                         String afterContent, String toolCallId) {
        return new ProposedChange(filePath, beforeContent, afterContent, ChangeKind.REPLACE, toolCallId);
    }

    // === Getters ===

    public String getFilePath() {
        return filePath;
    }

    public String getBeforeContent() {
        return beforeContent;
    }

    public String getAfterContent() {
        return afterContent;
    }

    public ChangeKind getKind() {
        return kind;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public ChangeStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    // === Setters ===

    public void setStatus(ChangeStatus status) {
        this.status = Objects.requireNonNull(status);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // === Status Methods ===

    /**
     * Marks this change as accepted.
     */
    public void accept() {
        this.status = ChangeStatus.ACCEPTED;
    }

    /**
     * Marks this change as rejected.
     */
    public void reject() {
        this.status = ChangeStatus.REJECTED;
    }

    /**
     * Marks this change as applied.
     */
    public void markApplied() {
        this.status = ChangeStatus.APPLIED;
    }

    /**
     * Marks this change as failed.
     */
    public void markFailed() {
        this.status = ChangeStatus.FAILED;
    }

    /**
     * Returns whether this change is pending review.
     *
     * @return true if pending
     */
    public boolean isPending() {
        return status == ChangeStatus.PENDING;
    }

    /**
     * Returns whether this change was accepted.
     *
     * @return true if accepted
     */
    public boolean isAccepted() {
        return status == ChangeStatus.ACCEPTED;
    }

    /**
     * Returns whether this change was rejected.
     *
     * @return true if rejected
     */
    public boolean isRejected() {
        return status == ChangeStatus.REJECTED;
    }

    // === Utility Methods ===

    /**
     * Returns the file name (without path).
     *
     * @return the file name
     */
    public String getFileName() {
        int lastSep = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSep >= 0 ? filePath.substring(lastSep + 1) : filePath;
    }

    /**
     * Returns a summary of the change for display.
     *
     * @return human-readable summary
     */
    public String getSummary() {
        return switch (kind) {
            case CREATE -> "Создать: " + getFileName(); //$NON-NLS-1$
            case MODIFY -> "Изменить: " + getFileName(); //$NON-NLS-1$
            case REPLACE -> "Заменить: " + getFileName(); //$NON-NLS-1$
            case DELETE -> "Удалить: " + getFileName(); //$NON-NLS-1$
        };
    }

    /**
     * Returns the number of lines changed.
     *
     * @return approximate line count difference
     */
    public int getLinesDelta() {
        int beforeLines = beforeContent != null ? beforeContent.split("\n").length : 0; //$NON-NLS-1$
        int afterLines = afterContent != null ? afterContent.split("\n").length : 0; //$NON-NLS-1$
        return afterLines - beforeLines;
    }

    @Override
    public String toString() {
        return String.format("ProposedChange[%s, %s, %s]", kind, filePath, status); //$NON-NLS-1$
    }
}
