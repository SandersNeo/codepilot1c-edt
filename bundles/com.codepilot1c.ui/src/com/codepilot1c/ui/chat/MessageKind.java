/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.chat;

/**
 * Kind of chat message for UI display purposes.
 *
 * <p>Separate from {@link com.codepilot1c.core.model.LlmMessage.Role}
 * to support UI-specific message types like system notifications.</p>
 */
public enum MessageKind {

    /**
     * Message from the user.
     */
    USER,

    /**
     * Message from the AI assistant.
     */
    ASSISTANT,

    /**
     * System notification (welcome message, errors, etc.).
     */
    SYSTEM,

    /**
     * Tool call request from assistant (shown in agent mode).
     */
    TOOL_CALL,

    /**
     * Tool execution result.
     */
    TOOL_RESULT;

    /**
     * Returns whether this message kind represents AI output.
     *
     * @return true for ASSISTANT and TOOL_CALL
     */
    public boolean isAssistantOutput() {
        return this == ASSISTANT || this == TOOL_CALL;
    }

    /**
     * Returns whether this message kind is editable by the user.
     *
     * @return true for USER messages
     */
    public boolean isEditable() {
        return this == USER;
    }

    /**
     * Returns the display name for this message kind.
     *
     * @return localized display name
     */
    public String getDisplayName() {
        return switch (this) {
            case USER -> "Вы"; //$NON-NLS-1$
            case ASSISTANT -> "AI"; //$NON-NLS-1$
            case SYSTEM -> "Система"; //$NON-NLS-1$
            case TOOL_CALL -> "Инструмент"; //$NON-NLS-1$
            case TOOL_RESULT -> "Результат"; //$NON-NLS-1$
        };
    }
}
