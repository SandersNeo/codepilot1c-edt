/*******************************************************************************
 * Copyright (c) 2024 Example.
 * All rights reserved. This program and the accompanying materials
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 ******************************************************************************/
package com.codepilot1c.core.streaming;

/**
 * Observer interface for streaming responses from LLM providers.
 * Implementations receive chunks of text as they arrive and are notified
 * when the stream completes or encounters an error.
 *
 * <p>All methods are called on background threads - implementations must
 * handle thread synchronization appropriately (e.g., use Display.asyncExec
 * for SWT UI updates).</p>
 *
 * @param <T> the type of chunks received
 */
public interface IStreamObserver<T> {

    /**
     * Called when a new chunk is received from the stream.
     *
     * @param chunk the received chunk
     */
    void onNext(T chunk);

    /**
     * Called when the stream completes successfully.
     * After this method is called, no more chunks will be received.
     */
    void onComplete();

    /**
     * Called when an error occurs during streaming.
     * After this method is called, no more chunks will be received.
     *
     * @param error the error that occurred
     */
    void onError(Throwable error);

    /**
     * Creates a simple observer that only handles chunks.
     *
     * @param <T> the chunk type
     * @param onNext handler for chunks
     * @return a stream observer
     */
    static <T> IStreamObserver<T> create(java.util.function.Consumer<T> onNext) {
        return create(onNext, () -> {}, e -> {});
    }

    /**
     * Creates an observer with all handlers.
     *
     * @param <T> the chunk type
     * @param onNext handler for chunks
     * @param onComplete handler for completion
     * @param onError handler for errors
     * @return a stream observer
     */
    static <T> IStreamObserver<T> create(
            java.util.function.Consumer<T> onNext,
            Runnable onComplete,
            java.util.function.Consumer<Throwable> onError) {
        return new IStreamObserver<T>() {
            @Override
            public void onNext(T chunk) {
                if (onNext != null) {
                    onNext.accept(chunk);
                }
            }

            @Override
            public void onComplete() {
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (onError != null) {
                    onError.accept(error);
                }
            }
        };
    }
}
