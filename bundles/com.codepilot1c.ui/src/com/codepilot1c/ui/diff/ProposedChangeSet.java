/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.diff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Manages a set of proposed changes from an agent run.
 *
 * <p>Allows batch review and application of changes.</p>
 */
public class ProposedChangeSet {

    private final String sessionId;
    private final List<ProposedChange> changes;
    private final List<ChangeSetListener> listeners;

    /**
     * Creates a new proposed change set.
     *
     * @param sessionId the session ID
     */
    public ProposedChangeSet(String sessionId) {
        this.sessionId = sessionId;
        this.changes = new ArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
    }

    // === Change Management ===

    /**
     * Adds a proposed change.
     *
     * @param change the change to add
     */
    public void addChange(ProposedChange change) {
        changes.add(change);
        fireChangeAdded(change);
    }

    /**
     * Returns all proposed changes.
     *
     * @return unmodifiable list of changes
     */
    public List<ProposedChange> getChanges() {
        return Collections.unmodifiableList(changes);
    }

    /**
     * Returns changes by status.
     *
     * @param status the status to filter by
     * @return list of changes with the given status
     */
    public List<ProposedChange> getChangesByStatus(ProposedChange.ChangeStatus status) {
        return changes.stream()
                .filter(c -> c.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * Returns pending changes.
     *
     * @return list of pending changes
     */
    public List<ProposedChange> getPendingChanges() {
        return getChangesByStatus(ProposedChange.ChangeStatus.PENDING);
    }

    /**
     * Returns accepted changes.
     *
     * @return list of accepted changes
     */
    public List<ProposedChange> getAcceptedChanges() {
        return getChangesByStatus(ProposedChange.ChangeStatus.ACCEPTED);
    }

    /**
     * Finds a change by file path.
     *
     * @param filePath the file path
     * @return the change, or empty if not found
     */
    public Optional<ProposedChange> findByPath(String filePath) {
        return changes.stream()
                .filter(c -> c.getFilePath().equals(filePath))
                .findFirst();
    }

    /**
     * Finds a change by tool call ID.
     *
     * @param toolCallId the tool call ID
     * @return the change, or empty if not found
     */
    public Optional<ProposedChange> findByToolCallId(String toolCallId) {
        return changes.stream()
                .filter(c -> toolCallId.equals(c.getToolCallId()))
                .findFirst();
    }

    // === Batch Operations ===

    /**
     * Accepts all pending changes.
     */
    public void acceptAll() {
        for (ProposedChange change : changes) {
            if (change.isPending()) {
                change.accept();
                fireChangeStatusChanged(change);
            }
        }
    }

    /**
     * Rejects all pending changes.
     */
    public void rejectAll() {
        for (ProposedChange change : changes) {
            if (change.isPending()) {
                change.reject();
                fireChangeStatusChanged(change);
            }
        }
    }

    /**
     * Clears all changes.
     */
    public void clear() {
        List<ProposedChange> removed = new ArrayList<>(changes);
        changes.clear();
        fireChangesCleared(removed);
    }

    // === Statistics ===

    /**
     * Returns the total number of changes.
     *
     * @return change count
     */
    public int size() {
        return changes.size();
    }

    /**
     * Returns whether the set is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return changes.isEmpty();
    }

    /**
     * Returns the number of pending changes.
     *
     * @return pending count
     */
    public int getPendingCount() {
        return (int) changes.stream().filter(ProposedChange::isPending).count();
    }

    /**
     * Returns the number of accepted changes.
     *
     * @return accepted count
     */
    public int getAcceptedCount() {
        return (int) changes.stream().filter(ProposedChange::isAccepted).count();
    }

    /**
     * Returns the number of rejected changes.
     *
     * @return rejected count
     */
    public int getRejectedCount() {
        return (int) changes.stream().filter(ProposedChange::isRejected).count();
    }

    /**
     * Returns whether all changes have been reviewed.
     *
     * @return true if no pending changes
     */
    public boolean isFullyReviewed() {
        return getPendingCount() == 0;
    }

    /**
     * Returns whether any changes were accepted.
     *
     * @return true if at least one accepted
     */
    public boolean hasAcceptedChanges() {
        return getAcceptedCount() > 0;
    }

    /**
     * Returns whether there are actual content changes.
     * Checks if at least one change has different before/after content.
     *
     * @return true if at least one change has actual differences
     */
    public boolean hasActualChanges() {
        for (ProposedChange change : changes) {
            String before = change.getBeforeContent();
            String after = change.getAfterContent();

            // New file (before is null) or deleted file (after is null) is an actual change
            if (before == null || after == null) {
                return true;
            }

            // Check if content actually differs
            if (!before.equals(after)) {
                return true;
            }
        }
        return false;
    }

    // === Session Info ===

    public String getSessionId() {
        return sessionId;
    }

    // === Listeners ===

    /**
     * Adds a listener.
     *
     * @param listener the listener
     */
    public void addListener(ChangeSetListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener.
     *
     * @param listener the listener
     */
    public void removeListener(ChangeSetListener listener) {
        listeners.remove(listener);
    }

    private void fireChangeAdded(ProposedChange change) {
        forEachListener(l -> l.onChangeAdded(this, change));
    }

    private void fireChangeStatusChanged(ProposedChange change) {
        forEachListener(l -> l.onChangeStatusChanged(this, change));
    }

    private void fireChangesCleared(List<ProposedChange> removed) {
        forEachListener(l -> l.onChangesCleared(this, removed));
    }

    private void forEachListener(Consumer<ChangeSetListener> action) {
        for (ChangeSetListener listener : listeners) {
            try {
                action.accept(listener);
            } catch (Exception e) {
                // Log and continue
            }
        }
    }

    /**
     * Listener for change set events.
     */
    public interface ChangeSetListener {

        /**
         * Called when a change is added.
         */
        default void onChangeAdded(ProposedChangeSet set, ProposedChange change) {}

        /**
         * Called when a change status changes.
         */
        default void onChangeStatusChanged(ProposedChangeSet set, ProposedChange change) {}

        /**
         * Called when changes are cleared.
         */
        default void onChangesCleared(ProposedChangeSet set, List<ProposedChange> removed) {}
    }
}
