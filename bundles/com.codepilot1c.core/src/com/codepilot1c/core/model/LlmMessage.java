/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a message in a conversation with an LLM.
 */
public class LlmMessage {

    /**
     * Role of the message sender.
     */
    public enum Role {
        SYSTEM("system"), //$NON-NLS-1$
        USER("user"), //$NON-NLS-1$
        ASSISTANT("assistant"), //$NON-NLS-1$
        TOOL("tool"); //$NON-NLS-1$

        private final String value;

        Role(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final Role role;
    private final String content;
    private final List<ToolCall> toolCalls;
    private final String toolCallId;

    /**
     * Creates a new message.
     *
     * @param role    the role of the sender
     * @param content the message content
     */
    public LlmMessage(Role role, String content) {
        this(role, content, null, null);
    }

    /**
     * Creates a new message with tool calls.
     *
     * @param role      the role of the sender
     * @param content   the message content
     * @param toolCalls the tool calls (for assistant messages)
     * @param toolCallId the tool call ID (for tool result messages)
     */
    public LlmMessage(Role role, String content, List<ToolCall> toolCalls, String toolCallId) {
        this.role = Objects.requireNonNull(role, "role must not be null"); //$NON-NLS-1$
        this.content = content != null ? content : ""; //$NON-NLS-1$
        this.toolCalls = toolCalls != null ? Collections.unmodifiableList(toolCalls) : Collections.emptyList();
        this.toolCallId = toolCallId;
    }

    /**
     * Creates a system message.
     *
     * @param content the message content
     * @return a new system message
     */
    public static LlmMessage system(String content) {
        return new LlmMessage(Role.SYSTEM, content);
    }

    /**
     * Creates a user message.
     *
     * @param content the message content
     * @return a new user message
     */
    public static LlmMessage user(String content) {
        return new LlmMessage(Role.USER, content);
    }

    /**
     * Creates an assistant message.
     *
     * @param content the message content
     * @return a new assistant message
     */
    public static LlmMessage assistant(String content) {
        return new LlmMessage(Role.ASSISTANT, content);
    }

    /**
     * Creates an assistant message with tool calls.
     *
     * @param content   the message content (may be null)
     * @param toolCalls the tool calls requested by the assistant
     * @return a new assistant message
     */
    public static LlmMessage assistantWithToolCalls(String content, List<ToolCall> toolCalls) {
        return new LlmMessage(Role.ASSISTANT, content, toolCalls, null);
    }

    /**
     * Creates a tool result message.
     *
     * @param toolCallId the ID of the tool call this is a response to
     * @param content    the result content
     * @return a new tool message
     */
    public static LlmMessage toolResult(String toolCallId, String content) {
        return new LlmMessage(Role.TOOL, content, null, toolCallId);
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    /**
     * Returns the tool calls for this message.
     *
     * @return the tool calls, empty if none
     */
    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    /**
     * Returns whether this message contains tool calls.
     *
     * @return true if there are tool calls
     */
    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    /**
     * Returns the tool call ID (for tool result messages).
     *
     * @return the tool call ID, or null
     */
    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * Returns whether this is a tool result message.
     *
     * @return true if this is a tool result
     */
    public boolean isToolResult() {
        return role == Role.TOOL && toolCallId != null;
    }

    @Override
    public String toString() {
        return String.format("LlmMessage[role=%s, content=%s]", role, //$NON-NLS-1$
                content.length() > 50 ? content.substring(0, 50) + "..." : content); //$NON-NLS-1$
    }
}
