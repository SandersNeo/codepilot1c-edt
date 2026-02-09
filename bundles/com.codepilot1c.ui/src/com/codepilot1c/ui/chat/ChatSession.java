/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.codepilot1c.core.model.LlmMessage;

/**
 * Manages a chat conversation session.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Message history management</li>
 *   <li>Message editing and regeneration</li>
 *   <li>Conversion to/from LlmMessage for API calls</li>
 *   <li>Change notification via listeners</li>
 * </ul>
 */
public class ChatSession {

    private final String sessionId;
    private final List<ChatMessage> messages;
    private final List<SessionListener> listeners;

    // Current agent run state
    private AgentRunState agentState;

    /**
     * Creates a new chat session.
     */
    public ChatSession() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Creates a new chat session with the specified ID.
     *
     * @param sessionId unique session identifier
     */
    public ChatSession(String sessionId) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.messages = new ArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.agentState = null;
    }

    // === Session Info ===

    public String getSessionId() {
        return sessionId;
    }

    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public int getMessageCount() {
        return messages.size();
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    // === Message Management ===

    /**
     * Adds a message to the session.
     *
     * @param message the message to add
     */
    public void addMessage(ChatMessage message) {
        Objects.requireNonNull(message, "message must not be null"); //$NON-NLS-1$
        messages.add(message);
        fireMessageAdded(message);
    }

    /**
     * Creates and adds a user message.
     *
     * @param content the message content
     * @return the created message
     */
    public ChatMessage addUserMessage(String content) {
        ChatMessage message = ChatMessage.user(content);
        addMessage(message);
        return message;
    }

    /**
     * Creates and adds an assistant message.
     *
     * @param content the message content
     * @return the created message
     */
    public ChatMessage addAssistantMessage(String content) {
        ChatMessage message = ChatMessage.assistant(content);
        addMessage(message);
        return message;
    }

    /**
     * Creates and adds a pending assistant message for streaming.
     *
     * @return the created pending message
     */
    public ChatMessage addPendingAssistantMessage() {
        ChatMessage message = ChatMessage.pendingAssistant();
        addMessage(message);
        return message;
    }

    /**
     * Creates and adds a system message.
     *
     * @param content the message content
     * @return the created message
     */
    public ChatMessage addSystemMessage(String content) {
        ChatMessage message = ChatMessage.system(content);
        addMessage(message);
        return message;
    }

    /**
     * Finds a message by ID.
     *
     * @param messageId the message ID
     * @return the message, or empty if not found
     */
    public Optional<ChatMessage> findMessage(String messageId) {
        return messages.stream()
                .filter(m -> m.getId().equals(messageId))
                .findFirst();
    }

    /**
     * Returns the last message of the specified kind.
     *
     * @param kind the message kind
     * @return the last message, or empty if none
     */
    public Optional<ChatMessage> getLastMessage(MessageKind kind) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message.getKind() == kind && !message.isSuperseded()) {
                return Optional.of(message);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the last assistant message.
     *
     * @return the last assistant message, or empty
     */
    public Optional<ChatMessage> getLastAssistantMessage() {
        return getLastMessage(MessageKind.ASSISTANT);
    }

    /**
     * Returns the last user message.
     *
     * @return the last user message, or empty
     */
    public Optional<ChatMessage> getLastUserMessage() {
        return getLastMessage(MessageKind.USER);
    }

    // === Message Editing ===

    /**
     * Edits a user message and truncates the conversation after it.
     *
     * <p>This operation:</p>
     * <ol>
     *   <li>Updates the message content</li>
     *   <li>Removes all messages after the edited one</li>
     * </ol>
     *
     * @param messageId the ID of the message to edit
     * @param newContent the new content
     * @return true if the message was found and edited
     */
    public boolean editMessage(String messageId, String newContent) {
        int index = -1;
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getId().equals(messageId)) {
                index = i;
                break;
            }
        }

        if (index < 0) {
            return false;
        }

        ChatMessage message = messages.get(index);
        if (!message.getKind().isEditable()) {
            return false;
        }

        // Remove messages after the edited one
        List<ChatMessage> removed = new ArrayList<>();
        while (messages.size() > index + 1) {
            removed.add(messages.remove(messages.size() - 1));
        }

        // Create a new message with updated content (immutable style)
        ChatMessage newMessage = ChatMessage.user(newContent);
        messages.set(index, newMessage);

        fireMessageEdited(message, newMessage, removed);
        return true;
    }

    /**
     * Regenerates the last assistant response.
     *
     * <p>This operation:</p>
     * <ol>
     *   <li>Marks the last assistant message as superseded</li>
     *   <li>Creates a new pending assistant message</li>
     * </ol>
     *
     * @return the new pending message, or empty if there's no assistant message to regenerate
     */
    public Optional<ChatMessage> regenerateLastResponse() {
        Optional<ChatMessage> lastAssistant = getLastAssistantMessage();
        if (lastAssistant.isEmpty()) {
            return Optional.empty();
        }

        ChatMessage original = lastAssistant.get();
        ChatMessage newMessage = ChatMessage.pendingAssistant();
        newMessage.markRegeneratedFrom(original.getId());

        original.markSupersededBy(newMessage.getId());
        addMessage(newMessage);

        fireMessageRegenerated(original, newMessage);
        return Optional.of(newMessage);
    }

    // === Conversation History for API ===

    /**
     * Builds the conversation history as LlmMessages for API calls.
     *
     * <p>Excludes system messages and superseded messages.</p>
     *
     * @return list of LlmMessages
     */
    public List<LlmMessage> toConversationHistory() {
        List<LlmMessage> history = new ArrayList<>();

        for (ChatMessage message : messages) {
            // Skip system UI messages and superseded messages
            if (message.getKind() == MessageKind.SYSTEM) {
                continue;
            }
            if (message.isSuperseded() || message.isPending()) {
                continue;
            }

            history.add(message.toLlmMessage());
        }

        return history;
    }

    // === Session Management ===

    /**
     * Clears all messages from the session.
     */
    public void clear() {
        List<ChatMessage> removed = new ArrayList<>(messages);
        messages.clear();
        agentState = null;
        fireSessionCleared(removed);
    }

    // === Agent Run State ===

    /**
     * Returns the current agent run state.
     *
     * @return the agent state, or null if not in agent mode
     */
    public AgentRunState getAgentState() {
        return agentState;
    }

    /**
     * Starts a new agent run.
     *
     * @return the new agent state
     */
    public AgentRunState startAgentRun() {
        agentState = new AgentRunState();
        fireAgentStateChanged(agentState);
        return agentState;
    }

    /**
     * Ends the current agent run.
     */
    public void endAgentRun() {
        if (agentState != null) {
            agentState.complete();
            fireAgentStateChanged(agentState);
            agentState = null;
        }
    }

    /**
     * Checks if an agent run is currently active.
     *
     * @return true if agent is running
     */
    public boolean isAgentRunning() {
        return agentState != null && agentState.isRunning();
    }

    // === Listeners ===

    /**
     * Adds a session listener.
     *
     * @param listener the listener to add
     */
    public void addListener(SessionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a session listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(SessionListener listener) {
        listeners.remove(listener);
    }

    private void fireMessageAdded(ChatMessage message) {
        forEachListener(l -> l.onMessageAdded(this, message));
    }

    private void fireMessageEdited(ChatMessage original, ChatMessage newMessage, List<ChatMessage> removed) {
        forEachListener(l -> l.onMessageEdited(this, original, newMessage, removed));
    }

    private void fireMessageRegenerated(ChatMessage original, ChatMessage newMessage) {
        forEachListener(l -> l.onMessageRegenerated(this, original, newMessage));
    }

    private void fireSessionCleared(List<ChatMessage> removed) {
        forEachListener(l -> l.onSessionCleared(this, removed));
    }

    private void fireAgentStateChanged(AgentRunState state) {
        forEachListener(l -> l.onAgentStateChanged(this, state));
    }

    private void forEachListener(Consumer<SessionListener> action) {
        for (SessionListener listener : listeners) {
            try {
                action.accept(listener);
            } catch (Exception e) {
                // Log and continue with other listeners
            }
        }
    }

    /**
     * Listener for session events.
     */
    public interface SessionListener {

        /**
         * Called when a message is added.
         */
        default void onMessageAdded(ChatSession session, ChatMessage message) {}

        /**
         * Called when a message is edited.
         */
        default void onMessageEdited(ChatSession session, ChatMessage original,
                                     ChatMessage newMessage, List<ChatMessage> removed) {}

        /**
         * Called when a response is regenerated.
         */
        default void onMessageRegenerated(ChatSession session, ChatMessage original, ChatMessage newMessage) {}

        /**
         * Called when the session is cleared.
         */
        default void onSessionCleared(ChatSession session, List<ChatMessage> removed) {}

        /**
         * Called when agent state changes.
         */
        default void onAgentStateChanged(ChatSession session, AgentRunState state) {}
    }

    /**
     * State of an agent run (multi-step tool execution).
     */
    public static class AgentRunState {

        private final String runId;
        private int currentIteration;
        private int maxIterations = 10;
        private boolean running = true;
        private boolean cancelled = false;
        private final List<ToolExecutionStep> steps = new ArrayList<>();

        public AgentRunState() {
            this.runId = UUID.randomUUID().toString();
            this.currentIteration = 0;
        }

        public String getRunId() {
            return runId;
        }

        public int getCurrentIteration() {
            return currentIteration;
        }

        public int getMaxIterations() {
            return maxIterations;
        }

        public boolean isRunning() {
            return running && !cancelled;
        }

        public boolean isCancelled() {
            return cancelled;
        }

        public List<ToolExecutionStep> getSteps() {
            return Collections.unmodifiableList(steps);
        }

        public void incrementIteration() {
            currentIteration++;
        }

        public void addStep(ToolExecutionStep step) {
            steps.add(step);
        }

        public void cancel() {
            this.cancelled = true;
            this.running = false;
        }

        public void complete() {
            this.running = false;
        }

        /**
         * Represents a single tool execution step.
         */
        public static class ToolExecutionStep {
            private final String toolCallId;
            private final String toolName;
            private StepStatus status;
            private String result;
            private String error;

            public ToolExecutionStep(String toolCallId, String toolName) {
                this.toolCallId = toolCallId;
                this.toolName = toolName;
                this.status = StepStatus.PENDING;
            }

            public String getToolCallId() { return toolCallId; }
            public String getToolName() { return toolName; }
            public StepStatus getStatus() { return status; }
            public String getResult() { return result; }
            public String getError() { return error; }

            public void markRunning() { this.status = StepStatus.RUNNING; }
            public void markSuccess(String result) {
                this.status = StepStatus.SUCCESS;
                this.result = result;
            }
            public void markFailed(String error) {
                this.status = StepStatus.FAILED;
                this.error = error;
            }
            public void markCancelled() { this.status = StepStatus.CANCELLED; }

            public enum StepStatus {
                PENDING, RUNNING, SUCCESS, FAILED, CANCELLED
            }
        }
    }
}
