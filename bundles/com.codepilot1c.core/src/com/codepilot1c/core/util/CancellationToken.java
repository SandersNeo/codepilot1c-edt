/*******************************************************************************
 * Copyright (c) 2024 Example.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 ******************************************************************************/
package com.codepilot1c.core.util;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A token that can be used to signal cancellation of asynchronous operations.
 * Thread-safe implementation with support for callbacks.
 *
 * <p>Usage example:</p>
 * <pre>
 * CancellationToken token = new CancellationToken();
 *
 * // In async operation
 * void doWork(CancellationToken token) {
 *     while (!token.isCancelled()) {
 *         // do work
 *     }
 * }
 *
 * // To cancel
 * token.cancel();
 * </pre>
 */
public class CancellationToken {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final List<Runnable> callbacks = new CopyOnWriteArrayList<>();

    /**
     * Creates a new cancellation token.
     */
    public CancellationToken() {
    }

    /**
     * Cancels this token. All registered callbacks will be executed.
     * This method is idempotent - calling it multiple times has no additional effect.
     */
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            for (Runnable callback : callbacks) {
                try {
                    callback.run();
                } catch (Exception e) {
                    // Log but don't propagate - we want all callbacks to run
                }
            }
        }
    }

    /**
     * Returns whether this token has been cancelled.
     *
     * @return true if cancelled, false otherwise
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Throws a {@link CancellationException} if this token has been cancelled.
     *
     * @throws CancellationException if cancelled
     */
    public void throwIfCancelled() throws CancellationException {
        if (cancelled.get()) {
            throw new CancellationException("Operation was cancelled");
        }
    }

    /**
     * Registers a callback to be executed when this token is cancelled.
     * If the token is already cancelled, the callback is executed immediately.
     *
     * @param callback the callback to execute on cancellation
     */
    public void onCancel(Runnable callback) {
        if (callback == null) {
            return;
        }
        callbacks.add(callback);
        // If already cancelled, execute immediately
        if (cancelled.get()) {
            try {
                callback.run();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Removes a previously registered callback.
     *
     * @param callback the callback to remove
     * @return true if the callback was found and removed
     */
    public boolean removeCallback(Runnable callback) {
        return callbacks.remove(callback);
    }

    /**
     * Creates a new token that is linked to this token.
     * When this token is cancelled, the linked token will also be cancelled.
     *
     * @return a new linked token
     */
    public CancellationToken createLinkedToken() {
        CancellationToken linked = new CancellationToken();
        if (this.isCancelled()) {
            linked.cancel();
        } else {
            this.onCancel(linked::cancel);
        }
        return linked;
    }

    /**
     * Creates a token that is already cancelled.
     *
     * @return a cancelled token
     */
    public static CancellationToken cancelled() {
        CancellationToken token = new CancellationToken();
        token.cancel();
        return token;
    }

    /**
     * Creates a token that can never be cancelled.
     *
     * @return a non-cancellable token
     */
    public static CancellationToken none() {
        return new CancellationToken() {
            @Override
            public void cancel() {
                // Do nothing - this token cannot be cancelled
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };
    }
}
