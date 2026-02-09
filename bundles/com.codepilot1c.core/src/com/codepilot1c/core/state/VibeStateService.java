/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.state;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import com.codepilot1c.core.internal.VibeCorePlugin;
import com.codepilot1c.core.logging.VibeLogger;

/**
 * Service for managing and broadcasting plugin state changes.
 *
 * <p>This service provides a centralized way to track the current state
 * of the 1C Copilot plugin and notify listeners of changes. Used by the
 * status bar control and other UI components.</p>
 *
 * <p>Thread-safe. State changes are broadcast to all registered listeners.</p>
 */
public class VibeStateService {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(VibeStateService.class);

    private static VibeStateService instance;

    private final AtomicReference<VibeState> currentState = new AtomicReference<>(VibeState.IDLE);
    private final AtomicReference<String> statusMessage = new AtomicReference<>(""); //$NON-NLS-1$
    private final AtomicReference<String> errorMessage = new AtomicReference<>(null);
    private final List<StateChangeListener> listeners = new CopyOnWriteArrayList<>();

    private VibeStateService() {
    }

    /**
     * Returns the singleton instance.
     *
     * @return the service instance
     */
    public static synchronized VibeStateService getInstance() {
        if (instance == null) {
            instance = new VibeStateService();
        }
        return instance;
    }

    /**
     * Returns the current plugin state.
     *
     * @return the current state
     */
    public VibeState getState() {
        return currentState.get();
    }

    /**
     * Returns the current status message.
     *
     * @return the status message, or empty string if none
     */
    public String getStatusMessage() {
        return statusMessage.get();
    }

    /**
     * Returns the last error message, if any.
     *
     * @return the error message, or null if no error
     */
    public String getErrorMessage() {
        return errorMessage.get();
    }

    /**
     * Sets the plugin state to idle (ready).
     */
    public void setIdle() {
        setState(VibeState.IDLE, "Ready"); //$NON-NLS-1$
        errorMessage.set(null);
    }

    /**
     * Sets the plugin state to completing.
     *
     * @param message optional status message
     */
    public void setCompleting(String message) {
        setState(VibeState.COMPLETING, message != null ? message : "Completing..."); //$NON-NLS-1$
    }

    /**
     * Sets the plugin state to processing (chat request).
     *
     * @param message optional status message
     */
    public void setProcessing(String message) {
        setState(VibeState.PROCESSING, message != null ? message : "Processing..."); //$NON-NLS-1$
    }

    /**
     * Sets the plugin state to streaming.
     *
     * @param message optional status message
     */
    public void setStreaming(String message) {
        setState(VibeState.STREAMING, message != null ? message : "Streaming..."); //$NON-NLS-1$
    }

    /**
     * Sets the plugin state to indexing.
     *
     * @param message optional status message (e.g., "Indexing 45%")
     */
    public void setIndexing(String message) {
        setState(VibeState.INDEXING, message != null ? message : "Indexing..."); //$NON-NLS-1$
    }

    /**
     * Sets the plugin state to error.
     *
     * @param errorMsg the error message to display
     */
    public void setError(String errorMsg) {
        errorMessage.set(errorMsg);
        setState(VibeState.ERROR, "Error"); //$NON-NLS-1$
        LOG.warn("Vibe state error: %s", errorMsg); //$NON-NLS-1$
    }

    /**
     * Sets the plugin state to disabled.
     */
    public void setDisabled() {
        setState(VibeState.DISABLED, "Disabled"); //$NON-NLS-1$
    }

    /**
     * Sets the plugin state to not configured.
     *
     * @param message optional message explaining what's missing
     */
    public void setNotConfigured(String message) {
        errorMessage.set(message);
        setState(VibeState.NOT_CONFIGURED, message != null ? message : "Not Configured"); //$NON-NLS-1$
    }

    /**
     * Sets the state and message, notifying listeners.
     *
     * @param state the new state
     * @param message the status message
     */
    private void setState(VibeState state, String message) {
        VibeState oldState = currentState.getAndSet(state);
        statusMessage.set(message);

        if (oldState != state) {
            LOG.debug("Состояние изменено: %s -> %s (%s)", oldState, state, message); //$NON-NLS-1$
            notifyListeners(oldState, state, message);
        }
    }

    /**
     * Notifies all registered listeners of a state change.
     */
    private void notifyListeners(VibeState oldState, VibeState newState, String message) {
        for (StateChangeListener listener : listeners) {
            try {
                listener.onStateChanged(oldState, newState, message);
            } catch (Exception e) {
                VibeCorePlugin.logWarn("Error notifying state listener", e); //$NON-NLS-1$
            }
        }
    }

    /**
     * Adds a state change listener.
     *
     * @param listener the listener to add
     */
    public void addListener(StateChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a state change listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(StateChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Returns whether the plugin is currently in an active state.
     *
     * @return true if active
     */
    public boolean isActive() {
        return currentState.get().isActive();
    }

    /**
     * Returns whether the plugin is ready for new requests.
     *
     * @return true if ready
     */
    public boolean isReady() {
        return currentState.get().isReady();
    }

    /**
     * Returns whether there's a current error.
     *
     * @return true if error state
     */
    public boolean hasError() {
        return currentState.get().isError();
    }

    /**
     * Listener interface for state changes.
     */
    @FunctionalInterface
    public interface StateChangeListener {
        /**
         * Called when the plugin state changes.
         *
         * @param oldState the previous state
         * @param newState the new state
         * @param message the status message
         */
        void onStateChanged(VibeState oldState, VibeState newState, String message);
    }
}
