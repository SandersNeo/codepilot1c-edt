/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.chat;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a part of a chat message content.
 *
 * <p>A message can contain multiple parts of different types:</p>
 * <ul>
 *   <li>{@link TextPart} - Plain or Markdown text</li>
 *   <li>{@link CodeBlockPart} - Code block with language</li>
 *   <li>{@link ToolCallPart} - Tool invocation request</li>
 *   <li>{@link ToolResultPart} - Tool execution result</li>
 *   <li>{@link TodoListPart} - Task list with checkboxes</li>
 * </ul>
 */
public sealed interface MessagePart permits
        MessagePart.TextPart,
        MessagePart.CodeBlockPart,
        MessagePart.ToolCallPart,
        MessagePart.ToolResultPart,
        MessagePart.TodoListPart {

    /**
     * Returns the type of this message part.
     *
     * @return the part type
     */
    PartType getType();

    /**
     * Type of message part.
     */
    enum PartType {
        TEXT,
        CODE_BLOCK,
        TOOL_CALL,
        TOOL_RESULT,
        TODO_LIST
    }

    /**
     * Plain text or Markdown content.
     */
    record TextPart(String content) implements MessagePart {
        public TextPart {
            Objects.requireNonNull(content, "content must not be null"); //$NON-NLS-1$
        }

        @Override
        public PartType getType() {
            return PartType.TEXT;
        }
    }

    /**
     * Code block with optional language.
     */
    record CodeBlockPart(String code, String language) implements MessagePart {
        public CodeBlockPart {
            Objects.requireNonNull(code, "code must not be null"); //$NON-NLS-1$
        }

        public CodeBlockPart(String code) {
            this(code, null);
        }

        @Override
        public PartType getType() {
            return PartType.CODE_BLOCK;
        }

        /**
         * Returns whether a language is specified.
         *
         * @return true if language is not null or empty
         */
        public boolean hasLanguage() {
            return language != null && !language.isEmpty();
        }
    }

    /**
     * Tool call request from the assistant.
     */
    record ToolCallPart(
            String toolCallId,
            String toolName,
            Map<String, Object> arguments,
            ToolCallStatus status
    ) implements MessagePart {

        public ToolCallPart {
            Objects.requireNonNull(toolCallId, "toolCallId must not be null"); //$NON-NLS-1$
            Objects.requireNonNull(toolName, "toolName must not be null"); //$NON-NLS-1$
        }

        @Override
        public PartType getType() {
            return PartType.TOOL_CALL;
        }

        /**
         * Status of tool call execution.
         */
        public enum ToolCallStatus {
            PENDING,
            RUNNING,
            SUCCESS,
            FAILED,
            CANCELLED,
            NEEDS_CONFIRMATION
        }
    }

    /**
     * Result of tool execution.
     */
    record ToolResultPart(
            String toolCallId,
            String toolName,
            String content,
            boolean success,
            ResultType resultType
    ) implements MessagePart {

        public ToolResultPart {
            Objects.requireNonNull(toolCallId, "toolCallId must not be null"); //$NON-NLS-1$
            Objects.requireNonNull(toolName, "toolName must not be null"); //$NON-NLS-1$
        }

        @Override
        public PartType getType() {
            return PartType.TOOL_RESULT;
        }

        /**
         * Type of tool result for specialized rendering.
         */
        public enum ResultType {
            TEXT,
            CODE,
            FILE_LIST,
            SEARCH_RESULTS,
            CONFIRMATION
        }
    }

    /**
     * Todo/task list with checkable items.
     */
    record TodoListPart(java.util.List<TodoItem> items) implements MessagePart {
        public TodoListPart {
            Objects.requireNonNull(items, "items must not be null"); //$NON-NLS-1$
        }

        @Override
        public PartType getType() {
            return PartType.TODO_LIST;
        }

        /**
         * Single todo item.
         */
        public record TodoItem(String text, boolean checked) {
            public TodoItem {
                Objects.requireNonNull(text, "text must not be null"); //$NON-NLS-1$
            }
        }

        /**
         * Returns the number of completed items.
         *
         * @return count of checked items
         */
        public int completedCount() {
            return (int) items.stream().filter(TodoItem::checked).count();
        }

        /**
         * Returns the completion percentage.
         *
         * @return 0-100 percentage
         */
        public int completionPercent() {
            if (items.isEmpty()) {
                return 0;
            }
            return (completedCount() * 100) / items.size();
        }
    }
}
