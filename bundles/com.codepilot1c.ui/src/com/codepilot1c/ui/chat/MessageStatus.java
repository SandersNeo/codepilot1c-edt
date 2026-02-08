/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.chat;

/**
 * Status of a chat message.
 */
public enum MessageStatus {

    /**
     * Message is being composed or streamed.
     */
    PENDING,

    /**
     * Message is complete and finalized.
     */
    COMPLETE,

    /**
     * Message failed (error during generation or tool execution).
     */
    FAILED,

    /**
     * Message was cancelled by user.
     */
    CANCELLED,

    /**
     * Message was superseded by a regeneration.
     */
    SUPERSEDED;

    /**
     * Returns whether this status represents an in-progress state.
     *
     * @return true if PENDING
     */
    public boolean isInProgress() {
        return this == PENDING;
    }

    /**
     * Returns whether this status represents a terminal state.
     *
     * @return true if COMPLETE, FAILED, CANCELLED, or SUPERSEDED
     */
    public boolean isTerminal() {
        return this != PENDING;
    }

    /**
     * Returns whether this status represents an error state.
     *
     * @return true if FAILED
     */
    public boolean isError() {
        return this == FAILED;
    }
}
